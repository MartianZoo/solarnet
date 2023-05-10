package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Exceptions
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.Reifiable

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

  override fun isSubtypeOf(that: MType) = narrows(that) // TODO Hmmm

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

  internal fun specialize(specs: List<Expression>): MType =
      copy(dependencies = dependencies.specialize(specs))

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

  /** Returns the subset of [allConcreteSubtypes] having the exact same [root] as ours. */
  public fun concreteSubtypesSameClass(): Sequence<MType> =
      if (root.abstract) emptySequence() else dependencies.concreteSubtypesSameClass(this)

  internal fun concreteSubclasses(mclass: MClass) =
      mclass.allSubclasses.asSequence().filter { !it.abstract }

  override fun ensureNarrows(that: MType, info: TypeInfo) {
    root.ensureNarrows(that.root, info)
    dependencies.ensureNarrows(that.dependencies, info)

    val refin = that.refinement
    if (refin != null) {
      val requirement = root.table.transformers.refinementMangler(expression).transform(refin)
      if (!info.evaluate(requirement)) {
        throw Exceptions.refinementNotMet(requirement)
      }
    }
  }

  override fun narrows(that: Type, info: TypeInfo): Boolean {
    that as? MType ?: error("")
    if (!root.isSubtypeOf(that.root)) return false
    if (!dependencies.narrows(that.dependencies, info)) return false

    val refin = that.refinement ?: return true
    val requirement = root.table.transformers.refinementMangler(expression).transform(refin)
    return info.evaluate(requirement)
  }

  override fun toString() = "$expressionFull@${root.loader}"
}
