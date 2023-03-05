package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.ClassDeclaration.EffectDeclaration
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.Dependency.Companion.depsForClassType
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.Hierarchical.Companion.glb
import dev.martianzoo.util.toSetStrict

/**
 * A class that has been loaded by a [PClassLoader] based on a [ClassDeclaration]. Each loader has
 * its own separate universe of [PClass]es. While a declaration is just inert data, this type has
 * behavior that is useful to the engine. This loaded class should be the source for most
 * information you need to know about a class at runtime, but the declaration itself can always be
 * retrieved from it when necessary.
 */
public data class PClass
internal constructor(
    /** The class declaration this class was loaded from. */
    public val declaration: ClassDeclaration, // TODO unpublic after simplifying defaults

    /** The class loader that loaded this class. */
    internal val loader: PClassLoader,

    /**
     * This class's superclasses that are exactly one step away; empty only for `Component`.
     */
    public val directSuperclasses: List<PClass> = superclasses(declaration, loader),
) : HasClassName, Hierarchical<PClass> {

  /** The name of this class, in UpperCamelCase. */
  public override val className: ClassName = declaration.className.also { require(it != THIS) }

  /**
   * A short name for this class, such as `"CT"` for `"CityTile"`; is often the same as [className].
   */
  public val shortName: ClassName by declaration::shortName

  // HIERARCHY

  override val abstract: Boolean by declaration::abstract

  override fun isSubtypeOf(that: PClass): Boolean =
      this == that || directSuperclasses.any { it.isSubtypeOf(that) }

  override fun glb(that: PClass): PClass? =
      when {
        this.isSubtypeOf(that) -> this
        that.isSubtypeOf(this) -> that
        else ->
          allSubclasses.singleOrNull {
            it.intersectionType && this in it.directSuperclasses && that in it.directSuperclasses
          }
      }

  override fun lub(that: PClass): PClass { // TODO more deps is better??
    val commonSupers: Set<PClass> = this.allSuperclasses.intersect(that.allSuperclasses)
    val supersOfSupers: Set<PClass> = commonSupers.flatMap { it.properSuperclasses }.toSet()
    val candidates: Set<PClass> = commonSupers - supersOfSupers
    return candidates.maxBy { it.allSuperclasses.size } // most supers tends to be near us
  }

  /**
   * The types listed as this class's supertypes. There is one for each of this class's
   * [directSuperclasses], but as full types rather than just classes; these types might or might
   * not narrow the dependencies (such as `GreeneryTile`, whose supertype is `Tile<MarsArea>` rather
   * than `Tile<Area>`).
   */
  public val directSupertypes: Set<PType> by lazy {
    if (directSuperclasses.none()) {
      require(className == COMPONENT)
      setOf()
    } else {
      declaration.supertypes
          .ifEmpty { setOf(COMPONENT.expr) }
          .map(loader::resolve)
          .toSetStrict()
    }
  }

  /** Every class `c` for which `c.isSuperclassOf(this)` is true, including this class itself. */
  public val allSuperclasses: Set<PClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  public val properSuperclasses: Set<PClass> by lazy { allSuperclasses - this }

  /** Every class `c` for which `c.isSubclassOf(this)` is true, including this class itself. */
  public val allSubclasses: Set<PClass> by lazy {
    loader.allClasses.filter { this in it.allSuperclasses }.toSet()
  }

  public val directSubclasses: Set<PClass> by lazy {
    loader.allClasses.filter { this in it.directSuperclasses }.toSet()
  }

  /**
   * Whether this class serves as the intersection type of its full set of [directSuperclasses];
   * that is, no other [PClass] loaded by this [PClassLoader] is a subclass of all of them unless it
   * is also a subclass of `this`. An example is `OwnedTile`; since components like the `Landlord`
   * award count `OwnedTile` components, it would be a bug if a component like
   * `CommercialDistrictTile` (which is both an `Owned` and a `Tile`) forgot to also extend
   * `OwnedTile`.
   */
  public val intersectionType: Boolean by lazy {
    directSuperclasses.size >= 2 &&
        loader.allClasses
            .filter { pclass -> directSuperclasses.all(pclass::isSubtypeOf) }
            .all(::isSupertypeOf)
  }

  // DEPENDENCIES

  internal val directDependencyKeys: Set<Dependency.Key> by lazy {
    declaration.dependencies.indices.map { Dependency.Key(className, it) }.toSet()
  }

  internal val allDependencyKeys: Set<Dependency.Key> by lazy {
    (directSuperclasses.flatMap { it.allDependencyKeys } + directDependencyKeys).toSet()
  }

  internal val baseDependencies: DependencySet by lazy {
    if (className == CLASS) { // TODO reduce special-casing
      depsForClassType(loader.componentClass)
    } else {
      inheritedDeps().merge(declaredDeps()) { _, _ -> error("") }
    }
  }

  private fun declaredDeps(): DependencySet {
    val list: List<Dependency> =
        directDependencyKeys.map {
          val depExpression = declaration.dependencies[it.index].expression
          TypeDependency(it, loader.resolve(depExpression))
        }
    return DependencySet(list.toSetStrict())
  }

  private fun inheritedDeps(): DependencySet {
    val list: List<DependencySet> = directSupertypes.map { it.dependencies }
    return glb(list) ?: DependencySet(setOf())
  }

  // GETTING TYPES

  internal fun withExactDependencies(deps: DependencySet) =
      PType(this, deps.subMapInOrder(allDependencyKeys))

  /** Least upper bound of all types with pclass==this */
  public val baseType: PType by lazy { withExactDependencies(baseDependencies) }

  internal fun specialize(specs: List<Expression>): PType = baseType.specialize(specs)

  /**
   * Returns the special *class type* for this class; for example, for the class `Resource` returns
   * the type `Class<Resource>`.
   */
  internal val classType: PType by lazy {
    loader.classClass.withExactDependencies(depsForClassType(this))
  }

  fun concreteTypesThisClass(): Sequence<PType> =
      if (abstract) emptySequence() else baseType.concreteSubtypesSameClass()

  // EFFECTS

  /**
   * The effects belonging to this class; similar to those found on the [declaration], but processed
   * as far as we are able to. These effects will belong to every [PType] built from this class,
   * where they will be processed further.
   */
  public val classEffects: List<EffectDeclaration> by lazy {
    val xer = loader.transformer
    val thiss = className.refine(requirement("Ok"))
    declaration.effects
        .map { effect ->
          var fx = effect.effect
          fx = xer.insertDefaults(fx, thiss)
          fx = xer.fixEffectForUnownedContext(fx, this)
          effect.copy(effect = fx)
        }
        .sortedBy { it.effect }
  }

  // OTHER

  // includes abstract
  internal fun hasSingletonTypes(): Boolean =
      declaration.otherInvariants.any { it.requiresThis() } ||
          directSuperclasses.any { it.hasSingletonTypes() }

  /**
   * Returns a set of absolute invariants that must always be true; note that these can contain
   * `This` expressions, which are to be substituted with the concrete type.
   */
  public val invariants: Set<Requirement> by lazy {
    val xer =
        object : PetTransformer() {
          override fun <P : PetNode> transform(node: P): P {
            // for now, add <This> indiscriminately to this type but don't recurse *its* refinement
            // TODO should we be doing this here?
            return if (node is Expression) {
              @Suppress("UNCHECKED_CAST")
              node.addArgs(THIS) as P
            } else {
              transformChildren(node)
            }
          }
        }
    val topInvariants = listOfNotNull(declaration.topInvariant).map { xer.transform(it) }
    declaration.otherInvariants + topInvariants
  }

  override fun toString() = "$className@$loader"

  private companion object {
    fun superclasses(declaration: ClassDeclaration, loader: PClassLoader): List<PClass> {
      return declaration.supertypes
          .classNames()
          .also { require(COMPONENT !in it) }
          .map(loader::load)
          .ifEmpty { listOf(loader.componentClass) }
    }
  }
}
