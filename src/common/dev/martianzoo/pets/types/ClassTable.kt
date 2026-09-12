package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.Exceptions
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Dependency.TypeDependency

/**
 * Either the complete immutable class universe compiled from one catalog or a playable view that
 * marks a subset of that universe active. Identity and structural operations remain master-wide;
 * enumeration through a view is active-only, as specified by
 * [sections 1 and 12](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
 *
 * @constructor Creates a table implementation for one master universe or one of its views, under
 *   the identity rules in
 *   [section 1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
 */
public abstract class ClassTable {
  /** Construction operations for class-table views. */
  public companion object {
    /**
     * Forms and freezes the playable view selected by [premise], reusing its catalog's
     * master-universe objects as required by
     * [rules T12-1 through T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
     */
    public fun forPremise(premise: GamePremise): ClassTable {
      val premiseTable = premise.premiseClassTable
      val masterTable = premiseTable.master
      val initialClassNames =
          premise.initialComponentTypes.flatMap { it.descendantsOfType<ClassName>() }.toSet()
      val configurationNames: Set<ClassName> =
          premise.modules +
              premise.classSelections
                  .filter(ClassSelection::included)
                  .map(ClassSelection::className) +
              premise.playerNames +
              initialClassNames
      val moduleSelections = premise.modules.flatMap { premise.catalog.modules.getValue(it) }
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
          premise.classSelections
              .filter(ClassSelection::included)
              .mapTo(linkedSetOf(), ClassSelection::className)
      val explicitlyExcluded =
          premise.classSelections
              .filterNot(ClassSelection::included)
              .mapTo(linkedSetOf(), ClassSelection::className)
      val excluded = (moduleExcluded - explicitlyIncluded) + explicitlyExcluded
      val roots =
          premise.modules +
              ((selectedByModules - explicitlyExcluded) + explicitlyIncluded) +
              initialClassNames +
              premise.actors.map(Actor::className) +
              listOfNotNull(premise.bootstrapClassName, premise.premiseClassName)

      val table =
          ClassLoader.projection(
                  premise.catalog,
                  premiseTable,
                  premise.modules,
                  premise.classSelections,
              )
              .apply { loadAll(roots) }
              .freeze()
      val unexpectedModules =
          premise.catalog.modules.keys.filterTo(linkedSetOf()) { table.isActive(it) } -
              premise.modules
      require(unexpectedModules.isEmpty()) {
        "structural activation selected unrequested Modules: $unexpectedModules"
      }
      val playerClass = masterTable.findClass(PLAYER)
      val activePlayerClassNames =
          playerClass
              ?.let(table::allSubclasses)
              .orEmpty()
              .filterNot(Class::abstract)
              .mapTo(linkedSetOf(), Class::className)
      require(activePlayerClassNames == premise.playerNames.toSet()) {
        "active Player classes do not match occupied seats: $activePlayerClassNames"
      }
      val reactivated = excluded.filterTo(linkedSetOf(), table::isActive)
      require(reactivated.isEmpty()) {
        "structural activation conflicts with excluded classes: $reactivated"
      }
      PremiseViability.validate(table, roots)
      return table
    }
  }

  /** The Catalog whose compiled class universe backs this table. */
  internal abstract val catalog: Catalog

  /**
   * Creates a dispatcher for the selected catalog-defined syntax transformations. The table gives
   * those transformations the single universe required by
   * [rule T1-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity);
   * transformation semantics are outside the type-system specification.
   */
  public fun transformDispatcher(
      kinds: Set<String> = catalog.transformHandlerFactories.keys,
  ): PetTransformer {
    val handlers =
        catalog.transformHandlerFactories.filterKeys(kinds::contains).mapValues { (_, factory) ->
          factory(this)
        }
    return TransformHandler.dispatcher(handlers)
  }

  /** The Catalog-scoped table whose compiled class universe backs this projection. */
  internal abstract val masterTable: ClassTable

  /**
   * Returns the narrower table that can interpret values from both receivers, or null when the
   * values belong to unrelated premise or master universes.
   */
  internal fun commonTable(that: ClassTable): ClassTable? =
      when {
        this === that -> this
        this === that.masterTable -> that
        that === masterTable -> this
        else -> null
      }

  /** Whether values owned by [that] can participate in operations interpreted by this table. */
  internal fun accepts(that: ClassTable): Boolean =
      this === that || (this !== masterTable && that === masterTable)

  /** Immutable component-count limits compiled for the classes active in this table. */
  private val componentLimitsLazy = lazy { ClassLimitTable.create(this) }

  /**
   * The component-count limits that enforce the single-target dependency invariant in
   * [rule T3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public val componentLimits: ClassLimitTable
    get() = componentLimitsLazy.value

  /**
   * The required `Component` root specified by
   * [rule T1-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  public abstract val componentClass: Class

  /**
   * The required `Class` class specified by
   * [rule T1-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  public abstract val classClass: Class

  /**
   * Every active class in this view; for a master table, the complete frozen universe ([rules T1-6
   * and
   * T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance)).
   */
  public abstract fun allClasses(): Set<Class>

