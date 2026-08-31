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
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.AbsentRequirementValue
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.Requirement.Or
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx

/**
 * The translation of a [Expression] into a "live" type, referencing actual [Class]es loaded by a
 * [ClassTable]. These are usually obtained by [ClassTable.resolve]. These can be abstract. Usages
 * of this type should be fairly unrelated to questions of whether instances exist in a world.
 */
public data class GroundType(
    override val rootClass: Class,
    override val dependencies: DependencySet,
    override val refinement: Refinement? = null,
) : Type {
  // Types are immutable; zero is the uncached sentinel (and a harmless rare recomputation).
  private var cachedHashCode: Int = 0

  override val classTable: ClassTable = rootClass.classTable
  override val groundType: GroundType
    get() = this

  override val typeDependencies: Set<Dependency.TypeDependency> = dependencies.typeDependencies()

  /** The class represented by this `Class<Foo>` type, or null when this is not a class literal. */
  override val representedClass: Class? =
      if (rootClass.className == CLASS) dependencies.representedClass else null

  init {
    dependencies.classTable?.let {
      require(classTable === it) {
        "$rootClass and its dependencies belong to different class tables"
      }
    }
    require(dependencies.keys.toList() == rootClass.dependencies.keys.toList()) {
      "expected keys ${rootClass.dependencies.keys}, got $dependencies"
    }
    rootClass.requireVariableEqualitiesSatisfied(dependencies)
    if (refinement != null) classTable.checkAllTypes(refinement)
  }

  override val abstract: Boolean = rootClass.abstract || dependencies.abstract || refinement != null

  override fun isAbstract(info: TypeInfo): Boolean = abstract

  /** Returns the concrete numeric value of the class property named [propertyName]. */
  override fun getNumberPropertyValue(propertyName: String): Int =
      (rootClass.properties.getValue(PropertyName(propertyName)) as NumberValue).value

  /** Returns the concrete metric-valued class property named [propertyName]. */
  override fun getMetricPropertyValue(propertyName: String): Metric =
      (rootClass.properties.getValue(PropertyName(propertyName)) as MetricValue).value

  /** Returns the concrete requirement-valued class property named [propertyName], if present. */
  override fun getRequirementPropertyValue(propertyName: String): Requirement? =
      when (val value = rootClass.properties.getValue(PropertyName(propertyName))) {
        AbsentRequirementValue -> null
        is RequirementValue -> value.value
        else -> error("Property `$propertyName` is not a concrete Requirement value: $value")
      }

  /**
   * Performs a context-free subtype check. Comparisons that reach a state-dependent refinement
   * throw; use [narrows] in a [TypeInfo] for those.
   */
  override fun isSubtypeOf(that: Type): Boolean = narrows(that, NoGameState)

  /** Performs the converse context-free subtype check. */
  override fun isSupertypeOf(that: Type): Boolean = that.isSubtypeOf(this)

  /**
   * Values supplied to [variables] when this Type specializes [general]. Variables unrelated to the
   * Class header are omitted.
   */
  override fun variableBindingsFrom(
      general: Type,
      variables: Iterable<TypeVariable>,
  ): Map<TypeVariable, GroundType> = rootClass.variableBindings(general.groundType, this, variables)

  // Nearest common subtype
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
    return glbClass.withAllDependencies(glbDeps).refine(glbRefin)
  }

  // Nearest common supertype
  // Unlike glb, two types always have a least upper bound (if nothing else, Component)
  override infix fun lub(that: Type): GroundType {
    val that = that.groundType
    requireSameClassTable(that)
    val unrefined: GroundType =
        (rootClass lub that.rootClass).withAllDependencies(dependencies lub that.dependencies)

    return unrefined.refine(refinement.takeIf { it == that.refinement })
  }

  internal fun specialize(specs: List<Expression>): GroundType =
      rootClass.withAllDependencies(dependencies.specialize(specs)).refine(refinement)

  internal fun refine(newRef: Refinement?): GroundType =
      copy(
          refinement =
              when {
                refinement == null -> newRef
                newRef == null -> refinement
                else -> requireNotNull(Refinement.join(refinement, newRef))
              }
      )

  override val expression: Expression by lazy {
    toExpressionUsingSpecs(minimalDependencyExpressions())
  }

  override val expressionFull: Expression by lazy {
    toExpressionUsingSpecs(dependencies.expressionsFull())
  }

  override val narrowedDependencies: DependencySet by lazy {
    dependencies.minus(rootClass.dependencies)
  }

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
   * Returns every possible [Type] `t` such that `!t.abstract && t.isSubtypeOf(this)`. Note that
   * this sequence can potentially be very large.
   */
  override fun allConcreteSubtypes(): Sequence<GroundType> {
    return concreteSubclasses(rootClass).flatMap {
      val deps: DependencySet? = dependencies glb it.baseType.dependencies
      if (deps == null) {
        emptySequence()
      } else {
        it.withAllDependencies(deps).concreteSubtypesSameClass()
      }
    }
  }

  /**
   * Returns the sole structural concrete narrowing, if one exists and satisfies this type's
   * state-dependent constraints according to [info].
   */
  override fun singleConcreteSubtype(info: TypeInfo): GroundType? {
    if (rootClass.className == CLASS && refinement != null) {
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

  override fun ensureNarrows(that: Type, info: TypeInfo) {
    val that = that.groundType
    requireSameClassTable(that)
    rootClass.ensureNarrows(that.rootClass, info)

    if (that.refinement != null && refinement != null && refinement != that.refinement) {
      throw NarrowingException("$this does not have refinement ${that.refinement}")
    }
    dependencies.ensureNarrows(that.dependencies, info)

    if (that.refinement != null && refinement == null) {
      val requirement =
          try {
            formRequirement(expressionFull, that.expressionFull)
          } catch (e: ExpressionException) {
            throw NarrowingException("$this does not satisfy ${that.refinement}", e)
          }
      if (!info.has(requirement)) {
        throw Exceptions.refinementNotMet(requirement)
      }
    }
  }

  // TODO solo game spending 19% of its time in this method, allocating over 10 MB!?
  /** Performs a state-aware narrowing check using [info]. */
  override fun narrows(that: Type, info: TypeInfo): Boolean {
    val that = that.groundType
    requireSameClassTable(that)
    if (!rootClass.isSubtypeOf(that.rootClass)) return false
    if (that.refinement != null && refinement != null && refinement != that.refinement) return false
    if (!dependencies.narrows(that.dependencies, info)) return false

    that.refinement ?: return true
    if (refinement != null) return true
    val requirement =
        try {
          formRequirement(expressionFull, that.expressionFull)
        } catch (_: ExpressionException) {
          return false
        }
    return info.has(requirement)
  }

  private fun requireSameClassTable(that: GroundType) {
    require(classTable === that.classTable) { "$this and $that belong to different class tables" }
  }

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
            override fun transformNode(node: PetNode): PetNode {
              val specialized =
                  if (node is Expression && node.className == general) {
                    node.copy(className = specific)
                  } else {
                    node
                  }
              return transformChildren(specialized)
            }
          }
          .transformRequirement(requirement)
    }

    val refin = wide.refinement!!
    val specializedRequirement = specializeRepresentedClassReferences(refin.requirement)
    val transformed =
        refinementMangler(narrow, ignoreUnmatched = narrow.className == CLASS)
            .transformRequirement(specializedRequirement)
    return if (refin.forgiving) {
      Or(
          transformed,
          Max(scaledEx(wide.copy(refinement = refin.copy(forgiving = false)), 0)),
      )
    } else {
      transformed
    }
  }

  override fun toString(): String = "$expression"
}
