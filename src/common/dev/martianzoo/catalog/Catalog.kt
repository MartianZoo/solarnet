package dev.martianzoo.catalog

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.pets.types.ClassTable

/**
 * One coherent static namespace containing everything a game premise may select.
 *
 * A Catalog owns one validated master [ClassTable]. That table is the reusable schema for all of
 * its games, not a playable world: each [GamePremise] selects an inhabited view of it. Catalog
 * implementations may use internal packaging such as bundles, but callers compose and play exactly
 * one Catalog, in which every class name has one meaning. Assemblers can use
 * [ClassDeclaration.indexByName] to merge identical contributions and diagnose conflicting names.
 * Construction from other Catalogs combines their source declarations, custom-class dependencies,
 * transforms, Module selections, availability, and display names. Game-specific subclasses apply
 * their own lowering and selection policies to the assembled namespace.
 */
public open class Catalog(private vararg val catalogs: Catalog) {
  /** The fully compiled Catalog structure shared by its playable games. */
  public val classTable: ClassTable by lazy {
    createClassLoader(this).loadEverything().also(::validateClasses)
  }

  /** Additional validation owned by this game. */
  protected open fun validateClasses(table: ClassTable) {}

  /** Handlers for this game's explicitly marked Pets syntax, bound to one game class table. */
  public open val transformHandlerFactories: Map<String, (ClassTable) -> TransformHandler> by lazy {
    val combined = linkedMapOf<String, (ClassTable) -> TransformHandler>()
    catalogs.forEach { catalog ->
      catalog.transformHandlerFactories.forEach { (name, factory) ->
        val previous = combined.put(name, factory)
        require(previous == null || previous == factory) {
          "Conflicting transform handlers for $name"
        }
      }
    }
    combined
  }