  /** Every nominal class in this combined universe, including classes inactive in its view. */
  internal abstract fun allKnownClasses(): Set<Class>

  /**
   * Every active class name in this view, under the three-state model of
   * [rule T12-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
   */
  public abstract val allClassNames: Set<ClassName>

  /**
   * Returns the class currently available by canonical [name], or null when unknown or not yet
   * loaded. In a frozen table this includes uninhabited catalog classes, under
   * [rules T1-6, T1-7, and T12-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
   */
  public abstract fun findClass(name: ClassName): Class?

  /**
   * Returns the class currently available by canonical [name], including an uninhabited class in a
   * frozen table, or reports the unknown-name expression error of
   * [rules T1-6, T1-7, and T12-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
   *
   * @throws ExpressionException if [name] is unknown.
   */
  public fun getClass(name: ClassName): Class =
      findClass(name) ?: throw Exceptions.classNotFound(name)

  /**
   * Returns the active class with canonical [name], or null when it is unknown or uninhabited
   * ([rule
   * T12-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance)).
   */
  public fun findActiveClass(name: ClassName): Class? = findClass(name)?.takeIf(::isActive)

  /**
   * Whether [name] names an active class in this view ([rule
   * T12-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance)).
   */
  public fun isActive(name: ClassName): Boolean = name in allClassNames

  /**
   * Whether [klass] belongs to this master universe and is active in this view ([rules T12-2 and
   * T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance)).
   */
  public fun isActive(klass: Class): Boolean =
      accepts(klass.classTable) && klass.className in allClassNames

  /**
   * Safely tests whether [type] belongs to the master universe backing this table, before
   * operations governed by the universe-mismatch rule
   * [T1-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  public fun knows(type: Type): Boolean = accepts(type.classTable)

  /**
   * Whether [type]'s root and every bound dependency are active in this view, exactly as defined by
   * [rule T12-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
   */
  public fun isActive(type: Type): Boolean =
      knows(type) && isActive(type.rootClass) && type.dependencies.activeIn(this)

  private val activeSubclassesByClass = mutableMapOf<Class, Set<Class>>()

  private val premiseClasses: Set<Class> by lazy {
    if (this === masterTable) emptySet()
    else allKnownClasses().filterTo(linkedSetOf()) { it.classTable === this }
  }

  /**
   * Active subclasses of [klass], including [klass] when active, under view-relative enumeration in
   * [rule T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
   */
  public fun allSubclasses(klass: Class): Set<Class> {
    require(accepts(klass.classTable)) { "$klass belongs to a different Catalog" }
    if (this === masterTable) return klass.allSubclasses()
    return activeSubclassesByClass.getOrPut(klass) {
      klass.allSubclasses().filterTo(linkedSetOf(), ::isActive).apply {
        if (klass.classTable === masterTable) {
          premiseClasses.filterTo(this) { candidate ->
            isActive(candidate) && candidate.isSubtypeOf(klass)
          }
        }
      }
    }
  }

  private val activeDirectSubclassesByClass = mutableMapOf<Class, Set<Class>>()

  private val structuralSubclassesByClass = mutableMapOf<Class, Set<Class>>()

  /** Every structurally possible subclass in this combined universe, independent of activation. */
  internal fun allStructuralSubclasses(klass: Class): Set<Class> {
    require(accepts(klass.classTable)) { "$klass belongs to a different Catalog" }
    return structuralSubclassesByClass.getOrPut(klass) {
      klass.allSubclasses().toMutableSet().apply {
        if (klass.classTable === masterTable) {
          premiseClasses.filterTo(this) { candidate -> candidate.isSubtypeOf(klass) }
        }
      }
    }
  }

  /**
   * Active subclasses exactly one nominal step below [klass], under view-relative enumeration in
   * [rule T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
   */
  public fun directSubclasses(klass: Class): Set<Class> {
    require(accepts(klass.classTable)) { "$klass belongs to a different Catalog" }
    if (this === masterTable) return klass.directSubclasses()
    return activeDirectSubclassesByClass.getOrPut(klass) {
      klass.directSubclasses().filterTo(linkedSetOf(), ::isActive).apply {
        if (klass.classTable === masterTable) {
          premiseClasses.filterTo(this) { candidate ->
            isActive(candidate) && klass in candidate.directSuperclasses
          }
        }
      }
    }
  }

  /**
   * Enumerates active concrete structural narrowings of [type], combining
   * [rules T11-1, T11-2, and T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  public fun allConcreteSubtypes(type: Type): Sequence<GroundType> {
    val type = type.groundType
    require(knows(type)) { "$type belongs to a different Catalog" }
    return concreteSubtypes(type, ::allSubclasses) { candidate ->
      concreteSubtypesSameClass(candidate)
    }
  }

  /** Enumerates concrete structural types without applying this game's active-class filter. */
  internal fun allStructuralConcreteSubtypes(type: Type): Sequence<GroundType> {
    val type = type.groundType
    require(knows(type)) { "$type belongs to a different Catalog" }
    return concreteSubtypes(type, ::allStructuralSubclasses) { candidate ->
      candidate.dependencies.structuralConcreteSubtypesSameClass(candidate, this)
    }
  }

