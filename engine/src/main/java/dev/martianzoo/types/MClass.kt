package dev.martianzoo.types

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.OK
import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.engine.Transformers
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Companion.split
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.types.Dependency.Companion.depsForClassType
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.Hierarchical.Companion.glb
import dev.martianzoo.util.toSetStrict

/**
 * A class that has been loaded by a [MClassLoader] based on a [ClassDeclaration]. Each loader has
 * its own separate universe of [MClass]es. While a declaration is just inert data, this type has
 * behavior that is useful to the engine. This loaded class should be the source for most
 * information you need to know about a class at runtime, but the declaration itself can always be
 * retrieved from it when necessary.
 *
 * (Think of it as being called `Class`; it has a prefix just to distinguish it from
 * `java.lang.Class`. The name `MClass` comes from the fact that the object of the game is to turn
 * Mars into a "class M planet".)
 */
public class MClass
internal constructor(
    /** The class declaration this class was loaded from. */
    declaration: ClassDeclaration,

    /** The class loader that loaded this class. */
    internal val loader: MClassLoader,

    /** This class's superclasses that are exactly one step away; empty only for `Component`. */
    internal val directSuperclasses: List<MClass> = superclasses(declaration, loader),
    internal val custom: CustomClass? = null, // TODO move?
) : HasClassName, Hierarchical<MClass> {

  /** The name of this class, in UpperCamelCase. */
  override val className: ClassName = declaration.className.also { require(it != THIS) }

  /**
   * A short name for this class, such as `"CT"` for `"CityTile"`; is often the same as [className].
   */
  public val shortName: ClassName by declaration::shortName

  val docstring: String? by declaration::docstring

  init {
    require((declaration.custom) == (custom != null)) { declaration }
  }

  // HIERARCHY

  override val abstract: Boolean by declaration::abstract

  override fun isSubtypeOf(that: MClass): Boolean = that in getAllSuperclasses()

  override fun glb(that: MClass): MClass? =
      when {
        this.isSubtypeOf(that) -> this
        that.isSubtypeOf(this) -> that
        else -> {
          allSubclasses.singleOrNull {
            it.isIntersectionType() &&
                this in it.directSuperclasses &&
                that in it.directSuperclasses
          }
        }
      }

  override fun lub(that: MClass): MClass {
    val commonSupers: Set<MClass> = this.allSuperclasses.intersect(that.allSuperclasses)
    val supersOfSupers: Set<MClass> = commonSupers.flatMap { it.getProperSuperclasses() }.toSet()
    val candidates: Set<MClass> = commonSupers - supersOfSupers
    // This is a weird and stupid heuristic, but does it really matter which one we pick?
    return candidates.maxBy {
      it.dependencies.typeDependencies().size * 100 + it.allSuperclasses.size
    }
  }

  override fun ensureNarrows(that: MClass, info: TypeInfo) {
    if (!isSubtypeOf(that))
        throw NarrowingException("${this.className} is not a subclass of ${that.className}")
  }

  private val sups by declaration::supertypes

  private fun directSupertypes(): Set<MType> =
      when {
        className == COMPONENT -> setOf()
        sups.none() -> setOf(loader.componentClass.baseType)
        else ->
            sups.toSetStrict {
              val dethissed = replaceThisExpressionsWith(className.expression).transform(it)
              loader.resolve(dethissed)
            }
      }

  private val allSuperclasses: Set<MClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  /** Every class `c` for which `c.isSuperclassOf(this)` is true, including this class itself. */
  internal fun getAllSuperclasses(): Set<MClass> = allSuperclasses

  internal fun getProperSuperclasses(): Set<MClass> = getAllSuperclasses() - this

  private val allSubclasses: Set<MClass> by lazy {
    loader.allClasses().filter { this in it.getAllSuperclasses() }.toSet()
  }

  /** Every class `c` for which `c.isSubclassOf(this)` is true, including this class itself. */
  internal fun getAllSubclasses(): Set<MClass> = allSubclasses

  internal fun getDirectSubclasses(): Set<MClass> =
      loader.allClasses().filter { this in it.directSuperclasses }.toSet()

  /**
   * Whether this class serves as the intersection type of its full set of [directSuperclasses];
   * that is, no other [MClass] in this [MClassTable] is a subclass of all of them unless it is also
   * a subclass of `this`. An example is `OwnedTile`; since components like the `Landlord` award
   * count `OwnedTile` components, it would be a bug if a component like `CommercialDistrictTile`
   * (which is both an `Owned` and a `Tile`) forgot to also extend `OwnedTile`.
   */
  internal fun isIntersectionType(): Boolean = intersectionType

  private val intersectionType: Boolean by lazy {
    directSuperclasses.size >= 2 &&
        loader
            .allClasses()
            .filter { mclass -> directSuperclasses.all(mclass::isSubtypeOf) }
            .all(::isSupertypeOf)
  }

  // DEPENDENCIES

  private val inheritedDeps: DependencySet by lazy {
    val list: List<DependencySet> =
        directSupertypes().map { supertype ->
          val replacer = replacer(supertype.className, className)
          supertype.dependencies.map { mtype ->
            val depExpr = mtype.expressionFull
            val newArgs = depExpr.arguments.map(replacer::transform)
            loader.resolve(depExpr.replaceArguments(newArgs))
          }
        }
    glb(list) ?: DependencySet.of()
  }

  // property because we don't retain `declaration`
  private val declaredDeps by lazy {
    DependencySet.of(
        declaration.dependencies.mapIndexed { i, dep ->
          TypeDependency(Key(className, i), loader.resolve(dep))
        })
  }

  // `by lazy` enables dependency cycles, yay
  internal val dependencies: DependencySet by lazy {
    if (className == CLASS) {
      depsForClassType(loader.componentClass)
    } else {
      inheritedDeps.merge(declaredDeps) { _, _ -> error("unexpected") }
    }
  }

  // GETTING TYPES

  internal fun withAllDependencies(deps: DependencySet) =
      MType(this, deps.subMapInOrder(dependencies.keys))

  /** Least upper bound of all types with mclass==this */
  internal val baseType: MType by lazy { withAllDependencies(dependencies) }

  internal val defaultType: MType by lazy {
    loader.resolve(Transformers(loader).insertDefaults().transform(className.expression))
  }

  internal fun specialize(specs: List<Expression>): MType = baseType.specialize(specs)

  /**
   * Returns the special *class type* for this class; for example, for the class `Resource` returns
   * the type `Class<Resource>`.
   */
  internal val classType: MType by lazy {
    loader.classClass.withAllDependencies(depsForClassType(this))
  }

  fun concreteTypes(): Sequence<MType> = baseType.concreteSubtypesSameClass()

  internal val defaultsDecl by declaration::defaultsDeclaration

  internal val defaults: Defaults by lazy { Defaults.forClass(this) }

  // EFFECTS

  internal val rawEffects: Set<Effect> by declaration::effects

  /**
   * The effects belonging to this class; similar to those found on the [declaration], but processed
   * as far as we are able to. These effects will belong to every [MType] built from this class,
   * where they will be processed further.
   */
  internal val classEffects: Set<Effect> by lazy {
    getAllSuperclasses().flatMap { it.directClassEffects() }.toSetStrict()
  }

  private fun directClassEffects(): List<Effect> {
    val transformer =
        if (OWNED !in getAllSuperclasses().classNames()) {
          chain(
              attachToClassTransformer,
              loader.transformers.fixEffectForUnownedContext(),
          )
        } else {
          attachToClassTransformer
        }

    return rawEffects.map { transformer.transform(it) }
  }

  private val attachToClassTransformer: PetTransformer by lazy {
    val weirdExpression = className.has(Min(scaledEx(1, OK)))
    val xers = loader.transformers
    chain(xers.insertDefaults(weirdExpression), xers.atomizer())
  }

  // OTHER

  private val directInvariants = split(declaration.invariants)

  fun invariants(): Set<Requirement> {
    return if (abstract) {
      setOf()
    } else {
      getAllSuperclasses().flatMap { it.directInvariants }.toSet()
    }
  }

  fun isSingletonType(): Boolean =
      invariants().any {
        (it as Counting).range.first == 1 && it.scaledEx.expression == THIS.expression
      }

  override fun equals(other: Any?) =
      other is MClass && other.className == className && other.loader == loader

  override fun hashCode() = className.hashCode() xor loader.hashCode()

  override fun toString() = "$className"

  private companion object {
    fun superclasses(declaration: ClassDeclaration, loader: MClassLoader): List<MClass> {
      return declaration.supertypes
          .classNames()
          .also { require(COMPONENT !in it) }
          .ifEmpty { listOf(COMPONENT) }
          .map(loader::loadAndMaybeEnqueueRelated)
    }
  }
}