  /**
   * The available Modules and the Class selections each contributes by default.
   *
   * A selected Module and the Classes required by its own declaration form its intrinsic ambient
   * rules; they do not need entries here. Other selections can represent individually overridable
   * Content or implementation-level Classes such as map areas. A conditional selection contributes
   * its Class only when its requirement is met by the completed configuration. Selected Modules are
   * also the complete ambient-rule configuration of a live game.
   */
  public open val modules: Map<ClassName, Set<ClassSelection>> by lazy {
    catalogs
        .flatMap { it.modules.entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { (name, contributions) ->
          val selections = contributions.distinct()
          require(selections.size == 1) { "Conflicting selections for Module $name" }
          selections.single()
        }
  }

  /** Modules whose selection makes each otherwise bundle-local ambient Class available. */
  public open val classAvailabilityModules: Map<ClassName, Set<ClassName>> by lazy {
    catalogs
        .flatMap { it.classAvailabilityModules.entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { (_, modules) -> modules.flatten().toSet() }
  }

  /**
   * Natural-language display names keyed first by language tag and then by canonical class name.
   */
  public open val displayNamesByLanguage: Map<String, Map<ClassName, String>> by lazy {
    val combined = mutableMapOf<String, MutableMap<ClassName, String>>()
    catalogs.forEach { catalog ->
      catalog.displayNamesByLanguage.forEach { (language, names) ->
        val languageNames = combined.getOrPut(language, ::linkedMapOf)
        names.forEach { (className, displayName) ->
          val previous = languageNames.put(className, displayName)
          require(previous == null || previous == displayName) {
            "Conflicting $language display names for $className: $previous and $displayName"
          }
        }
      }
    }
    combined
  }

  /** The unique declaration for every class in this Catalog's namespace. */
  public val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    ClassDeclaration.indexByName(systemClassDeclarations.toList() + contributedClassDeclarations)
  }

  /** Executable declarations, after any game-specific source lowering. */
  protected open val contributedClassDeclarations: List<ClassDeclaration>
    get() = explicitClassDeclarations.toList()

  /** Every canonical class name in this Catalog's namespace. */
  public val allClassNames: Set<ClassName>
    get() = allClassDeclarations.keys

  /** Direct source declarations, before game-specific executable lowering. */
  public open val explicitClassDeclarations: Set<ClassDeclaration> =
      catalogs.flatMapTo(linkedSetOf(), Catalog::explicitClassDeclarations)

  /** Declarations whose executable behavior must be supplied when starting a live game. */
  public val customClassDeclarations: List<ClassDeclaration> by lazy {
    allClassDeclarations.values.filter { it.customMetric || it.customInstruction }
  }

  /** Static vocabulary required by custom behavior, independently of its Kotlin implementation. */
  public open val customClassDependencies: Map<ClassName, Set<ClassName>> by lazy {
    catalogs
        .flatMap { it.customClassDependencies.entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { (name, contributions) ->
          val dependencies = contributions.distinct()
          require(dependencies.size == 1) { "Conflicting custom class dependencies for $name" }
          dependencies.single()
        }
  }

  /** Returns the unique declaration having [name]. */
  public fun classDeclaration(name: ClassName): ClassDeclaration =
      allClassDeclarations[name]
          ?: throw IllegalArgumentException("no class declaration named `$name`")

  /**
   * Resolves signed Class selections and additive setup adjustments over this Catalog's master
   * table.
   */
  public fun gamePremise(
      config: GameConfig,
      additionalClassDeclarations: Set<ClassDeclaration> = emptySet(),
  ): GamePremise {
    val builder = GamePremiseBuilder(this, config, additionalClassDeclarations)
    configurePremise(builder)
    return builder.build()
  }

  /** Applies game-specific selection and setup policy before this Catalog freezes a premise. */
  protected open fun configurePremise(builder: GamePremiseBuilder) {}

  /** Cooks a premise whose player names and seat order come from [playerDeclarations]. */
  public fun gamePremise(
      config: GameConfig,
      playerDeclarations: List<ClassDeclaration>,
  ): GamePremise {
    if (playerDeclarations.isEmpty()) return gamePremise(config)
    require(config.playerNames.isEmpty()) {
      "player names must come from either GameConfig or Player declarations, not both"
    }
    val playerNames = playerDeclarations.map(ClassDeclaration::className)
    return gamePremise(
        config.copy(playerNames = playerNames),
        additionalClassDeclarations = playerDeclarations.toSet(),
    )
  }

  /** Returns this Catalog composed with concrete `Player1` through `PlayerN` seat Classes. */
  public fun withPlayers(playerCount: Int): Catalog {
    require(playerCount > 0) { "player count must be positive: $playerCount" }
    val names = conventionalPlayerClassNames(playerCount)
    if (hasPlayerClasses(names)) return this
    return conventionalPlayerCatalogs.getOrPut(playerCount) { withPlayerClassesUncached(names) }
  }

  /** Returns this Catalog composed with concrete `Player` subclasses named by [playerNames]. */
  public fun withPlayers(playerNames: List<ClassName>): Catalog {
    require(playerNames.isNotEmpty()) { "a game must have at least one player name" }
    require(playerNames.distinct().size == playerNames.size) {
      "a game cannot seat the same player name more than once"
    }
    val conventionalNames = conventionalPlayerClassNames(playerNames.size)
    return if (playerNames == conventionalNames) {
      withPlayers(playerNames.size)
    } else {
      withPlayerClassesUncached(playerNames)
    }
  }

  private val conventionalPlayerCatalogs: MutableMap<Int, Catalog> = mutableMapOf()

  private fun hasPlayerClasses(playerNames: List<ClassName>): Boolean {
    val playerClass = classTable.findClass(PLAYER) ?: return false
    return playerNames.all { name ->
      classTable.findClass(name)?.let { !it.abstract && it.isSubtypeOf(playerClass) } == true
    }
  }

  private fun withPlayerClassesUncached(playerNames: List<ClassName>): Catalog {
    val playerClass =
        requireNotNull(classTable.findClass(PLAYER)) { "Catalog does not define Player" }
    val missingNames = linkedSetOf<ClassName>()
    playerNames.forEach { name ->
      val existing = classTable.findClass(name)
      when {
        existing == null -> missingNames.add(name)
        existing.abstract || !existing.isSubtypeOf(playerClass) ->
            throw IllegalArgumentException("player name collides with Catalog Class $name")
      }
    }
    if (missingNames.isEmpty()) return this
    val declarations =
        parseClasses(missingNames.joinToString("\n") { name -> "CLASS $name : Player" }).toSet()
    return withDeclarations(declarations)
  }

  /** Preserves the game's Catalog policies when adding declarations. */
  protected open fun withDeclarations(declarations: Set<ClassDeclaration>): Catalog =
      Catalog(
          this,
          object : Catalog() {
            override val explicitClassDeclarations: Set<ClassDeclaration> = declarations
          },
      )

  protected fun isSubtypeOf(className: ClassName, possibleSupertype: ClassName): Boolean {
    if (className == possibleSupertype) return true
    return classDeclaration(className).supertypes.any { supertype ->
      isSubtypeOf(supertype.className, possibleSupertype)
    }
  }
}