  /**
   * Enumerates active concrete structural narrowings of [type], using [dependencyTargets] instead
   * of the full dependency domains as permitted by
   * [rule T11-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   * Each supplied target must be a concrete narrowing of the requested dependency type.
   */
  public fun allConcreteSubtypes(
      type: Type,
      dependencyTargets: (Type) -> Sequence<Type>,
  ): Sequence<GroundType> {
    val type = type.groundType
    require(knows(type)) { "$type belongs to a different Catalog" }
    return concreteSubtypes(type, ::allSubclasses) { candidate ->
      candidate.dependencies.concreteSubtypesSameClass(candidate, dependencyTargets)
    }
  }

  private fun concreteSubtypes(
      type: GroundType,
      subclasses: (Class) -> Set<Class>,
      concretizeDependencies: (GroundType) -> Sequence<GroundType>,
  ): Sequence<GroundType> {
    val unrefined = type.copy(refinement = null)
    val candidates =
        subclasses(type.rootClass).asSequence().filterNot(Class::abstract).flatMap { klass ->
          val dependencies = unrefined.dependencies glb klass.dependencies
          if (dependencies == null) {
            emptySequence()
          } else {
            concretizeDependencies(klass.withAllDependencies(dependencies))
          }
        }
    return applyStructuralRefinement(type, candidates)
  }

  private fun applyStructuralRefinement(
      requested: GroundType,
      candidates: Sequence<GroundType>,
  ): Sequence<GroundType> {
    val structuralRefinement = requested.refinement?.retaining { it is Not }
    return if (structuralRefinement == null) {
      candidates
    } else {
      val structuralType = requested.copy(refinement = structuralRefinement)
      candidates.filter { it.narrows(structuralType, NoGameState) }
    }
  }

  /**
   * Enumerates active concrete narrowings with the same root class as [type], combining
   * [rules T11-3 and T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  public fun concreteSubtypesSameClass(type: Type): Sequence<GroundType> {
    val type = type.groundType
    require(knows(type)) { "$type belongs to a different Catalog" }
    if (type.rootClass.abstract || !isActive(type.rootClass)) return emptySequence()
    val unrefined = type.copy(refinement = null)
    val candidates =
        unrefined.dependencies.concreteSubtypesSameClass(unrefined, this).filter(::isActive)
    return applyStructuralRefinement(type, candidates)
  }

  /**
   * Returns the sole active concrete narrowing of [type] when every structural choice is unique and
   * its refinement accepts [info], combining
   * [rules T11-4 and T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  public fun singleConcreteSubtype(type: Type, info: TypeInfo): GroundType? {
    val type = type.groundType
    if (type.rootClass.className == CLASS && type.refinement != null) {
      return allConcreteSubtypes(type).filter { it.narrows(type, info) }.take(2).singleOrNull()
    }

    val unrefined = type.copy(refinement = null)
    val structuralRefinement = type.refinement?.retaining { it is Not }
    if (structuralRefinement != null) {
      val structuralType = type.copy(refinement = structuralRefinement)
      val candidate = allConcreteSubtypes(structuralType).take(2).singleOrNull() ?: return null
      return candidate.takeIf { it.narrows(type, info) }
    }
    val intersection =
        allSubclasses(type.rootClass)
            .asSequence()
            .filterNot(Class::abstract)
            .mapNotNull { klass -> unrefined glb klass.baseType }
            .take(2)
            .singleOrNull() ?: return null
    val dependencies = intersection.dependencies.singleConcreteSubtype(info, this) ?: return null
    val candidate = intersection.rootClass.withAllDependencies(dependencies)
    return candidate.takeIf { it.narrows(type, info) }
  }

  /**
   * Resolves [expression] to a universe-scoped ground type under
   * [rules T1-3 and T1-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   *
   * @throws ExpressionException if [expression] is invalid in this universe.
   */
  public abstract fun resolve(expression: Expression): GroundType

  /**
   * Resolves every type expression in [node], reporting any expression error defined by
   * [rules T1-7, T3-5, T4-6, and T8-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   *
   * @throws ExpressionException if any expression in [node] is invalid in this universe.
   */
  public fun checkAllTypes(node: PetNode): Unit = node.visitDescendants {
    if (it is Expression) {
      resolve(it).expression
      false
    } else {
      true
    }
  }

  /**
   * Tests [candidate] against [constraint] interpreted within [domain] and [info], exactly
   * implementing constrained narrowing in
   * [rule T6-6](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  public fun matchesConstraint(
      candidate: Type,
      constraint: Expression,
      domain: Type,
      info: TypeInfo,
  ): Boolean {
    require(knows(candidate) && knows(domain)) {
      "constraint types belong to a different Catalog"
    }
    val key = Key(domain.className, 0)
    val domainDependency = TypeDependency(key, domain.groundType)
    val constrained = domainDependency.intersect(constraint, this) ?: return false
    return TypeDependency(key, candidate.groundType).narrows(constrained, info)
  }
}
