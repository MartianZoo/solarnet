package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.api.TypeInfo.NoGameState
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.Requirement.Or
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.Reifiable

/**
 * The translation of a [Expression] into a "live" type, referencing actual [Class]es loaded by a
 * [ClassTable]. These are usually obtained by [ClassTable.resolve]. These can be abstract. Usages
 * of this type should be fairly unrelated to questions of whether instances exist in a game state.
 */
public data class Type(
    val rootClass: Class,
    val dependencies: DependencySet,
    val refinement: Refinement? = null,
) : HasExpression, Hierarchical<Type>, Reifiable<Type>, HasClassName by rootClass {

  public val classTable: ClassTable = rootClass.classTable
  public val typeDependencies: Set<Dependency.TypeDependency> = dependencies.typeDependencies()

  init {
    dependencies.classTable?.let {
      require(classTable === it) {
        "$rootClass and its dependencies belong to different class tables"
      }
    }
    require(dependencies.keys.toList() == rootClass.dependencies.keys.toList()) {
      "expected keys ${rootClass.dependencies.keys}, got $dependencies"
    }
    rootClass.requireLinksSatisfied(dependencies)
    if (refinement != null) classTable.checkAllTypes(refinement)
  }

  override val abstract: Boolean = rootClass.abstract || dependencies.abstract || refinement != null

  /**
   * Performs a context-free subtype check. Comparisons that reach a state-dependent refinement
   * throw; use [narrows] in a [TypeInfo] for those.
   */
  override fun isSubtypeOf(that: Type): Boolean = narrows(that, NoGameState)

  // Nearest common subtype
  // TODO allocating 28 MB per solo game
  override fun glb(that: Type): Type? {
    requireSameClassTable(that)
    val glbClass = (rootClass glb that.rootClass) ?: return null
    val glbDeps = (dependencies glb that.dependencies) ?: return null
    val glbRefin = Refinement.join(this.refinement, that.refinement)
    return glbClass.withAllDependencies(glbDeps).refine(glbRefin)
  }

  // Nearest common supertype
  // Unlike glb, two types always have a least upper bound (if nothing else, Component)
  override fun lub(that: Type): Type {
    requireSameClassTable(that)
    val unrefined: Type =
        (rootClass lub that.rootClass).withAllDependencies(dependencies lub that.dependencies)

    return if (refinement != null && that.refinement == null) {
      unrefined.refine(refinement)
    } else if (that.refinement != null && refinement == null) {
      unrefined.refine(that.refinement)
    } else {
      unrefined
    }
  }

  internal fun specialize(specs: List<Expression>): Type =
      rootClass.withAllDependencies(dependencies.specialize(specs)).refine(refinement)

  public fun refine(newRef: Refinement?): Type =
      copy(refinement = Refinement.join(refinement, newRef))

  override val expression: Expression by lazy {
    toExpressionUsingSpecs(narrowedDependencies.expressions())
  }

  override val expressionFull: Expression by lazy {
    toExpressionUsingSpecs(dependencies.expressionsFull())
  }

  public val narrowedDependencies: DependencySet by lazy {
    dependencies.minus(rootClass.dependencies)
  }

  private fun toExpressionUsingSpecs(specs: List<Expression>) = className.of(specs).has(refinement)

  /**
   * Returns every possible [Type] `t` such that `!t.abstract && t.isSubtypeOf(this)`. Note that
   * this sequence can potentially be very large.
   */
  public fun allConcreteSubtypes(): Sequence<Type> {
    return concreteSubclasses(rootClass).flatMap {
      val deps: DependencySet? = dependencies glb it.baseType.dependencies
      if (deps == null) {
        emptySequence()
      } else {
        it.withAllDependencies(deps).concreteSubtypesSameClass()
      }
    }
  }

  public fun singleConcreteSubtype(): Type? {
    val klass = concreteSubclasses(rootClass).singleOrNull() ?: return null
    val abstractType = this glb klass.baseType
    val deps = abstractType!!.dependencies.singleConcreteSubtype() ?: return null
    return abstractType.rootClass.withAllDependencies(deps)
  }

  /** Returns the subset of [allConcreteSubtypes] having the exact same [rootClass] as ours. */
  // used publicly only by `desc random`
  public fun concreteSubtypesSameClass(): Sequence<Type> =
      if (rootClass.abstract) emptySequence() else dependencies.concreteSubtypesSameClass(this)

  internal fun concreteSubclasses(baseClass: Class) =
      baseClass.allSubclasses().asSequence().filter { !it.abstract }

  override fun ensureNarrows(that: Type, info: TypeInfo) {
    requireSameClassTable(that)
    rootClass.ensureNarrows(that.rootClass, info)
    dependencies.ensureNarrows(that.dependencies, info)

    if (that.refinement != null) {
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
  public fun narrows(that: Type, info: TypeInfo): Boolean {
    requireSameClassTable(that)
    if (!rootClass.isSubtypeOf(that.rootClass)) return false
    if (!dependencies.narrows(that.dependencies, info)) return false

    that.refinement ?: return true
    val requirement =
        try {
          formRequirement(expressionFull, that.expressionFull)
        } catch (_: ExpressionException) {
          return false
        }
    return info.has(requirement)
  }

  private fun requireSameClassTable(that: Type) {
    require(classTable === that.classTable) {
      "$this and $that belong to different class tables"
    }
  }

  private fun formRequirement(narrow: Expression, wide: Expression): Requirement {

    fun refinementMangler(proposed: Expression): PetTransformer {
      return object : PetTransformer() {
        override fun <P : PetNode> transform(node: P): P {
          return if (node is Expression) {
            val modded = classTable.resolve(node).specialize(listOf(proposed))
            @Suppress("UNCHECKED_CAST")
            modded.expressionFull as P
          } else {
            transformChildren(node)
          }
        }
      }
    }

    val refin = wide.refinement!!
    val transformed = refinementMangler(narrow).transform(refin.requirement)
    return if (refin.forgiving) {
      Or(transformed, Max(scaledEx(0, wide.copy(refinement = refin.copy(forgiving = false)))))
    } else {
      transformed
    }
  }

  override fun toString(): String = "$expression"
}
