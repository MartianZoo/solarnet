package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Type
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.engine.Component
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
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
    internal val root: MClass,
    internal val dependencies: DependencySet,
    override val refinement: Requirement? = null,
) : Type, Hierarchical<MType>, Reifiable<MType>, HasClassName by root {
  internal val loader by root::loader
  internal val typeDependencies = dependencies.typeDependencies()

  init {
    require(dependencies.keys.toList() == root.dependencies.keys.toList()) {
      "expected keys ${root.dependencies.keys}, got $dependencies"
    }
    if (refinement != null) loader.checkAllTypes(refinement)
  }

  override val abstract = root.abstract || dependencies.abstract || refinement != null

  override fun isSubtypeOf(that: MType) = narrows(that)

  // Nearest common subtype
  // TODO allocating 28 MB per solo game
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
    toExpressionUsingSpecs(narrowedDependencies.expressions())
  }

  override val expressionFull: Expression by lazy {
    toExpressionUsingSpecs(dependencies.expressionsFull())
  }

  internal val narrowedDependencies: DependencySet by lazy { dependencies.minus(root.dependencies) }

  private fun toExpressionUsingSpecs(specs: List<Expression>): Expression {
    val expression = root.className.of(specs).has(refinement)
    val roundTrip = loader.resolve(expression)
    require(roundTrip == this) { "$expression" }
    return expression
  }

  /**
   * Returns every possible [MType] `t` such that `!t.abstract && t.isSubtypeOf(this)`. Note that
   * this sequence can potentially be very large.
   */
  internal fun allConcreteSubtypes(): Sequence<MType> {
    return concreteSubclasses(root).flatMap {
      val deps: DependencySet? = dependencies glb it.baseType.dependencies
      if (deps == null) {
        emptySequence()
      } else {
        it.withAllDependencies(deps).concreteSubtypesSameClass()
      }
    }
  }

  internal fun singleConcreteSubtype(): MType? {
    val mclass = concreteSubclasses(root).singleOrNull() ?: return null
    val abstractType = this glb mclass.baseType
    val deps = abstractType!!.dependencies.singleConcreteSubtype() ?: return null
    return abstractType.root.withAllDependencies(deps)
  }

  /** Returns the subset of [allConcreteSubtypes] having the exact same [root] as ours. */
  // used publicly only by `desc random`
  public fun concreteSubtypesSameClass(): Sequence<MType> =
      if (root.abstract) emptySequence() else dependencies.concreteSubtypesSameClass(this)

  internal fun concreteSubclasses(mclass: MClass) =
      mclass.getAllSubclasses().asSequence().filter { !it.abstract }

  override fun ensureNarrows(that: MType, info: TypeInfo) {
    root.ensureNarrows(that.root, info)
    dependencies.ensureNarrows(that.dependencies, info)

    val refin = that.refinement
    if (refin != null) {
      val requirement = refinementMangler(expressionFull).transform(refin)
      if (!info.has(requirement)) {
        throw Exceptions.refinementNotMet(requirement)
      }
    }
  }

  // TODO solo game spending 19% of its time in this method, allocating over 10 MB!?
  override fun narrows(that: Type, info: TypeInfo): Boolean {
    that as? MType ?: error("")
    if (!root.isSubtypeOf(that.root)) return false
    if (!dependencies.narrows(that.dependencies, info)) return false

    val refin = that.refinement ?: return true
    val requirement = refinementMangler(expressionFull).transform(refin)
    return info.has(requirement)
  }

  // We check if MartianIndustries reifies CardFront(HAS 1 BuildingTag)
  // by testing the requirement `1 BuildingTag<MartianIndustries>`
  private fun refinementMangler(proposed: Expression): PetTransformer {
    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        return if (node is Expression) {
          val modded = root.loader.resolve(node).specialize(listOf(proposed))
          @Suppress("UNCHECKED_CAST")
          modded.expressionFull as P
        } else {
          transformChildren(node)
        }
      }
    }
  }

  private val asComponent: Component? by lazy { if (abstract) null else Component(this) }

  fun toComponent(): Component {
    require(!abstract) { "type is abstract: $expressionFull" }
    return asComponent!!
  }

  override fun toString() = "$expression"
}
