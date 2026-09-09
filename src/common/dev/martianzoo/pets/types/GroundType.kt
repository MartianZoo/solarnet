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
 * [rules 5-1 and 5-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
 *
 * "Ground" excludes type variables; it does not mean refinement-free. A narrowing judgment
 * involving a state-dependent refinement may need a world.
 *
 * Ground types are universe-scoped immutable values. Obtain one by resolving an [Expression] in a
 * [ClassTable], or by specializing a [Class].
 *
 * @constructor Combines a [rootClass], complete [dependencies], and optional [refinement] into one
 *   resolved type under
 *   [rule 5-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
 * @throws IllegalArgumentException if [dependencies] have different keys or belong to another
 *   universe.
 */
public data class GroundType(
    /**
     * The nominal root class specified by
     * [rule 5-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
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
) : Type {
  // Types are immutable; zero is the uncached sentinel (and a harmless rare recomputation).
  private var cachedHashCode: Int = 0
  private val structuralOverlapCache = mutableMapOf<GroundType, Boolean>()

  /**
   * The universe containing [rootClass], under
   * [rule 1-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  override val classTable: ClassTable = rootClass.classTable

  /**
   * This value itself, because a ground type is its own resolved interpretation ([rule
   * 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
   */
  override val groundType: GroundType
    get() = this

  /**
   * The component-targeting dependencies specified by
   * [rule 4-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#4-class-literals).
   */
  override val typeDependencies: List<Dependency.TypeDependency> = dependencies.typeDependencies()

  /**
   * The class represented by this `Class<Foo>` type, or null when this is not a class literal
   * ([rule
   * 4-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#4-class-literals)).
   */
  override val representedClass: Class? =
      if (rootClass.className == CLASS) dependencies.representedClass else null

  init {
    dependencies.classTable?.let {
      require(classTable === it) {
        "$rootClass and its dependencies belong to different class tables"
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
   * [rule 5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override val abstract: Boolean = rootClass.abstract || dependencies.abstract || refinement != null

  /**
   * Returns [abstract]; a ground type's abstractness never consults [info] ([rule
   * 5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types)).
   */
  override fun isAbstract(info: TypeInfo): Boolean = abstract

  /**
   * Returns the concrete numeric value of [propertyName], as specified by
   * [rule 9-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties).
   */
  override fun getNumberPropertyValue(propertyName: String): Int =
      (rootClass.properties.getValue(PropertyName(propertyName)) as NumberValue).value

  /**
   * Returns the concrete metric value of [propertyName], as specified by
   * [rule 9-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties).
   */
  override fun getMetricPropertyValue(propertyName: String): Metric =
      (rootClass.properties.getValue(PropertyName(propertyName)) as MetricValue).value

  /**
   * Returns the concrete requirement value of [propertyName], or null for an absent optional, as
   * specified by
   * [rule 9-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties).
   */
  override fun getRequirementPropertyValue(propertyName: String): Requirement? =
      when (val value = rootClass.properties.getValue(PropertyName(propertyName))) {
        AbsentRequirementValue -> null
        is RequirementValue -> value.value
        else -> error("Property `$propertyName` is not a concrete Requirement value: $value")
      }

  /**
   * Performs the context-free subtype test of
   * [rules 6-1 and 8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   * Comparisons that reach a state-dependent refinement fail; use [narrows] with a world for those.
   */
  override fun isSubtypeOf(that: Type): Boolean = narrows(that, NoGameState)

  /**
   * The converse context-free subtype test specified by
   * [rule 6-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  override fun isSupertypeOf(that: Type): Boolean = that.isSubtypeOf(this)

  /**
   * Values supplied to selected class-header [variables] when this type specializes [general].
   * Variables unrelated to the header are omitted, following
   * [rule 13-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  override fun variableBindingsFrom(
      general: Type,
      variables: Iterable<TypeVariable>,
  ): Map<TypeVariable, GroundType> = rootClass.variableBindings(general.groundType, this, variables)

  /**
   * The greatest lower bound with [that], including the refinement rules, or null when absent
   * ([rules 7-1 and
   * 8-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#7-bounds)).
   */
  // TODO allocating 28 MB per solo game
  override infix fun glb(that: Type): GroundType? {
    val that = that.groundType
    requireSameClassTable(that)
    val glbClass = (rootClass glb that.rootClass) ?: return null
    val glbDeps = (dependencies glb that.dependencies) ?: return null
    val glbRefin =
        when {
          refinement == null -> that.refinement
          that.refinement == null -> refinement
          else -> Refinement.join(refinement, that.refinement) ?: return null
        }
    val unrefined = glbClass.withAllDependencies(glbDeps)
    if (glbRefin is Not) {
      val excluded = classTable.resolve(glbRefin.excluded)
      if (!unrefined.overlapsStructurally(excluded)) return unrefined
    }
    return unrefined.refine(glbRefin)
  }

  /**
   * A minimal common supertype with [that], including the refinement rules ([rules 7-2 and
   * 8-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#7-bounds)).
   */
  override infix fun lub(that: Type): GroundType {
    val that = that.groundType
    requireSameClassTable(that)
    val unrefined: GroundType =
        (rootClass lub that.rootClass).withAllDependencies(dependencies lub that.dependencies)

    return unrefined.refine(refinement.takeIf { it == that.refinement })
  }

  internal fun specialize(specs: List<Expression>): GroundType =
      rootClass.withAllDependencies(dependencies.specialize(specs)).refine(refinement)

  internal fun refine(newRef: Refinement?): GroundType {
    val combined =
        when {
          refinement == null -> newRef
          newRef == null -> refinement
          else -> requireNotNull(Refinement.join(refinement, newRef))
        }
    return if (combined is Not && !overlapsStructurally(classTable.resolve(combined.excluded))) {
      copy(refinement = null)
    } else {
      copy(refinement = combined)
    }
  }

  private val expressionLazy = lazy {
    toExpressionUsingSpecs(minimalDependencyExpressions())
  }
  /**
   * The minimal round-tripping expression specified by
   * [rule 5-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override val expression: Expression
    get() = expressionLazy.value

  private val expressionFullLazy = lazy {
    toExpressionUsingSpecs(dependencies.expressionsFull())
  }
  /**
   * The full round-tripping expression specified by
   * [rule 5-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override val expressionFull: Expression
    get() = expressionFullLazy.value

  private val narrowedDependenciesLazy = lazy {
    dependencies.minus(rootClass.dependencies)
  }
  /**
   * The bounds narrowed below [rootClass]'s base type, as defined by
   * [rule 3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  override val narrowedDependencies: DependencySet
    get() = narrowedDependenciesLazy.value

  private fun minimalDependencyExpressions(): List<Expression> {
    val candidates = dependencies.expressions()

    fun expressionsAt(indices: Collection<Int>) = indices.sorted().map(candidates::get)

    fun resolvesToThis(indices: Collection<Int>): Boolean = runCatching {
      rootClass.specialize(expressionsAt(indices)).dependencies == dependencies
    }
        .getOrDefault(false)

    for (argumentCount in 0..candidates.size) {
      fun find(start: Int, selected: List<Int>): List<Expression>? {
        if (selected.size == argumentCount) {
          return expressionsAt(selected).takeIf { resolvesToThis(selected) }
        }
        val remaining = argumentCount - selected.size
        for (index in start..candidates.size - remaining) {
          find(index + 1, selected + index)?.let {
            return it
          }
        }
        return null
      }
      find(0, emptyList())?.let {
        return it
      }
    }
    return candidates
  }

  private fun toExpressionUsingSpecs(specs: List<Expression>) = className.of(specs).has(refinement)

  /**
   * Enumerates every concrete structural candidate below this type's structural domain according to
   * [rules 11-1 and 11-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   * A `NOT` refinement filters the candidates because it is decided structurally; a `HAS`
   * refinement is left for a caller with a world to test. The sequence can be very large.
   */
  override fun allConcreteSubtypes(): Sequence<GroundType> {
    val candidates =
        concreteSubclasses(rootClass).flatMap {
          val deps: DependencySet? = dependencies glb it.baseType.dependencies
          if (deps == null) {
            emptySequence()
          } else {
            it.withAllDependencies(deps).concreteSubtypesSameClass()
          }
        }
    return if (refinement is Not) {
      candidates.filter { it.narrows(this, NoGameState) }
    } else {
      candidates
    }
  }

  /**
   * Returns the sole concrete narrowing in the master universe when every structural choice is
   * unique and its refinement accepts [info], as specified by
   * [rule 11-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  override fun singleConcreteSubtype(info: TypeInfo): GroundType? {
    if ((rootClass.className == CLASS && refinement != null) || refinement is Not) {
      return allConcreteSubtypes().filter { it.narrows(this, info) }.take(2).singleOrNull()
    }
    val intersection =
        concreteSubclasses(rootClass).mapNotNull { klass -> this glb klass.baseType }.singleOrNull()
            ?: return null
    val deps = intersection.dependencies.singleConcreteSubtype(info) ?: return null
    val candidate = intersection.rootClass.withAllDependencies(deps)
    return candidate.takeIf { !it.abstract && it.narrows(this, info) }
  }

  /** Returns the subset of [allConcreteSubtypes] having the exact same [rootClass] as ours. */
  // used publicly only by `desc random`
  internal fun concreteSubtypesSameClass(): Sequence<GroundType> =
      if (rootClass.abstract) emptySequence() else dependencies.concreteSubtypesSameClass(this)

  internal fun concreteSubclasses(baseClass: Class) =
      baseClass.allSubclasses().asSequence().filter { !it.abstract }

  /**
   * Asserts the contextual narrowing relation with [that], consulting [info] only for a `HAS`
   * refinement, as specified by
   * [rules 6-1, 6-2, and 8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  override fun ensureNarrows(that: Type, info: TypeInfo) {
    val that = that.groundType
    requireSameClassTable(that)
    rootClass.ensureNarrows(that.rootClass, info)

    dependencies.ensureNarrows(that.dependencies, info)

    when (val targetRefinement = that.refinement) {
      null -> Unit
      refinement.takeIf { readsPredicatesAlike(that) } -> Unit
      is Not -> {
        if (!isDisjointFrom(targetRefinement.excluded)) {
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
                formRequirement(expressionFull, that.expressionFull)
              } catch (e: ExpressionException) {
                throw NarrowingException("$this does not satisfy $targetRefinement", e)
              }
          if (!info.has(requirement)) throw Exceptions.refinementNotMet(requirement)
        }
      }
    }
  }

  // TODO solo game spending 19% of its time in this method, allocating over 10 MB!?
  /**
   * Tests contextual narrowing with [that], consulting [info] only for a `HAS` refinement, as
   * specified by
   * [rules 6-1, 6-2, and 8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  override fun narrows(that: Type, info: TypeInfo): Boolean {
    val that = that.groundType
    requireSameClassTable(that)
    if (!rootClass.isSubtypeOf(that.rootClass)) return false
    if (!dependencies.narrows(that.dependencies, info)) return false

    return when (val targetRefinement = that.refinement) {
      null -> true
      refinement.takeIf { readsPredicatesAlike(that) } -> true
      is Not -> isDisjointFrom(targetRefinement.excluded)
      is Has -> {
        if (refinement != null) {
          return alreadyGuarantees(targetRefinement) && readsPredicatesAlike(that)
        }
        val requirement =
            try {
              formRequirement(expressionFull, that.expressionFull)
            } catch (_: ExpressionException) {
              return false
            }
        info.has(requirement)
      }
    }
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
    val own = refinement as? Has ?: return false
    return split(own.requirement).containsAll(split(target.requirement))
  }

  /** Whether this entire structural domain has no member in common with [excludedExpression]. */
  private fun isDisjointFrom(excludedExpression: Expression): Boolean =
      !overlapsStructurally(classTable.resolve(excludedExpression))

  /** Whether at least one concrete structural Type inhabits both domains. */
  private fun overlapsStructurally(that: GroundType): Boolean =
      structuralOverlapCache.getOrPut(that) {
        copy(refinement = null).allConcreteSubtypes().any { candidate ->
          candidate.narrows(that, NoGameState)
        }
      }

  private fun requireSameClassTable(that: GroundType) {
    require(classTable === that.classTable) { "$this and $that belong to different class tables" }
  }

  /**
   * Hashes the root class, dependencies, and refinement that determine identity in
   * [rule 5-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override fun hashCode(): Int {
    if (cachedHashCode != 0) return cachedHashCode
    var result = rootClass.hashCode()
    result = 31 * result + dependencies.hashCode()
    result = 31 * result + (refinement?.hashCode() ?: 0)
    cachedHashCode = result
    return result
  }

  private fun formRequirement(narrow: Expression, wide: Expression): Requirement {

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
            val resolved = classTable.resolve(node)
            val modded =
                try {
                  resolved.specialize(listOf(proposed))
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

    val refin = wide.refinement as Has
    val specializedRequirement = specializeRepresentedClassReferences(refin.requirement)
    val transformed =
        refinementMangler(narrow, ignoreUnmatched = wide.className == CLASS)
            .transformRequirement(specializedRequirement)
    return transformed
  }

  /**
   * Returns the minimal expression required by
   * [rule 5-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override fun toString(): String = "$expression"
}
