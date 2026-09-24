package dev.martianzoo.pets.data

import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.SystemClasses.AUDIT
import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassLoader
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.PremiseClassTable
import dev.martianzoo.pets.types.PremiseViability

/**
 * The complete immutable, resolved input from which equivalent playable worlds are constructed.
 *
 * [modules] names the exact ambient rules, [classSelections] records the remaining signed Class
 * choices, [playerNames] fixes seat order, and [initialComponentTypes] names state to instantiate
 * once. [classTable] forms their inclusion closure over the Catalog's reusable master table. Known
 * Classes outside that closure remain uninhabited; closure that reaches an excluded Class or an
 * unrequested Module is invalid.
 *
 * @throws InvalidGameConfigException if its fields cannot describe a playable game configuration
 */
public data class GamePremise(
    public val catalog: Catalog,
    public val modules: Set<ClassName>,
    public val classSelections: Set<ClassSelection>,
    public val initialComponentTypes: Set<Expression>,
    /** Concrete Player Class Names in seat order. */
    public val playerNames: List<ClassName> = emptyList(),
    /** Concrete Component created by Admin immediately before the generated premise Class. */
    public val bootstrapClassName: ClassName? = null,
    /** Concrete configuration Class created during bootstrap, when the Catalog supplies one. */
    public val premiseClassName: ClassName? = null,
    /** Generated and ad-hoc declarations owned only by this premise. */
    public val premiseClassDeclarations: Set<ClassDeclaration> = emptySet(),
) {
  /** The premise-local declaration delta over the Catalog's reusable master table. */
  private val premiseClassTableLazy = lazy {
    PremiseClassTable(catalog.classTable, premiseClassDeclarations)
  }
  internal val premiseClassTable: PremiseClassTable
    get() = premiseClassTableLazy.value

  /**
   * The immutable premise-selected class-table view shared by every World built from this premise.
   */
  private val classTableLazy = lazy(::createClassTable)
  public val classTable: ClassTable
    get() = classTableLazy.value

  /**
   * Forms and freezes this premise's playable view, reusing the Catalog's compiled objects as
   * required by
   * [rules T12-1 through T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
   */
  private fun createClassTable(): ClassTable {
    val premiseTable = premiseClassTable
    val masterTable = premiseTable.master
    val initialClassNames =
        initialComponentTypes.flatMap { it.descendantsOfType<ClassName>() }.toSet()
    val configurationNames: Set<ClassName> =
        modules +
            classSelections.filter(ClassSelection::included).map(ClassSelection::className) +
            playerNames +
            initialClassNames
    val moduleSelections = modules.flatMap { catalog.modules.getValue(it) }
    val (applicableModuleSelections, inapplicableModuleSelections) =
        moduleSelections.partition { selection ->
          selection.appliesTo(configurationNames, premiseTable)
        }
    val moduleIncluded =
        applicableModuleSelections
            .filter(ClassSelection::included)
            .mapTo(linkedSetOf(), ClassSelection::className)
    val conditionallyExcluded =
        inapplicableModuleSelections
            .filter(ClassSelection::included)
            .mapTo(hashSetOf(), ClassSelection::className) - moduleIncluded
    val moduleExcluded =
        applicableModuleSelections
            .filterNot(ClassSelection::included)
            .mapTo(hashSetOf(), ClassSelection::className) + conditionallyExcluded
    val selectedByModules = moduleIncluded - moduleExcluded
    val explicitlyIncluded =
        classSelections
            .filter(ClassSelection::included)
            .mapTo(linkedSetOf(), ClassSelection::className)
    val explicitlyExcluded =
        classSelections
            .filterNot(ClassSelection::included)
            .mapTo(linkedSetOf(), ClassSelection::className)
    val excluded = (moduleExcluded - explicitlyIncluded) + explicitlyExcluded
    val roots =
        setOf(AUDIT) +
            modules +
            ((selectedByModules - explicitlyExcluded) + explicitlyIncluded) +
            initialClassNames +
            actors.map(Actor::className) +
            listOfNotNull(bootstrapClassName, premiseClassName)

    val table = ClassLoader.forPremise(catalog, premiseTable, modules, classSelections)
    table.freeze()
    table.validateNoOkSubscriptions()
    table.validateTransformKinds()
    table.includeAll(roots)
    val unexpectedModules =
        catalog.modules.keys.filterTo(linkedSetOf()) { table.isIncluded(it) } - modules
    if (unexpectedModules.isNotEmpty()) {
      throw InvalidGameConfigException(
          "structural selection included unrequested modules: `$unexpectedModules`"
      )
    }
    val playerClass = masterTable.findClass(PLAYER)
    val inhabitedPlayerClassNames =
        playerClass
            ?.let(table::allSubclasses)
            .orEmpty()
            .filterNot(Class::abstract)
            .filter(table::isInhabited)
            .mapTo(linkedSetOf(), Class::className)
    if (inhabitedPlayerClassNames != playerNames.toSet()) {
      throw InvalidGameConfigException(
          "inhabited `Player` classes do not match occupied seats: `$inhabitedPlayerClassNames`"
      )
    }
    val includedExclusions = excluded.filterTo(linkedSetOf(), table::isIncluded)
    if (includedExclusions.isNotEmpty()) {
      throw InvalidGameConfigException(
          "structural selection conflicts with excluded classes: `$includedExclusions`"
      )
    }
    PremiseViability.validate(table, roots)
    return table
  }

  init {
    val premiseDeclarationsByName = premiseClassTable.declarations
    val allKnownNames = catalog.allClassNames + premiseDeclarationsByName.keys
    val selectedNames = classSelections.map(ClassSelection::className)
    val invalidPlayerNames = playerNames.filter { playerName ->
      val configuredClass = catalog.classTable.findClass(playerName)
      val premiseDeclaration = premiseDeclarationsByName[playerName]
      (configuredClass?.abstract ?: premiseDeclaration?.abstract ?: true) ||
          !premiseClassTable.isSubtypeOf(playerName, PLAYER)
    }
    if (invalidPlayerNames.isNotEmpty()) {
      throw InvalidGameConfigException(
          "player names must be concrete `Player` classes: `$invalidPlayerNames`"
      )
    }
    if (playerNames.distinct().size != playerNames.size) {
      throw InvalidGameConfigException("duplicate player names: `$playerNames`")
    }
    if (modules.any { it !in catalog.modules }) {
      throw InvalidGameConfigException("unknown modules: `${modules - catalog.modules.keys}`")
    }
    if (selectedNames.distinct().size != selectedNames.size) {
      throw InvalidGameConfigException("duplicate individual class selections: `$selectedNames`")
    }
    if (classSelections.any { it.requirement != null }) {
      throw InvalidGameConfigException(
          "individual class selections cannot be conditional: " +
              "`${classSelections.filter { it.requirement != null }}`"
      )
    }
    if (selectedNames.any { it !in allKnownNames }) {
      throw InvalidGameConfigException(
          "individual class selections are absent from the premise catalog: " +
              "`${selectedNames - allKnownNames}`"
      )
    }
    if (selectedNames.any { it in catalog.modules }) {
      throw InvalidGameConfigException(
          "modules cannot be selected as individual classes: " +
              "`${selectedNames.filter { it in catalog.modules }}`"
      )
    }
    val initialClassNames =
        initialComponentTypes.flatMap { it.descendantsOfType<ClassName>() }.toSet()
    if (initialClassNames.any { it !in allKnownNames }) {
      throw InvalidGameConfigException(
          "initial component types name classes absent from the premise catalog: " +
              "`${initialClassNames - allKnownNames}`"
      )
    }
    premiseClassName?.let { className ->
      val declaration =
          premiseDeclarationsByName[className] ?: catalog.allClassDeclarations[className]
      if (declaration == null || declaration.abstract || declaration.dependencies.isNotEmpty()) {
        throw InvalidGameConfigException(
            "premise class must be a concrete dependency-free catalog Class: `$className`"
        )
      }
    }
    bootstrapClassName?.let { className ->
      val declaration =
          premiseDeclarationsByName[className] ?: catalog.allClassDeclarations[className]
      if (declaration == null || declaration.abstract || declaration.dependencies.isNotEmpty()) {
        throw InvalidGameConfigException(
            "bootstrap class must be a concrete dependency-free catalog Class: `$className`"
        )
      }
    }
  }

  /** The administrative Actor plus the seated Players. */
  public val actors: List<Actor>
    get() = playerNames.map(::Player) + ADMIN
}
