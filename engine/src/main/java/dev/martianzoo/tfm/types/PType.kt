package dev.martianzoo.tfm.types

import com.google.common.collect.Lists
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.types.Dependency.ClassDependency
import dev.martianzoo.tfm.types.Dependency.TypeDependency

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
) : Type {
  init {
    require(dependencies.keys.toList() == pclass.allDependencyKeys.toList()) {
      "expected keys ${pclass.allDependencyKeys}, got $dependencies"
    }
    if (pclass.className == CLASS) {
      require(dependencies.dependencies.single() is ClassDependency)
    } else {
      require(dependencies.dependencies.all { it is TypeDependency })
    }
    if (refinement != null) pclass.loader.checkAllTypes(refinement)
  }

  override val abstract = pclass.abstract || dependencies.abstract || refinement != null

  override fun isSubtypeOf(that: Type) =
      pclass.isSubclassOf((that as PType).pclass) &&
          dependencies.specializes(that.dependencies) &&
          that.refinement in setOf(null, refinement)

  // Nearest common subtype
  fun intersect(that: PType): PType? =
      pclass
          .intersect(that.pclass)
          ?.withExactDependencies(dependencies.intersect(that.dependencies))
          ?.refine(combine(this.refinement, that.refinement))

  // Nearest common supertype
  // Unlike glb, two types always have a least upper bound (if nothing else, Component)
  fun lub(that: PType): PType {
    val lubClass = this.pclass.lub(that.pclass)
    val deps = this.dependencies.lub(that.dependencies)
    val ref = setOf(this.refinement, that.refinement).singleOrNull()
    return lubClass.withExactDependencies(deps).refine(ref)
  }

  fun specialize(specs: List<Expression>): PType {
    val deps = dependencies.specialize(specs, pclass.loader)
    return copy(dependencies = deps.subMap(dependencies.keys))
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
    toExpressionUsingSpecs(narrowedDependencies.dependencies.map { it.expression })
  }

  override val expressionFull: Expression by lazy {
    toExpressionUsingSpecs(dependencies.dependencies.map { it.expressionFull })
  }

  internal val narrowedDependencies: DependencyMap by lazy {
    dependencies - pclass.baseType.dependencies
  }

  private fun toExpressionUsingSpecs(specs: List<Expression>): Expression {
    val expression = pclass.className.addArgs(specs).refine(refinement)
    val roundTrip = pclass.loader.resolve(expression)
    require(roundTrip == this) { "$expressionFull -> ${roundTrip.expressionFull}" }
    return expression
  }

  fun supertypes(): List<PType> {
    val supers = pclass.allSuperclasses - pclass.loader.componentClass - pclass
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

  /** Returns the subset of [allConcreteSubtypes] having the exact same [pclass] as ours. */
  fun concreteSubtypesSameClass(): Sequence<PType> {
    if (refinement != null) return emptySequence()
    if (pclass.className == CLASS) {
      val classDep = dependencies.dependencies.single() as ClassDependency
      return concreteSubclasses(classDep.bound).map { it.classType }
    }

    val axes: List<List<Dependency>> =
        dependencies.dependencies.map { dep ->
          (dep as TypeDependency).allConcreteSpecializations().toList()
        }
    val product: List<List<Dependency>> = Lists.cartesianProduct(axes)
    return product.asSequence().map { pclass.withExactDependencies(DependencyMap(it)) }
  }

  private fun concreteSubclasses(pclass: PClass): Sequence<PClass> =
      pclass.allSubclasses.asSequence().filter { !it.abstract }

  override fun toString() = expression.toString()
}
