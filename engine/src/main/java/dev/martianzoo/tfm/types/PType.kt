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
    internal val dependencies: DependencyMap = DependencyMap(),
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
          dependencies.specializes(that.dependencies) &&
          that.refinement in setOf(null, refinement)

  // Nearest common subtype
  override fun glb(that: PType): PType? =
      pclass
          .glb(that.pclass)
          ?.withExactDependencies(dependencies.intersect(that.dependencies))
          ?.refine(combine(this.refinement, that.refinement))

  // Nearest common supertype
  // Unlike glb, two types always have a least upper bound (if nothing else, Component)
  override fun lub(that: PType): PType {
    val lubClass = this.pclass.lub(that.pclass)
    val deps = this.dependencies.lub(that.dependencies)
    val ref = setOf(this.refinement, that.refinement).singleOrNull()
    return lubClass.withExactDependencies(deps).refine(ref)
  }

  fun specialize(specs: List<Expression>): PType {
    return if (isClassType) {
      if (specs.isEmpty()) {
        loader.componentClass.classType
      } else {
        val spec = specs.single().also { require(it.simple) }
        loader.getClass(spec.className).classType
      }
    } else {
      val deps = loader.match(specs, dependencies).overlayOn(dependencies)
      copy(dependencies = deps.subMap(dependencies.keys))
    }
  }

  private fun combine(one: Requirement?, two: Requirement?): Requirement? {
    val x = setOfNotNull(one, two)
    return when (x.size) {
      0 -> null
      1 -> x.first()
      2 -> And(x.toList())
      else -> error("imposserous")
    }
  }

  fun refine(newRef: Requirement?): PType = copy(refinement = combine(refinement, newRef))

  override val expression: Expression by lazy {
    toExpressionUsingSpecs(narrowedDependencies.expressions)
  }

  override val expressionFull: Expression by lazy {
    toExpressionUsingSpecs(dependencies.expressionsFull)
  }

  internal val narrowedDependencies: DependencyMap by lazy {
    dependencies.minus(pclass.baseType.dependencies)
  }

  private fun toExpressionUsingSpecs(specs: List<Expression>): Expression {
    val expression = pclass.className.addArgs(specs).refine(refinement)
    val roundTrip = loader.resolve(expression)
    require(roundTrip == this) { "$expressionFull -> ${roundTrip.expressionFull}" }
    return expression
  }

  fun supertypes(): List<PType> {
    val supers = pclass.allSuperclasses - loader.componentClass - pclass
    // the argument to wAD is allowed to be a superset
    return supers.map { it.withExactDependencies(dependencies) }
  }

  /**
   * Returns every possible [PType] `t` such that `!t.abstract && t.isSubtypeOf(this)`. Note that
   * this sequence can potentially be very large.
   */
  fun allConcreteSubtypes(): Sequence<PType> {
    if (refinement != null) return emptySequence()
    return concreteSubclasses(pclass).flatMap {
      it.intersectDependencies(this.dependencies).concreteSubtypesSameClass()
    }
  }

  val isClassType: Boolean = pclass.className == CLASS

  /** If [isClassType], return the class it's a class type of. */
  fun getClassForClassType(): PClass = dependencies.getClassForClassType()

  /** Returns the subset of [allConcreteSubtypes] having the exact same [pclass] as ours. */
  fun concreteSubtypesSameClass(): Sequence<PType> {
    return if (refinement != null) { // expression.hasAnyRefinements()) { TODO
      emptySequence()
    } else if (isClassType) { // TODO maybe it should be impossible for a class type to have refins
      concreteSubclasses(getClassForClassType()).map { it.classType }
    } else {
      val axes: List<List<Dependency>> =
          dependencies.realDependencies.map { it.allConcreteSpecializations().toList() }
      val product: List<List<Dependency>> = Lists.cartesianProduct(axes)
      product.asSequence().map { pclass.withExactDependencies(DependencyMap(it)) }
    }
  }

  private fun concreteSubclasses(pclass: PClass): Sequence<PClass> =
      pclass.allSubclasses.asSequence().filter { !it.abstract }

  override fun toString() = expression.toString()
}
