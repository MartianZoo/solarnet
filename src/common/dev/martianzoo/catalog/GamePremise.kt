package dev.martianzoo.catalog

import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.SystemClasses.ADMIN
import dev.martianzoo.pets.api.SystemClasses.AUDIT
import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassLoader
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.PremiseClassTable

/**
 * The complete immutable, resolved input from which equivalent playable worlds are constructed.
 *
 * [classSelections] records every signed Class choice, [componentAdjustments] changes Module-owned
 * setup quantities, and [playerNames] fixes seat order. [classTable] forms their inclusion closure
 * over the Catalog's reusable master table and derives executable effects using fixed structural
 * Class presence and selected Module counts. Known Classes outside that closure remain uninhabited;
 * closure that reaches an excluded Class or an unrequested Module is invalid.
 *
 * @throws InvalidGameConfigException if its fields cannot describe a playable game configuration
 */
public data class GamePremise(
    public val catalog: Catalog,
    public val classSelections: Set<ClassSelection>,
    /** Signed setup-component changes applied after selected Modules are created. */
    public val componentAdjustments: Map<ClassName, Int> = emptyMap(),
    /** Concrete Player Class Names in seat order. */
    public val playerNames: List<ClassName> = emptyList(),
    /** Concrete Component created by Admin immediately before the generated premise Class. */
    public val bootstrapClassName: ClassName? = null,
    /** Concrete configuration Class created during bootstrap, when the Catalog supplies one. */
    public val premiseClassName: ClassName? = null,
    /** Generated and ad-hoc declarations owned only by this premise. */
    public val premiseClassDeclarations: Set<ClassDeclaration> = emptySet(),
) {
  /** Selected ambient-rule Classes, derived from the unified signed Class selections. */
  public val modules: Set<ClassName>
    get() =
        classSelections
            .asSequence()
            .filter(ClassSelection::included)
            .map(ClassSelection::className)
            .filterTo(linkedSetOf(), catalog.modules::containsKey)

  /** The premise-local declaration delta over the Catalog's reusable master table. */
  private val premiseClassTableLazy = lazy {
    PremiseClassTable(catalog.classTable, premiseClassDeclarations)
  }
  private val premiseClassTable: PremiseClassTable
    get() = premiseClassTableLazy.value

  /**
   * The immutable premise-selected class-table view shared by every World built from this premise.
   * Its normal effect lookup is premise-specialized; each Class declaration remains the reusable
   * backing form.
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
    val configurationNames: Set<ClassName> =
        classSelections
            .filter(ClassSelection::included)
            .mapTo(linkedSetOf(), ClassSelection::className) +
            playerNames +
            componentAdjustments.keys
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
            componentAdjustments.keys +
            playerNames +
            ADMIN +
            listOfNotNull(bootstrapClassName, premiseClassName)

    val table =
        ClassLoader.forPremise(
            premiseTable = premiseTable,
            roots = roots,
            additionalRequiredClasses = {
              catalog.customClassDependencies[it.className].orEmpty()
            },
            checkAvailability = ::checkAvailability,
            exactCount = ::configuredCount,
            selectedModuleCount = ::selectedModuleCount,
        )
    val unexpectedModules =
        catalog.modules.keys.filterTo(linkedSetOf()) { it in table.allClassNames } - modules
    if (unexpectedModules.isNotEmpty()) {
      throw InvalidGameConfigException(
          "structural selection included unrequested modules: ${unexpectedModules.joinToString { "`$it`" }}"
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
          "inhabited `Player` classes do not match occupied seats: ${inhabitedPlayerClassNames.joinToString { "`$it`" }}"
      )
    }
    val includedExclusions = excluded.filterTo(linkedSetOf(), table.allClassNames::contains)
    if (includedExclusions.isNotEmpty()) {
      throw InvalidGameConfigException(
          "structural selection conflicts with excluded classes: ${includedExclusions.joinToString { "`$it`" }}"
      )
    }
    PremiseViability.validate(table, roots)
    return table
  }

  private fun checkAvailability(className: ClassName, requiredBy: ClassName?) {
    val availabilityModules = catalog.classAvailabilityModules[className] ?: return
    if (availabilityModules.intersect(modules).isNotEmpty()) return
    val path =
        requiredBy?.let { "`$it` requires locked class `$className`" }
            ?: "class `$className` is locked"
    throw InvalidGameConfigException(
        "broken game premise: $path; required bundle modules: ${availabilityModules.joinToString { "`$it`" }}"
    )
  }

  private fun configuredCount(expression: Expression, table: ClassTable): Int? {
    selectedModuleCount(expression, table)?.let {
      return it
    }
    if (!expression.simple || expression.className == THIS) return null
    val countedClass = table.getClass(expression.className)
    if (table.findClass(PLAYER)?.let(countedClass::isSubtypeOf) == true) {
      return playerNames.count { name -> table.getClass(name).isSubtypeOf(countedClass) }
    }
    val concreteSubclassNames = concreteSubclassNames(countedClass, table)
    if (concreteSubclassNames.isEmpty()) return null
    val selections = classSelections.associateBy(ClassSelection::className)
    if (!selections.keys.containsAll(concreteSubclassNames)) return null
    return selections.values.count { selection ->
      selection.included && table.getClass(selection.className).isSubtypeOf(countedClass)
    }
  }

  /** Counts selected Modules whose presence is a fact of this complete premise. */
  private fun selectedModuleCount(expression: Expression, table: ClassTable): Int? {
    if (!expression.simple || expression.className == THIS) return null
    val countedClass = table.getClass(expression.className)
    val concreteSubclassNames = concreteSubclassNames(countedClass, table)
    if (
        concreteSubclassNames.isNotEmpty() &&
            catalog.modules.keys.containsAll(concreteSubclassNames)
    ) {
      return modules.count { moduleName ->
        table.getClass(moduleName).isSubtypeOf(countedClass)
      }
    }
    return null
  }

  private fun concreteSubclassNames(countedClass: Class, table: ClassTable): Set<ClassName> {
    val masterSubclasses =
        if (countedClass.classTable === catalog.classTable)
            catalog.classTable.allSubclasses(countedClass)
        else emptySet()
    val premiseSubclasses =
        premiseClassTable.declarations.keys
            .asSequence()
            .map(table::getClass)
            .filter { candidate -> candidate.isSubtypeOf(countedClass) }
            .toSet()
    return (masterSubclasses + premiseSubclasses)
        .filterNot(Class::abstract)
        .mapTo(linkedSetOf(), Class::className)
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
          "player names must be concrete `Player` classes: ${invalidPlayerNames.joinToString { "`$it`" }}"
      )
    }
    if (playerNames.distinct().size != playerNames.size) {
      throw InvalidGameConfigException(
          "duplicate player names: ${playerNames.joinToString { "`$it`" }}"
      )
    }
    if (selectedNames.distinct().size != selectedNames.size) {
      throw InvalidGameConfigException(
          "duplicate individual class selections: ${selectedNames.joinToString { "`$it`" }}"
      )
    }
    if (classSelections.any { it.requirement != null }) {
      throw InvalidGameConfigException(
          "individual class selections cannot be conditional: " +
              classSelections
                  .filter { it.requirement != null }
                  .joinToString {
                    "`${it.className}` (condition `${it.requirement}`)"
                  }
      )
    }
    if (selectedNames.any { it !in allKnownNames }) {
      throw InvalidGameConfigException(
          "individual class selections are absent from the premise catalog: " +
              (selectedNames - allKnownNames).joinToString { "`$it`" }
      )
    }
    if (componentAdjustments.keys.any { it !in allKnownNames }) {
      throw InvalidGameConfigException(
          "component adjustments name classes absent from the premise catalog: " +
              (componentAdjustments.keys - allKnownNames).joinToString { "`$it`" }
      )
    }
    premiseClassName?.let { className ->
      val declaration =
          premiseDeclarationsByName[className] ?: catalog.allClassDeclarations[className]
      if (declaration == null || declaration.abstract || declaration.dependencies.isNotEmpty()) {
        throw InvalidGameConfigException(
            "premise class must be a concrete dependency-free catalog class: `$className`"
        )
      }
    }
    bootstrapClassName?.let { className ->
      val declaration =
          premiseDeclarationsByName[className] ?: catalog.allClassDeclarations[className]
      if (declaration == null || declaration.abstract || declaration.dependencies.isNotEmpty()) {
        throw InvalidGameConfigException(
            "bootstrap class must be a concrete dependency-free catalog class: `$className`"
        )
      }
    }
  }
}
