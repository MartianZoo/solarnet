package dev.martianzoo.tfm.types

import com.google.common.collect.Lists
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.util.Hierarchical

/**
 * The translation of a [Expression] into a "live" type, referencing actual [PClass]es loaded by a
 * [PClassLoader]. These are usually obtained by [PClassLoader.resolve]. These can be abstract.
 * Usages of this type should be fairly unrelated to questions of whether instances exist in a game
 * state.
 */
public data class PType
internal constructor(
    public val pclass: PClass, // TODO try renaming root?
    internal val dependencies: DependencySet = DependencySet(setOf()),
    override val refinement: Requirement? = null,
) : Type, Hierarchical<PType> {
  private val loader by pclass::loader

  init {
    require(dependencies.keys.toList() == pclass.allDependencyKeys.toList()) {
      "expected keys ${pclass.allDependencyKeys}, got $dependencies"
    }
    if (refinement != null) loader.checkAllTypes(refinement)
  }

  override val abstract = pclass.abstract || dependencies.abstract || refinement != null

  override fun isSubtypeOf(that: Type) = isSubtypeOf(that as PType)

  override fun isSubtypeOf(that: PType) =
      pclass.isSubtypeOf(that.pclass) &&
          dependencies.isSubtypeOf(that.dependencies) &&
          that.refinement in setOf(null, refinement)

  // Nearest common subtype
  override fun glb(that: PType): PType? =
      (pclass glb that.pclass)
          ?.withExactDependencies(dependencies glb that.dependencies)
          ?.refine(conjoin(this.refinement, that.refinement)) // BIGTODO glb/lub for Req

  // Nearest common supertype
  // Unlike glb, two types always have a least upper bound (if nothing else, Component)
  override fun lub(that: PType): PType =
      (pclass lub that.pclass)
          .withExactDependencies(dependencies lub that.dependencies)
          .refine(setOf(refinement, that.refinement).singleOrNull())

  internal fun specialize(specs: List<Expression>): PType {
    return if (isClassType) { // TODO reduce special-casing
      if (specs.isEmpty()) {
        loader.componentClass.classType
      } else {
        val spec = specs.single().also { require(it.simple) }
        loader.getClass(spec.className).classType
      }
    } else {
      val deps = loader.match(specs, dependencies).overlayOn(dependencies)
      copy(dependencies = deps.subMapInOrder(dependencies.keys))
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

  public fun refine(newRef: Requirement?): PType = copy(refinement = conjoin(refinement, newRef))

  override val expression: Expression by lazy {
    toExpressionUsingSpecs(narrowedDependencies.expressions)
  }

  override val expressionFull: Expression by lazy {
    toExpressionUsingSpecs(dependencies.expressionsFull)
  }

  internal val narrowedDependencies: DependencySet by lazy {
    dependencies.minus(pclass.baseType.dependencies)
  }

  private fun toExpressionUsingSpecs(specs: List<Expression>): Expression {
    val expression = pclass.className.addArgs(specs).refine(refinement)
    val roundTrip = loader.resolve(expression)
    require(roundTrip == this) { "$expressionFull -> ${roundTrip.expressionFull}" }
    return expression
  }

  public fun supertypes(): List<PType> {
    val supers = pclass.allSuperclasses - loader.componentClass - pclass
    // the argument to wAD is allowed to be a superset
    return supers.map { it.withExactDependencies(dependencies) }
  }

  /**
   * Returns every possible [PType] `t` such that `!t.abstract && t.isSubtypeOf(this)`. Note that
   * this sequence can potentially be very large.
   */
  public fun allConcreteSubtypes(): Sequence<PType> {
    if (refinement != null) return emptySequence()
    return concreteSubclasses(pclass).flatMap {
      val top = it.withExactDependencies(dependencies glb it.baseType.dependencies)
      top.concreteSubtypesSameClass()
    }
  }

  public val isClassType: Boolean = pclass.className == CLASS

  /** If [isClassType], return the class it's a class type of. */
  internal fun getClassForClassType(): PClass = dependencies.getClassForClassType()

  /** Returns the subset of [allConcreteSubtypes] having the exact same [pclass] as ours. */
  public fun concreteSubtypesSameClass(): Sequence<PType> {
    return if (refinement != null) {
      emptySequence()
    } else if (isClassType) { // TODO reduce special-casing
      concreteSubclasses(getClassForClassType()).map { it.classType }
    } else {
      val axes: List<List<Dependency>> =
          dependencies.asSet.map { it.allConcreteSpecializations().toList() }
      val product: List<List<Dependency>> = Lists.cartesianProduct(axes)
      product.asSequence().map { pclass.withExactDependencies(DependencySet(it.toSet())) }
    }
  }

  private fun concreteSubclasses(pclass: PClass) =
      pclass.allSubclasses.asSequence().filter { !it.abstract }

  override fun toString() = "$expressionFull@${pclass.loader}"
}
