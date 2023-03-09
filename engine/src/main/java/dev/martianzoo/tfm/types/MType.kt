package dev.martianzoo.tfm.types

import com.google.common.collect.Lists.cartesianProduct
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.util.Hierarchical

/**
 * The translation of a [Expression] into a "live" type, referencing actual [MClass]es loaded by a
 * [MClassLoader]. These are usually obtained by [MClassLoader.resolve]. These can be abstract.
 * Usages of this type should be fairly unrelated to questions of whether instances exist in a game
 * state.
 */
public data class MType
internal constructor(
    public val mclass: MClass, // TODO try renaming root?
    internal val dependencies: DependencySet,
    override val refinement: Requirement? = null,
) : Type, Hierarchical<MType>, HasClassName by mclass {
  private val loader by mclass::loader

  init {
    require(dependencies.keys.toList() == mclass.dependencies.keys.toList()) {
      "expected keys ${mclass.dependencies.keys}, got $dependencies"
    }
    if (refinement != null) loader.checkAllTypes(refinement)
  }

  override val abstract = mclass.abstract || dependencies.abstract || refinement != null

  override fun isSubtypeOf(that: Type) = isSubtypeOf(that as MType)

  override fun isSubtypeOf(that: MType) =
      mclass.isSubtypeOf(that.mclass) &&
          dependencies.isSubtypeOf(that.dependencies) &&
          that.refinement in setOf(null, refinement)

  // Nearest common subtype
  override fun glb(that: MType): MType? =
      (mclass glb that.mclass)
          ?.withExactDependencies(dependencies glb that.dependencies)
          ?.refine(conjoin(this.refinement, that.refinement)) // BIGTODO glb/lub for Req

  // Nearest common supertype
  // Unlike glb, two types always have a least upper bound (if nothing else, Component)
  override fun lub(that: MType): MType =
      (mclass lub that.mclass)
          .withExactDependencies(dependencies lub that.dependencies)
          .refine(setOf(refinement, that.refinement).singleOrNull())

  internal fun specialize(specs: List<Expression>): MType {
    return if (isClassType) { // TODO reduce special-casing
      if (specs.isEmpty()) {
        loader.componentClass.classType
      } else {
        val spec = specs.single().also { require(it.simple) }
        loader.getClass(spec.className).classType
      }
    } else {
      copy(dependencies = loader.matchFull(specs, dependencies))
    }
  }

  private fun conjoin(one: Requirement?, two: Requirement?): Requirement? {
    val x = setOfNotNull(one, two)
    return when (x.size) {
      0 -> null
      1 -> x.first()
      2 -> And(x.toList())
      else -> error("imposserous")
    }
  }

  public fun refine(newRef: Requirement?): MType = copy(refinement = conjoin(refinement, newRef))

  override val expression: Expression by lazy {
    toExpressionUsingSpecs(narrowedDependencies.expressions)
  }

  override val expressionFull: Expression by lazy {
    toExpressionUsingSpecs(dependencies.expressionsFull)
  }

  internal val narrowedDependencies: DependencySet by lazy {
    dependencies.minus(mclass.dependencies)
  }

  private fun toExpressionUsingSpecs(specs: List<Expression>): Expression {
    val expression = mclass.className.addArgs(specs).refine(refinement)
    val roundTrip = loader.resolve(expression)
    require(roundTrip == this) { "$expression" }
    return expression
  }

  public fun supertypes(): List<MType> {
    val supers = mclass.allSuperclasses - loader.componentClass - mclass
    // the argument to wAD is allowed to be a superset
    return supers.map { it.withExactDependencies(dependencies) }
  }

  /**
   * Returns every possible [MType] `t` such that `!t.abstract && t.isSubtypeOf(this)`. Note that
   * this sequence can potentially be very large.
   */
  public fun allConcreteSubtypes(): Sequence<MType> {
    if (refinement != null) return emptySequence()
    return concreteSubclasses(mclass).flatMap {
      val top = it.withExactDependencies(dependencies glb it.baseType.dependencies)
      top.concreteSubtypesSameClass()
    }
  }

  public val isClassType: Boolean = mclass.className == CLASS // TODO reduce special-casing

  /** Returns the subset of [allConcreteSubtypes] having the exact same [mclass] as ours. */
  public fun concreteSubtypesSameClass(): Sequence<MType> {
    return when {
      mclass.abstract || refinement != null -> emptySequence()
      isClassType -> concreteSubclasses(dependencies.getClassForClassType()).map { it.classType }
      else -> {
        val axes = dependencies.asSet.map { it.allConcreteSpecializations().toList() }
        val product: List<List<Dependency>> = cartesianProduct(axes)
        product.asSequence().map { mclass.withExactDependencies(DependencySet.of(it)) }
      }
    }
  }

  private fun concreteSubclasses(mclass: MClass) =
      mclass.allSubclasses.asSequence().filter { !it.abstract }

  override fun toString() = "$expressionFull@${mclass.loader}"
}