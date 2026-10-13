package dev.martianzoo.catalog

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
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
  /**
   * Fully compiled class namespace shared by premises built from this catalog. Computed on first
   * access; invalid or conflicting declarations fail while loading it.
   */
  public val classTable: ClassTable by lazy {
    createClassLoader(this).loadEverything()
  }

  /**
   * Factories for named Pets transforms, each bound later to a game's class table. Contributions
   * with the same name must use the same factory or composition fails.
   */
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

  /**
   * For each otherwise bundle-local ambient class, the modules that make it available. Composed
   * catalogs union the module sets for the same class name.
   */
  public open val classAvailabilityModules: Map<ClassName, Set<ClassName>> by lazy {
    catalogs
        .flatMap { it.classAvailabilityModules.entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { (_, modules) -> modules.flatten().toSet() }
  }

  /**
   * Natural-language display names keyed first by language tag and then by canonical class name.
   * Composed catalogs may repeat an identical name; conflicting values for the same language and
   * class fail when this map is loaded.
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

  /**
   * Stable Catalog facts not already represented by class declarations or Module selections, keyed
   * by source name. A composed Catalog merges equal contributions and rejects conflicting values
   * for the same key. Overrides must return values that do not depend on object identity or the
   * order in which this Catalog was queried.
   */
  public open val fingerprintMetadata: Map<String, String> by lazy {
    catalogs
        .flatMap { it.fingerprintMetadata.entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { (name, values) ->
          require(values.distinct().size == 1) { "conflicting fingerprint metadata: $name" }
          values.first()
        }
  }

  /**
   * Unique declaration for every class in this catalog's namespace, including Pets system classes
   * and contributed declarations after [contributedClassDeclarations] lowering. Conflicting names
   * fail rather than replacing an earlier declaration.
   */
  public val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    ClassDeclaration.indexByName(systemClassDeclarations.toList() + contributedClassDeclarations)
  }

  /** Executable declarations, after any game-specific source lowering. */
  protected open val contributedClassDeclarations: List<ClassDeclaration>
    get() = explicitClassDeclarations.toList()

  /** Names of [allClassDeclarations], including system and contributed classes. */
  public val allClassNames: Set<ClassName>
    get() = allClassDeclarations.keys

  /** Authored declarations from the composed catalogs, before game-specific executable lowering. */
  public open val explicitClassDeclarations: Set<ClassDeclaration> =
      catalogs.flatMapTo(linkedSetOf(), Catalog::explicitClassDeclarations)

  /** Declarations marked as custom metrics or instructions whose runtime code a live game needs. */
  public val customClassDeclarations: List<ClassDeclaration> by lazy {
    allClassDeclarations.values.filter { it.customMetric || it.customInstruction }
  }

  /** Returns [name]'s declaration, or throws [IllegalArgumentException] if it is unknown. */
  public fun classDeclaration(name: ClassName): ClassDeclaration =
      allClassDeclarations[name]
          ?: throw IllegalArgumentException("no class declaration named `$name`")

  /**
   * Resolves [config]'s signed class selections and additive setup adjustments over this catalog's
   * master table. [additionalClassDeclarations] are local to the resulting premise; they do not
   * change this reusable catalog. Selection policy, dependency closure, and validation occur before
   * the [GamePremise] is returned. An explicit class or card-bundle exclusion must change the
   * resolved premise; an exclusion whose removal leaves the same playable selection is rejected.
   * Invalid selections fail rather than producing a partial premise.
   */
  public fun gamePremise(
      config: GameConfig,
      additionalClassDeclarations: Set<ClassDeclaration> = emptySet(),
  ): GamePremise {
    val premise = resolvePremise(config, additionalClassDeclarations)
    if (config.excludedClassNames.isEmpty() && config.excludedCardBundles.isEmpty()) return premise
    premise.classTable
    config.excludedClassNames.forEach { excluded ->
      val without = config.copy(excludedClassNames = config.excludedClassNames - excluded)
      rejectIneffectiveExclusion(
          "-$excluded",
          premise,
          without,
          additionalClassDeclarations,
      )
    }
    config.excludedCardBundles.forEach { excluded ->
      val without = config.copy(excludedCardBundles = config.excludedCardBundles - excluded)
      rejectIneffectiveExclusion(
          "-$excluded:cards",
          premise,
          without,
          additionalClassDeclarations,
      )
    }
    return premise
  }

  private fun resolvePremise(
      config: GameConfig,
      additionalClassDeclarations: Set<ClassDeclaration>,
  ): GamePremise {
    val builder = GamePremiseBuilder(this, config, additionalClassDeclarations)
    configurePremise(builder)
    return builder.build()
  }

  private fun rejectIneffectiveExclusion(
      entry: String,
      premise: GamePremise,
      without: GameConfig,
      additionalClassDeclarations: Set<ClassDeclaration>,
  ) {
    val alternative =
        runCatching {
          resolvePremise(without, additionalClassDeclarations).also { it.classTable }
        }
            .getOrNull() ?: return
    val selected =
        premise.classSelections.filter(ClassSelection::included).mapTo(hashSetOf()) { it.className }
    val alternativeSelected =
        alternative.classSelections.filter(ClassSelection::included).mapTo(hashSetOf()) {
          it.className
        }
    if (
        selected == alternativeSelected &&
            premise.componentAdjustments == alternative.componentAdjustments &&
            premise.classTable.allClassNames == alternative.classTable.allClassNames &&
            premise.premiseClassDeclarations == alternative.premiseClassDeclarations &&
            premise.bootstrapClassName == alternative.bootstrapClassName &&
            premise.premiseClassName == alternative.premiseClassName
    ) {
      throw InvalidGameConfigException("configuration exclusion has no effect: `$entry`")
    }
  }

  /** Subclass hook to add game-specific selection and setup policy before the builder is frozen. */
  protected open fun configurePremise(builder: GamePremiseBuilder) {}

  /**
   * Resolves [config] with concrete player classes supplied by [playerDeclarations], in seat order.
   * The declarations are local to the premise. A nonempty list cannot be combined with player names
   * already in [config]; an empty list delegates to [gamePremise] unchanged.
   */
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

  /**
   * Returns this catalog with concrete `Player1` through `PlayerN` classes available. Existing
   * suitable classes are reused; [playerCount] must be positive. This does not seat players in a
   * game until a [GameConfig] names them. For example, `withPlayers(2)` makes `Player1` and
   * `Player2` available, but does not put either in a game's [GamePremise.playerNames].
   */
  public fun withPlayers(playerCount: Int): Catalog {
    require(playerCount > 0) { "player count must be positive: $playerCount" }
    val names = conventionalPlayerClassNames(playerCount)
    if (hasPlayerClasses(names)) return this
    return conventionalPlayerCatalogs.getOrPut(playerCount) { withPlayerClassesUncached(names) }
  }

  /**
   * Returns this catalog with concrete `Player` subclasses for [playerNames]. Names must be
   * nonempty and distinct; an existing name must already be a concrete Player subclass. This does
   * not seat them until a [GameConfig] supplies the names in seat order.
   */
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

  /** Subclass hook for composing [declarations] while preserving game-specific catalog policy. */
  protected open fun withDeclarations(declarations: Set<ClassDeclaration>): Catalog =
      Catalog(
          this,
          object : Catalog() {
            override val explicitClassDeclarations: Set<ClassDeclaration> = declarations
          },
      )

  /**
   * Whether [className] is [possibleSupertype] or inherits it through declared supertypes. Unknown
   * class names fail through [classDeclaration].
   */
  protected fun isSubtypeOf(className: ClassName, possibleSupertype: ClassName): Boolean {
    if (className == possibleSupertype) return true
    return classDeclaration(className).supertypes.any { supertype ->
      isSubtypeOf(supertype.className, possibleSupertype)
    }
  }
}
