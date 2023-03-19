package dev.martianzoo.tfm.types

import com.google.common.collect.Lists.cartesianProduct
import dev.martianzoo.tfm.api.Exceptions.InvalidReificationException
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.types.Transformers.UseShortNames
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
  internal val loader by mclass::loader

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
        val spec = specs.single().also { require(it.simple) } // TODO check if exposed
        loader.getClass(spec.className).classType
      }
    } else {
      copy(dependencies = loader.matchFull(specs, dependencies))
    }
  }

  private fun conjoin(one: Requirement?, two: Requirement?): Requirement? {
    // TODO move to Requirement like we did for Instruction.Multi
    val x = setOfNotNull(one, two)
    return when (x.size) {
      0 -> null
      1 -> x.first()
      else -> And(x.toList())
    }
  }

  public fun refine(newRef: Requirement?): MType = copy(refinement = conjoin(refinement, newRef))

  override val expression: Expression by lazy {
    toExpressionUsingSpecs(narrowedDependencies.expressions)
  }

  override val expressionFull: Expression by lazy {
    toExpressionUsingSpecs(dependencies.expressionsFull)
  }

  val expressionShort: Expression by lazy { UseShortNames(loader).transform(expression) }

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

  fun checkReifies(proposed: MType) {
    if (proposed.abstract) throw InvalidReificationException("Reification must be concrete")
    if (proposed == this) return // else, anything concrete can reify to itself

    if (!this.abstract) throw InvalidReificationException("Already concrete, can't be reified")

    if (!proposed.isSubtypeOf(this)) {
      throw InvalidReificationException("${proposed.expression} is not a subtype of ${expression}")
    }

    val myPropConcSubs = allConcreteSubtypes().filterNot { it == proposed }
    // this gon be slowasfuck
    for (propConcSub in myPropConcSubs) {
      if (proposed.isSubtypeOf(propConcSub)) {
        throw InvalidReificationException(
            "A more general type such as ${propConcSub.expression}" +
                " already reifies $expression; can't narrow even further")
      }
    }
  }

  override fun toString() = "$expressionFull@${mclass.loader}"

  /** Decides what substitutions should be made to class effects to yield component effects. */
  fun findSubstitutions(linkages: Set<ClassName>): Map<ClassName, Expression> {
    val general: Expression = mclass.baseType.expressionFull
    val specific: Expression = expressionFull

    val map = mutableMapOf<ClassName, Expression>()
    doIt(linkages, map, general, specific)
    return map
  }

  private fun doIt(
      linkages: Set<ClassName>,
      map: MutableMap<ClassName, Expression>,
      general: Expression,
      specific: Expression
  ) {
    for ((g, s) in general.arguments.zip(specific.arguments)) {
      if (g.simple && g.className in linkages && s != g) {
        map[g.className] = s
      } else {
        doIt(linkages, map, g, s)
      }
    }
  }
}
