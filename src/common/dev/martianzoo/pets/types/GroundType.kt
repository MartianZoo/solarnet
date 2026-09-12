package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.Expression.Refinement.Has
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.AbsentRequirementValue
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Companion.split

/**
 * An ordinary resolved type, consisting of a [rootClass], one bound for every [dependencies] entry,
 * and an optional [refinement], as defined by
 * [rules T5-1 and T5-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
 *
 * "Ground" excludes type variables; it does not mean refinement-free. A narrowing judgment
 * involving a state-dependent refinement may need a world.
 *
 * Ground types are universe-scoped immutable values. Obtain one by resolving an [Expression] in a
 * [ClassTable], or by specializing a [Class].
 *
 * @constructor Combines a [rootClass], complete [dependencies], and optional [refinement] into one
 *   resolved type under
 *   [rule T5-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
 * @throws IllegalArgumentException if [dependencies] have different keys or belong to another
 *   universe.
 */
@ConsistentCopyVisibility
public data class GroundType
internal constructor(
    /**
     * The nominal root class specified by
     * [rule T5-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
     */
    override val rootClass: Class,

    /**
     * One bound for each dependency key of [rootClass], in the model of
     * [section 3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
     */
    override val dependencies: DependencySet,

    /**
     * The optional predicate specified by
     * [section 8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#8-refinements).
     */
    override val refinement: Refinement? = null,
    private val resolutionTable: ClassTable? = null,
) : Type {
  // Types are immutable; zero is the uncached sentinel (and a harmless rare recomputation).
  private var cachedHashCode: Int = 0
  private val structuralOverlapCache = mutableMapOf<GroundType, Boolean>()

  /**
   * The universe containing [rootClass], under
   * [rule T1-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  override val classTable: ClassTable =
      resolutionTable
          ?: dependencies.classTable?.let { dependencyTable ->
            requireNotNull(rootClass.classTable.commonTable(dependencyTable)) {
              "$rootClass and its dependencies belong to different class tables"
            }
          }
          ?: rootClass.classTable

  /**
   * This value itself, because a ground type is its own resolved interpretation ([rule
   * T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
   */
  override val groundType: GroundType
    get() = this

  /**
   * The component-targeting dependencies specified by
   * [rule T4-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#4-class-literals).
   */
  override val typeDependencies: List<Dependency.TypeDependency> = dependencies.typeDependencies()

  /**
   * The class represented by this `Class<Foo>` type, or null when this is not a class literal
   * ([rule
   * T4-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#4-class-literals)).
   */
  override val representedClass: Class? =
      if (rootClass.className == CLASS) dependencies.representedClass else null

  init {
    require(classTable.accepts(rootClass.classTable)) {
      "$rootClass cannot be interpreted by $classTable"
    }
    dependencies.classTable?.let { dependencyTable ->
      require(classTable.accepts(dependencyTable)) {
        "$dependencies cannot be interpreted by $classTable"
      }
    }
    require(dependencies.keys == rootClass.dependencies.keys) {
      "expected keys ${rootClass.dependencies.keys}, got $dependencies"
    }
    rootClass.requireVariableEqualitiesSatisfied(dependencies)
    if (refinement != null) classTable.checkAllTypes(refinement)
  }

  /**
   * Structural abstractness according to
   * [rule T5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override val abstract: Boolean = rootClass.abstract || dependencies.abstract || refinement != null

  /**
   * Returns [abstract]; a ground type's abstractness never consults [info] ([rule
   * T5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types)).
   */
  override fun isAbstract(info: TypeInfo): Boolean = abstract

  /**
   * Returns the concrete numeric value of [propertyName], as specified by
   * [rule T9-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties).
   */
  override fun getNumberPropertyValue(propertyName: String): Int =
      (rootClass.properties.getValue(PropertyName(propertyName)) as NumberValue).value

  /**
   * Returns the concrete metric value of [propertyName], as specified by
   * [rule T9-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties).
   */
  override fun getMetricPropertyValue(propertyName: String): Metric =
      (rootClass.properties.getValue(PropertyName(propertyName)) as MetricValue).value

  /**
   * Returns the concrete requirement value of [propertyName], or null for an absent optional, as
   * specified by
   * [rule T9-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties).
   */
  override fun getRequirementPropertyValue(propertyName: String): Requirement? =
      when (val value = rootClass.properties.getValue(PropertyName(propertyName))) {
        AbsentRequirementValue -> null
        is RequirementValue -> value.value
        else -> error("Property `$propertyName` is not a concrete Requirement value: $value")
      }

  /**
   * Performs the context-free subtype test of
   * [rules T6-1 and T8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   * Comparisons that reach a state-dependent refinement fail; use [narrows] with a world for those.
   */
  override fun isSubtypeOf(that: Type): Boolean = narrows(that, NoGameState)

  /**
   * The converse context-free subtype test specified by
   * [rule T6-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  override fun isSupertypeOf(that: Type): Boolean = that.isSubtypeOf(this)

  /**
   * Values supplied to selected class-header [variables] when this type specializes [general].
   * Variables unrelated to the header are omitted, following
   * [rule T13-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  override fun variableBindingsFrom(
      general: Type,
      variables: Iterable<TypeVariable>,
  ): Map<TypeVariable, GroundType> = rootClass.variableBindings(general.groundType, this, variables)

  /**
   * The greatest lower bound with [that], including the refinement rules, or null when absent
   * ([rules T7-1 and
   * T8-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#7-bounds)).
   */
  // TODO allocating 28 MB per solo game
  override infix fun glb(that: Type): GroundType? {
    val that = that.groundType
    val commonTable = requireSameClassTable(that)
    val glbClass = (rootClass glb that.rootClass) ?: return null
    val glbDeps = (dependencies glb that.dependencies) ?: return null
    val glbRefin =
        when {
          refinement == null -> that.refinement
          that.refinement == null -> refinement
          else -> Refinement.join(refinement, that.refinement)
        }
    val completeDeps = (glbClass.dependencies glb glbDeps) ?: return null
    val unrefined = glbClass.withAllDependencies(completeDeps).inTable(commonTable)
    return unrefined.refine(glbRefin)
  }

  internal fun specialize(
      specs: List<Expression>,
      classTable: ClassTable = this.classTable,
  ): GroundType =
      rootClass
          .withAllDependencies(dependencies.specialize(specs, classTable))
          .inTable(classTable)
          .refine(refinement)

  internal fun inTable(classTable: ClassTable): GroundType =
      if (this.classTable === classTable) this else copy(resolutionTable = classTable)

  internal fun refine(newRef: Refinement?): GroundType {
    val combined =
        when {
          refinement == null -> newRef
          newRef == null -> refinement
          else -> Refinement.join(refinement, newRef)
        }
    val applicable = combined?.retaining {
      it !is Not || overlapsStructurally(classTable.resolve(it.excluded))
    }
    return copy(refinement = applicable)
  }

  private val expressionLazy = lazy {
    toExpressionUsingSpecs(compactDependencyExpressions())
  }
  /**
   * The compact round-tripping expression specified by
   * [rule T5-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override val expression: Expression
    get() = expressionLazy.value

  private val expressionFullLazy = lazy {
    toExpressionUsingSpecs(dependencies.expressionsFull())
  }
  /**
   * The full round-tripping expression specified by
   * [rule T5-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override val expressionFull: Expression
    get() = expressionFullLazy.value

  private val narrowedDependenciesLazy = lazy {
    dependencies.minus(rootClass.dependencies)
  }
  /**
   * The bounds narrowed below [rootClass]'s base type, as defined by
   * [rule T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  override val narrowedDependencies: DependencySet
    get() = narrowedDependenciesLazy.value

  private fun compactDependencyExpressions(): List<Expression> {
    val keys = dependencies.keys
    val expressions = dependencies.expressions()
    val narrowed = narrowedDependencies.keys.toSet()
    val write = MutableList(keys.size) { keys[it] in narrowed }

    // A narrowed bound must be written or the narrowing is lost. An unnarrowed one must be written
    // only to occupy its slot: an omitted argument leaves its slot free, and rule T3-4 hands each
    // argument to the first free slot that accepts it, which could be an earlier one. Descending
    // means a slot promoted here is revisited before the loop ends, so one pass reaches a fixpoint.
    for (later in keys.indices.reversed()) {
      if (!write[later]) continue
      // An unwritten slot still holds its declared bound, which is what T3-4 matches against.
      for (earlier in later - 1 downTo 0) {
        if (
            !write[earlier] &&
                dependencies.get(keys[earlier]).intersect(expressions[later], classTable) != null
        ) {
          write[earlier] = true
        }
      }
    }
    var compact = expressions.filterIndexed { index, _ -> write[index] }

    // Equality propagation can make a written narrowing redundant; for example, the card bound in
    // Animal<Player1, Pets<Player1>> already determines Player1. Let specialization remain the one
    // authority on that inference, and discard an argument only when it proves the same Type.
    var index = 0
    while (index < compact.size) {
      val candidate = compact.filterIndexed { candidateIndex, _ -> candidateIndex != index }
      val resolvesBack =
          try {
            classTable.resolve(toExpressionUsingSpecs(candidate)) == this
          } catch (_: ExpressionException) {
            false
          }
      if (resolvesBack) {
        compact = candidate
        index = 0
      } else {
        index++
      }
    }
    return compact
  }

  private fun toExpressionUsingSpecs(specs: List<Expression>) = className.of(specs).has(refinement)

  /**
   * Enumerates every concrete structural candidate below this type's structural domain according to
   * [rules T11-1 and T11-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   * A `NOT` refinement filters the candidates because it is decided structurally; a `HAS`
   * refinement is left for a caller with a world to test. The sequence can be very large.
   */
  override fun allConcreteSubtypes(): Sequence<GroundType> {
    return classTable.allConcreteSubtypes(this)
  }

  /**
   * Returns the sole concrete narrowing in the master universe when every structural choice is
   * unique and its refinement accepts [info], as specified by
   * [rule T11-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  override fun singleConcreteSubtype(info: TypeInfo): GroundType? {
    return classTable.singleConcreteSubtype(this, info)
  }

  /** Returns the subset of [allConcreteSubtypes] having the exact same [rootClass] as ours. */
  // used publicly only by `desc random`
  internal fun concreteSubtypesSameClass(): Sequence<GroundType> =
      classTable.concreteSubtypesSameClass(this)

  /**
   * Asserts the contextual narrowing relation with [that], consulting [info] only for a `HAS`
   * refinement, as specified by
   * [rules T6-1, T6-2, and T8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  override fun ensureNarrows(that: Type, info: TypeInfo) {
    val that = that.groundType
    val comparisonTable = requireSameClassTable(that)
    rootClass.ensureNarrows(that.rootClass, info)

    dependencies.ensureNarrows(that.dependencies, info)

    that.refinement?.conjuncts()?.forEach { targetRefinement ->
      when (targetRefinement) {
        is Refinement.And -> error("nested refinement conjunction: $targetRefinement")
        is Not -> {
          if (
              !alreadyGuarantees(targetRefinement) &&
                  !isDisjointFrom(targetRefinement.excluded, comparisonTable)
          ) {
            throw NarrowingException("$this does not satisfy $targetRefinement")
          }
        }
        is Has -> {
          if (refinement != null) {
            if (!alreadyGuarantees(targetRefinement) || !readsPredicatesAlike(that)) {
              throw NarrowingException("$this does not have refinement $targetRefinement")
            }
          } else {
            val requirement =
                try {
                  formRequirement(
                      expressionFull,
                      that.expressionFull,
                      targetRefinement,
                      comparisonTable,
                  )
                } catch (e: ExpressionException) {
                  throw NarrowingException("$this does not satisfy $targetRefinement", e)
                }
            if (!info.has(requirement)) throw Exceptions.refinementNotMet(requirement)
          }
        }
      }
    }
  }

  // TODO solo game spending 19% of its time in this method, allocating over 10 MB!?
  /**
   * Tests contextual narrowing with [that], consulting [info] only for a `HAS` refinement, as
   * specified by
   * [rules T6-1, T6-2, and T8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  override fun narrows(that: Type, info: TypeInfo): Boolean {
    val that = that.groundType
    val comparisonTable = requireSameClassTable(that)
    if (!rootClass.isSubtypeOf(that.rootClass)) return false
    if (!dependencies.narrows(that.dependencies, info)) return false

    return that.refinement?.conjuncts()?.all { targetRefinement ->
      when (targetRefinement) {
        is Refinement.And -> error("nested refinement conjunction: $targetRefinement")
        is Not ->
            alreadyGuarantees(targetRefinement) ||
                isDisjointFrom(targetRefinement.excluded, comparisonTable)
        is Has -> {
          if (refinement != null) {
            alreadyGuarantees(targetRefinement) && readsPredicatesAlike(that)
          } else {
            val requirement =
                try {
                  formRequirement(
                      expressionFull,
                      that.expressionFull,
                      targetRefinement,
                      comparisonTable,
                  )
                } catch (_: ExpressionException) {
                  return false
                }
            info.has(requirement)
          }
        }
      }
    } ?: true
  }

  /**
   * Whether comparing our predicate with [that]'s as written is meaningful. It is, unless the two
   * are class literals for different classes: a refined class literal rewrites its own represented
   * class into its predicate before testing it, so the same words say different things about each
   * of them.
   */
  private fun readsPredicatesAlike(that: GroundType): Boolean =
      representedClass == null || representedClass == that.representedClass

  /** Whether our own refinement conjoins at least all of [target]'s requirements. */
  private fun alreadyGuarantees(target: Has): Boolean {
    val own =
        refinement
            ?.conjuncts()
            ?.filterIsInstance<Has>()
            ?.flatMap { split(it.requirement) }
            .orEmpty()
    return own.containsAll(split(target.requirement))
  }

  /** Whether our own refinement explicitly includes [target]. */
  private fun alreadyGuarantees(target: Not): Boolean =
      refinement?.conjuncts()?.contains(target) == true

  /** Whether this entire structural domain has no member in common with [excludedExpression]. */
  private fun isDisjointFrom(
      excludedExpression: Expression,
      comparisonTable: ClassTable,
  ): Boolean =
      !inTable(comparisonTable).overlapsStructurally(comparisonTable.resolve(excludedExpression))

  /** Whether at least one concrete structural Type inhabits both domains. */
  private fun overlapsStructurally(that: GroundType): Boolean =
      structuralOverlapCache.getOrPut(that) {
        val unrefined = copy(refinement = null)
        val unrefinedThat = that.copy(refinement = null)
        classTable.allStructuralConcreteSubtypes(unrefined).any { candidate ->
          candidate.narrows(unrefinedThat, NoGameState)
        }
      }

  private fun requireSameClassTable(that: GroundType): ClassTable {
    return requireNotNull(classTable.commonTable(that.classTable)) {
      "$this and $that belong to different class tables"
    }
  }

  /**
   * Compares and hashes the root class, dependencies, and refinement that determine identity in
   * [rule T5-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override fun equals(other: Any?): Boolean =
      this === other ||
          (other is GroundType &&
              rootClass == other.rootClass &&
              dependencies == other.dependencies &&
              refinement == other.refinement)

  override fun hashCode(): Int {
    if (cachedHashCode != 0) return cachedHashCode
    var result = rootClass.hashCode()
    result = 31 * result + dependencies.hashCode()
    result = 31 * result + (refinement?.hashCode() ?: 0)
    cachedHashCode = result
    return result
  }

  private fun formRequirement(
      narrow: Expression,
      wide: Expression,
      refinement: Has,
      comparisonTable: ClassTable,
  ): Requirement {

    fun refinementMangler(
        proposed: Expression,
        ignoreUnmatched: Boolean = false,
    ): PetTransformer {
      return object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          return if (node is Property && node.receiver == null) {
            node.copy(receiver = proposed)
          } else if (node is Metric.Rank && node.candidate == null) {
            node.copy(candidate = proposed)
          } else if (node is Expression) {
            val resolved = comparisonTable.resolve(node)
            val modded =
                try {
                  resolved.specialize(listOf(proposed), comparisonTable)
                } catch (e: ExpressionException) {
                  if (!ignoreUnmatched) throw e
                  resolved
                }
            modded.expressionFull
          } else {
            transformChildren(node)
          }
        }
      }
    }

    fun specializeRepresentedClassReferences(requirement: Requirement): Requirement {
      if (wide.className != CLASS) return requirement
      check(narrow.className == CLASS)
      val general = wide.arguments.single().className
      val specific = narrow.arguments.single().className
      return object : PetTransformer() {
            override fun transformNode(node: PetNode): PetNode =
                when {
                  node is Metric.Rank -> node
                  node is Expression && node.className == general ->
                      transformChildren(node.copy(className = specific))
                  else -> transformChildren(node)
                }
          }
          .transformRequirement(requirement)
    }

    val specializedRequirement = specializeRepresentedClassReferences(refinement.requirement)
    val transformed =
        refinementMangler(narrow, ignoreUnmatched = wide.className == CLASS)
            .transformRequirement(specializedRequirement)
    return transformed
  }

  /**
   * Returns the compact expression required by
   * [rule T5-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override fun toString(): String = "$expression"
}
