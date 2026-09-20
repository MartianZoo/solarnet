package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
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
 * includes the subset selected by one premise. Identity and structural operations remain
 * master-wide; concrete enumeration through a view is limited to that subset, as specified by
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
     *
     * @throws InvalidGameConfigException if the selected configuration cannot form a playable view
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
      table.freeze()
      table.validateNoOkSubscriptions()
      table.validateTransformKinds()
      table.includeAll(roots)
      val unexpectedModules =
          premise.catalog.modules.keys.filterTo(linkedSetOf()) { table.isIncluded(it) } -
              premise.modules
      if (unexpectedModules.isNotEmpty()) {
        throw InvalidGameConfigException(
            "structural activation selected unrequested modules: `$unexpectedModules`"
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
      if (inhabitedPlayerClassNames != premise.playerNames.toSet()) {
        throw InvalidGameConfigException(
            "inhabited `Player` classes do not match occupied seats: `$inhabitedPlayerClassNames`"
        )
      }
      val reactivated = excluded.filterTo(linkedSetOf(), table::isIncluded)
      if (reactivated.isNotEmpty()) {
        throw InvalidGameConfigException(
            "structural activation conflicts with excluded classes: `$reactivated`"
        )
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
  public fun transformDispatcher(): PetTransformer {
    val handlers = catalog.transformHandlerFactories.mapValues { (_, factory) -> factory(this) }
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

  /**
   * Returns the unique greatest common subclass of [left] and [right] in this universe, or null
   * when absent, following
   * [rule T2-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
   *
   * @throws IllegalArgumentException if either operand cannot be interpreted by this table (rule
   *   T1-2).
   */
  public fun glb(left: Class, right: Class): Class? {
    require(accepts(left.classTable) && accepts(right.classTable)) {
      "`$left` and `$right` cannot both be interpreted by this class table"
    }
    if (left.isSubtypeOf(right)) return left
    if (right.isSubtypeOf(left)) return right
    val lowerBounds = allStructuralSubclasses(left).filterTo(linkedSetOf(), right::isSupertypeOf)
    return lowerBounds.singleOrNull { candidate -> lowerBounds.all(candidate::isSupertypeOf) }
  }

  /**
   * Returns the greatest lower bound of [left] and [right] in this universe, or null when absent,
   * following
   * [rule T7-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#7-bounds).
   *
   * @throws IllegalArgumentException if either operand cannot be interpreted by this table (rule
   *   T1-2).
   */
  public fun glb(left: Type, right: Type): GroundType? = left.groundType.glbIn(right, this)

  internal fun glb(left: Dependency, right: Dependency): Dependency? = left.glb(right, this)

  internal fun glb(left: DependencySet, right: DependencySet): DependencySet? =
      left.merge(right) { a, b -> glb(a, b) ?: return null }

  /** Immutable component-count limits compiled for this table's included classes. */
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
   * Every class included by this view's premise closure; for a master table, the complete frozen
   * universe ([rules T1-6 and
   * T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance)).
   */
  public abstract fun allClasses(): Set<Class>

  /** Every nominal class in this combined universe, including classes excluded from its view. */
  internal abstract fun allKnownClasses(): Set<Class>

  /** Every class name included by this view's premise closure. */
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
      findClass(name) ?: throw ExpressionException("no class named `$name` in the current game")

  /**
   * Returns the class with canonical [name] when its base Type is inhabited in this universe, or
   * null when the name is unknown or its base Type is uninhabited.
   */
  public fun findInhabitedClass(name: ClassName): Class? = findClass(name)?.takeIf(::isInhabited)

  /**
   * Whether [name] is known and its class's base Type has at least one concrete narrowing in this
   * universe. Unknown names are not inhabited.
   */
  public fun isInhabited(name: ClassName): Boolean = findClass(name)?.let(::isInhabited) == true

  /**
   * Whether [klass] belongs to this master universe and its base Type has at least one concrete
   * narrowing in this universe.
   */
  public fun isInhabited(klass: Class): Boolean =
      accepts(klass.classTable) && isInhabited(klass.baseType)

  /**
   * Safely tests whether [type] belongs to the master universe backing this table, before
   * operations governed by the universe-mismatch rule
   * [T1-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  public fun knows(type: Type): Boolean = accepts(type.classTable)

  private val inhabitanceByType = mutableMapOf<GroundType, Boolean>()
  private var inhabitedConcreteClassesCache: Set<Class>? = null

  /**
   * Whether [type] has at least one concrete narrowing in this universe. This includes abstract
   * Types with an inhabited concrete specialization and excludes Types whose root, dependency, or
   * structural refinement leaves no concrete possibility.
   */
  public fun isInhabited(type: Type): Boolean {
    if (!knows(type)) return false
    val groundType = type.groundType
    val structuralRefinement = groundType.refinement?.retaining { it is Not }
    val cacheKey =
        if (structuralRefinement == groundType.refinement) groundType
        else groundType.copy(refinement = structuralRefinement)
    return inhabitanceByType.getOrPut(cacheKey) {
      allConcreteSubtypes(cacheKey).iterator().hasNext()
    }
  }

  /**
   * Every included concrete Class whose base Type is inhabited. The result is the greatest fixed
   * point because Class-literal dependencies may be mutually supporting: a Class representative
   * exists exactly when its represented Class is inhabited.
   */
  public fun allInhabitedConcreteClasses(): Set<Class> {
    inhabitedConcreteClassesCache?.let {
      return it
    }
    var possible = allClasses().filterNotTo(linkedSetOf(), Class::abstract)
    while (true) {
      val surviving =
          possible.filterTo(linkedSetOf()) { klass ->
            allConcreteSubtypes(klass.baseType, possible).iterator().hasNext()
          }
      if (surviving == possible) {
        inhabitedConcreteClassesCache = surviving
        return surviving
      }
      possible = surviving
    }
  }

  /** Whether premise construction included [name], independent of its Type's inhabitance. */
  internal fun isIncluded(name: ClassName): Boolean = name in allClassNames

  /** Whether premise construction included [klass], independent of its Type's inhabitance. */
  internal fun isIncluded(klass: Class): Boolean =
      accepts(klass.classTable) && isIncluded(klass.className)

  private val includedSubclassesByClass = mutableMapOf<Class, Set<Class>>()

  private val premiseClasses: Set<Class> by lazy {
    if (this === masterTable) emptySet()
    else allKnownClasses().filterTo(linkedSetOf()) { it.classTable === this }
  }

  /**
   * Included subclasses of [klass], including [klass] when included, under view-relative
   * enumeration in
   * [rules T2-7 and T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
   */
  public fun allSubclasses(klass: Class): Set<Class> {
    require(accepts(klass.classTable)) { "`$klass` belongs to a different Catalog" }
    if (this === masterTable) return allSubclassesOf(klass)
    return includedSubclassesByClass.getOrPut(klass) {
      klass.classTable.allSubclassesOf(klass).filterTo(linkedSetOf(), ::isIncluded).apply {
        if (klass.classTable === masterTable) {
          premiseClasses.filterTo(this) { candidate ->
            isIncluded(candidate) && candidate.isSubtypeOf(klass)
          }
        }
      }
    }
  }

  private val includedDirectSubclassesByClass = mutableMapOf<Class, Set<Class>>()

  private val structuralSubclassesByClass = mutableMapOf<Class, Set<Class>>()

  /** Clears answers whose domain changes while a premise's inclusion closure is growing. */
  internal fun invalidateInclusionCaches() {
    inhabitanceByType.clear()
    inhabitedConcreteClassesCache = null
    includedSubclassesByClass.clear()
    includedDirectSubclassesByClass.clear()
  }

  /** Every structurally possible subclass in this combined universe, independent of inclusion. */
  internal fun allStructuralSubclasses(klass: Class): Set<Class> {
    require(accepts(klass.classTable)) { "`$klass` belongs to a different Catalog" }
    return structuralSubclassesByClass.getOrPut(klass) {
      klass.classTable.allSubclassesOf(klass).toMutableSet().apply {
        if (klass.classTable === masterTable) {
          premiseClasses.filterTo(this) { candidate -> candidate.isSubtypeOf(klass) }
        }
      }
    }
  }

  /**
   * Included subclasses exactly one nominal step below [klass], under view-relative enumeration in
   * [rules T2-7 and T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#12-inhabitance).
   */
  public fun directSubclasses(klass: Class): Set<Class> {
    require(accepts(klass.classTable)) { "`$klass` belongs to a different Catalog" }
    if (this === masterTable) return directSubclassesOf(klass)
    return includedDirectSubclassesByClass.getOrPut(klass) {
      klass.classTable.directSubclassesOf(klass).filterTo(linkedSetOf(), ::isIncluded).apply {
        if (klass.classTable === masterTable) {
          premiseClasses.filterTo(this) { candidate ->
            isIncluded(candidate) && klass in candidate.directSuperclasses
          }
        }
      }
    }
  }

  internal abstract fun allSubclassesOf(klass: Class): Set<Class>

  internal abstract fun directSubclassesOf(klass: Class): Set<Class>

  /**
   * Enumerates inhabited concrete structural narrowings of [type], combining
   * [rules T11-1, T11-2, and T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  public fun allConcreteSubtypes(type: Type): Sequence<GroundType> {
    return allConcreteSubtypes(type, allInhabitedConcreteClasses())
  }

  internal fun allConcreteSubtypes(
      type: Type,
      inhabitedConcreteClasses: Set<Class>,
  ): Sequence<GroundType> {
    val type = type.groundType
    require(knows(type)) { "`$type` belongs to a different Catalog" }
    return concreteSubtypes(
        type,
        ::allSubclasses,
        { it in inhabitedConcreteClasses },
    ) { candidate ->
      candidate.dependencies.concreteSubtypesSameClass(
          candidate,
          this,
          inhabitedConcreteClasses,
      )
    }
  }

  /** Enumerates concrete structural types without applying this game's inclusion filter. */
  internal fun allStructuralConcreteSubtypes(type: Type): Sequence<GroundType> {
    val type = type.groundType
    require(knows(type)) { "`$type` belongs to a different Catalog" }
    return concreteSubtypes(
        type,
        ::allStructuralSubclasses,
        { true },
    ) { candidate ->
      candidate.dependencies.structuralConcreteSubtypesSameClass(candidate, this)
    }
  }

  /**
   * Enumerates inhabited concrete structural narrowings of [type], using [dependencyTargets]
   * instead of the full dependency domains as permitted by
   * [rule T11-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   * Each supplied target must be a concrete narrowing of the requested dependency type.
   */
  public fun allConcreteSubtypes(
      type: Type,
      dependencyTargets: (Type) -> Sequence<Type>,
  ): Sequence<GroundType> {
    val type = type.groundType
    require(knows(type)) { "`$type` belongs to a different Catalog" }
    val inhabitedConcreteClasses = allInhabitedConcreteClasses()
    return concreteSubtypes(
        type,
        ::allSubclasses,
        { it in inhabitedConcreteClasses },
    ) { candidate ->
      candidate.dependencies.concreteSubtypesSameClass(candidate, dependencyTargets)
    }
  }

  private fun concreteSubtypes(
      type: GroundType,
      subclasses: (Class) -> Set<Class>,
      concreteClassIsInhabited: (Class) -> Boolean,
      concretizeDependencies: (GroundType) -> Sequence<GroundType>,
  ): Sequence<GroundType> {
    val unrefined = type.copy(refinement = null)
    val candidates =
        subclasses(type.rootClass)
            .asSequence()
            .filterNot(Class::abstract)
            .filter(concreteClassIsInhabited)
            .flatMap { klass ->
              val dependencies = glb(unrefined.dependencies, klass.dependencies)
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
   * Enumerates inhabited concrete narrowings with the same root class as [type], combining
   * [rules T11-3 and T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  public fun concreteSubtypesSameClass(type: Type): Sequence<GroundType> {
    val type = type.groundType
    require(knows(type)) { "`$type` belongs to a different Catalog" }
    val inhabitedConcreteClasses = allInhabitedConcreteClasses()
    if (type.rootClass !in inhabitedConcreteClasses) return emptySequence()
    val unrefined = type.copy(refinement = null)
    val candidates =
        unrefined.dependencies.concreteSubtypesSameClass(unrefined, this, inhabitedConcreteClasses)
    return applyStructuralRefinement(type, candidates)
  }

  /**
   * Returns the sole inhabited concrete narrowing of [type] when every structural choice is unique
   * and its refinement accepts [info], combining
   * [rules T11-4 and T12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  public fun singleConcreteSubtype(type: Type, info: TypeInfo): GroundType? {
    val type = type.groundType
    if (type.rootClass.className == CLASS && type.refinement != null) {
      return allConcreteSubtypes(type).filter { it.narrows(type, info) }.take(2).singleOrNull()
    }

    val unrefined = type.copy(refinement = null)
    val inhabitedConcreteClasses = allInhabitedConcreteClasses()
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
            .filter { it in inhabitedConcreteClasses }
            .mapNotNull { klass -> glb(unrefined, klass.baseType) }
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
