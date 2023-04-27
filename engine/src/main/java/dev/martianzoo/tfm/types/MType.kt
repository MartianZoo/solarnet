package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.api.UserException.InvalidReificationException
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.Reifiable
import dev.martianzoo.util.cartesianProduct

/**
 * The translation of a [Expression] into a "live" type, referencing actual [MClass]es loaded by a
 * [MClassLoader]. These are usually obtained by [MClassLoader.resolve]. These can be abstract.
 * Usages of this type should be fairly unrelated to questions of whether instances exist in a game
 * state.
 */
public data class MType
internal constructor(
    public val root: MClass,
    internal val dependencies: DependencySet,
    override val refinement: Requirement? = null,
) : Type, Hierarchical<MType>, Reifiable<MType>, HasClassName by root {
  internal val loader by root::loader
  internal val typeDependencies by dependencies::typeDependencies

  init {
    require(dependencies.keys.toList() == root.dependencies.keys.toList()) {
      "expected keys ${root.dependencies.keys}, got $dependencies"
    }
    if (refinement != null) loader.checkAllTypes(refinement)
  }

  override val abstract = root.abstract || dependencies.abstract || refinement != null

  override fun isSubtypeOf(that: Type) = isSubtypeOf(that as MType)

  override fun isSubtypeOf(that: MType) =
      // Not smart enough to understand whether our refinement specializes that's
      root.isSubtypeOf(that.root) &&
          dependencies.isSubtypeOf(that.dependencies) &&
          that.refinement in setOf(null, refinement)

  // Nearest common subtype
  override fun glb(that: MType): MType? {
    val glbClass = (root glb that.root) ?: return null
    val glbDeps = (dependencies glb that.dependencies) ?: return null
    val glbRefin = Requirement.join(this.refinement, that.refinement)
    return glbClass.withAllDependencies(glbDeps).refine(glbRefin)
  }

  // Nearest common supertype
  // Unlike glb, two types always have a least upper bound (if nothing else, Component)
  override fun lub(that: MType): MType =
      (root lub that.root)
          .withAllDependencies(dependencies lub that.dependencies)
          .refine(setOf(refinement, that.refinement).singleOrNull())

  internal fun specialize(specs: List<Expression>): MType {
    return if (isClassType) { // TODO reduce special-casing
      if (specs.size > 1) throw UserException.badClassExpression(specs)
      val classNameExpr = specs.singleOrNull() ?: COMPONENT.expression
      if (!classNameExpr.simple) throw UserException.badClassExpression(specs)
      loader.getClass(classNameExpr.className).classType
    } else {
      val deps =
          try {
            loader
                .matchPartial(specs, dependencies)
                .overlayOn(dependencies)
                .subMapInOrder(dependencies.keys)
          } catch (e: UserException) {
            throw UserException("Can't narrow ${this.expressionFull} with specs $specs", e)
          }
      copy(dependencies = deps)
    }
  }

  public fun refine(newRef: Requirement?): MType =
      copy(refinement = Requirement.join(refinement, newRef))

  override val expression: Expression by lazy {
    toExpressionUsingSpecs(narrowedDependencies.expressions)
  }

  override val expressionFull: Expression by lazy {
    toExpressionUsingSpecs(dependencies.expressionsFull)
  }

  internal val narrowedDependencies: DependencySet by lazy { dependencies.minus(root.dependencies) }

  private fun toExpressionUsingSpecs(specs: List<Expression>): Expression {
    val expression = root.className.of(specs).has(refinement)
    val roundTrip = loader.resolve(expression)
    require(roundTrip == this) { "$expression" }
    return expression
  }

  public fun supertypes(): List<MType> {
    val supers = root.allSuperclasses - loader.componentClass - root
    // the argument to wAD is allowed to be a superset
    return supers.map { it.withAllDependencies(dependencies) }
  }

  /**
   * Returns every possible [MType] `t` such that `!t.abstract && t.isSubtypeOf(this)`. Note that
   * this sequence can potentially be very large.
   */
  public fun allConcreteSubtypes(): Sequence<MType> {
    return concreteSubclasses(root).flatMap {
      val deps: DependencySet? = dependencies glb it.baseType.dependencies
      if (deps == null) {
        emptySequence()
      } else {
        it.withAllDependencies(deps).concreteSubtypesSameClass()
      }
    }
  }

  public val isClassType: Boolean = root.className == CLASS

  /** Returns the subset of [allConcreteSubtypes] having the exact same [root] as ours. */
  public fun concreteSubtypesSameClass(): Sequence<MType> {
    // TODO reduce special-casing
    return when {
      root.abstract -> emptySequence()
      isClassType -> concreteSubclasses(dependencies.getClassForClassType()).map { it.classType }
      else -> {
        val axes = dependencies.typeDependencies.map { it.allConcreteSpecializations().toList() }
        val product = axes.cartesianProduct()
        product.map { root.withAllDependencies(DependencySet.of(it)) }
      }
    }
  }

  private fun concreteSubclasses(mclass: MClass) =
      mclass.allSubclasses.asSequence().filter { !it.abstract }

  override fun ensureReifies(abstractTarget: MType) {
    super<Reifiable>.ensureReifies(abstractTarget)
  }

  fun stripRefinements(): MType =
      root.withAllDependencies(dependencies.map { it.stripRefinements() })

  override fun ensureNarrows(that: MType) {
    if (!isSubtypeOf(that.stripRefinements())) throw InvalidReificationException("$this $that")
  }

  override fun toString() = "$expressionFull@${root.loader}"

  fun findSubstitutions(linkages: Set<ClassName>) =
      findSubstitutions(linkages, root.baseType.expressionFull, expressionFull)
}

/** Decides what substitutions should be made to class effects to yield component effects. */
fun findSubstitutions(
    linkages: Set<ClassName>,
    general: Expression,
    specific: Expression,
): Map<ClassName, Expression> {
  val map = mutableMapOf<ClassName, Expression>()
  doIt(linkages, map, general, specific)
  return map
}

private fun doIt(
    linkages: Set<ClassName>,
    map: MutableMap<ClassName, Expression>,
    general: Expression,
    specific: Expression,
) {
  for ((g, s) in general.arguments.zip(specific.arguments)) {
    if (g.simple && g.className in linkages && s != g) {
      map[g.className] = s
    } else {
      doIt(linkages, map, g, s)
    }
  }
}
