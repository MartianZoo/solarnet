package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.api.UserException.NarrowingException
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.split
import dev.martianzoo.tfm.pets.ast.Requirement.Counting
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.types.Dependency.Companion.depsForClassType
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.Hierarchical.Companion.glb
import dev.martianzoo.util.toSetStrict
import kotlin.Int.Companion.MAX_VALUE
import kotlin.math.max
import kotlin.math.min

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
public data class MClass
internal constructor(

    /** The class declaration this class was loaded from. */
    internal val declaration: ClassDeclaration,

    /** The class loader that loaded this class. */
    internal val loader: MClassLoader,

    /** This class's superclasses that are exactly one step away; empty only for `Component`. */
    public val directSuperclasses: List<MClass> = superclasses(declaration, loader),
) : HasClassName, Hierarchical<MClass> {
  public val table: MClassTable by ::loader

  /** The name of this class, in UpperCamelCase. */
  public override val className: ClassName = declaration.className.also { require(it != THIS) }

  /**
   * A short name for this class, such as `"CT"` for `"CityTile"`; is often the same as [className].
   */
  public val shortName: ClassName by declaration::shortName

  // HIERARCHY

  override val abstract: Boolean by declaration::abstract

  override fun isSubtypeOf(that: MClass): Boolean =
      this == that || directSuperclasses.any { it.isSubtypeOf(that) }

  override fun glb(that: MClass): MClass? =
      when {
        this.isSubtypeOf(that) -> this
        that.isSubtypeOf(this) -> that
        else -> {
          allSubclasses.singleOrNull {
            it.intersectionType && this in it.directSuperclasses && that in it.directSuperclasses
          }
        }
      }

  override fun lub(that: MClass): MClass {
    val commonSupers: Set<MClass> = this.allSuperclasses.intersect(that.allSuperclasses)
    val supersOfSupers: Set<MClass> = commonSupers.flatMap { it.properSuperclasses }.toSet()
    val candidates: Set<MClass> = commonSupers - supersOfSupers
    // This is a weird and stupid heuristic, but does it really matter which one we pick?
    return candidates.maxBy {
      it.dependencies.typeDependencies.size * 100 + it.allSuperclasses.size
    }
  }

  override fun ensureNarrows(that: MClass, info: TypeInfo) {
    if (!isSubtypeOf(that))
        throw NarrowingException("${this.className} is not a subclass of ${that.className}")
  }

  /**
   * The types listed as this class's supertypes. There is one for each of this class's
   * [directSuperclasses], but as full types rather than just classes; these types might or might
   * not narrow the dependencies (such as `GreeneryTile`, whose supertype is `Tile<MarsArea>` rather
   * than `Tile<Area>`).
   */
  public val directSupertypes: Set<MType> by lazy {
    when {
      className == COMPONENT -> setOf()
      declaration.supertypes.none() -> setOf(table.componentClass.baseType)
      else ->
          declaration.supertypes.toSetStrict {
            val dethissed = replaceThisExpressionsWith(className.expression).transform(it)
            table.resolve(dethissed)
          }
    }
  }

  /** Every class `c` for which `c.isSuperclassOf(this)` is true, including this class itself. */
  public val allSuperclasses: Set<MClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  public val properSuperclasses: Set<MClass> by lazy { allSuperclasses - this }

  /** Every class `c` for which `c.isSubclassOf(this)` is true, including this class itself. */
  public val allSubclasses: Set<MClass> by lazy {
    table.allClasses.filter { this in it.allSuperclasses }.toSet()
  }

  public val directSubclasses: Set<MClass> by lazy {
    table.allClasses.filter { this in it.directSuperclasses }.toSet()
  }

  /**
   * Whether this class serves as the intersection type of its full set of [directSuperclasses];
   * that is, no other [MClass] in this [MClassTable] is a subclass of all of them unless it is also
   * a subclass of `this`. An example is `OwnedTile`; since components like the `Landlord` award
   * count `OwnedTile` components, it would be a bug if a component like `CommercialDistrictTile`
   * (which is both an `Owned` and a `Tile`) forgot to also extend `OwnedTile`.
   */
  public val intersectionType: Boolean by lazy {
    directSuperclasses.size >= 2 &&
        table.allClasses
            .filter { mclass -> directSuperclasses.all(mclass::isSubtypeOf) }
            .all(::isSupertypeOf)
  }

  // DEPENDENCIES

  internal val dependencies: DependencySet by lazy {
    if (className == CLASS) { // TODO reduce special-casing
      depsForClassType(table.componentClass)
    } else {
      inheritedDeps.merge(declaredDeps) { _, _ -> throw AssertionError() }
    }
  }

  internal val declaredDeps: DependencySet by lazy {
    DependencySet.of(declaration.dependencies.mapIndexed(::createDep))
  }

  private fun createDep(i: Int, dep: Expression) =
      TypeDependency(Key(className, i), table.resolve(dep))

  private val inheritedDeps: DependencySet by lazy {
    val list: List<DependencySet> =
        directSupertypes.map { supertype ->
          val replacer = replacer(supertype.className, className)
          supertype.dependencies.map { mtype ->
            val depExpr = mtype.expressionFull
            val newArgs = depExpr.arguments.map(replacer::transform)
            table.resolve(depExpr.replaceArguments(newArgs))
          }
        }
    glb(list) ?: DependencySet.of()
  }

  // GETTING TYPES

  internal fun withAllDependencies(deps: DependencySet) =
      MType(this, deps.subMapInOrder(dependencies.keys))

  /** Least upper bound of all types with mclass==this */
  public val baseType: MType by lazy { withAllDependencies(dependencies) }

  internal val defaultType: MType by lazy {
    loader.resolve(loader.transformers.insertDefaults().transform(className.expression))
  }

  internal fun specialize(specs: List<Expression>): MType = baseType.specialize(specs)

  /**
   * Returns the special *class type* for this class; for example, for the class `Resource` returns
   * the type `Class<Resource>`.
   */
  internal val classType: MType by lazy {
    table.classClass.withAllDependencies(depsForClassType(this))
  }

  // EFFECTS

  public fun rawEffects(): Set<Effect> = declaration.effects

  /**
   * The effects belonging to this class; similar to those found on the [declaration], but processed
   * as far as we are able to. These effects will belong to every [MType] built from this class,
   * where they will be processed further.
   */
  public val classEffects: Set<Effect> by lazy {
    allSuperclasses.flatMap { it.directClassEffects() }.toSetStrict()
  }

  private fun directClassEffects(): List<Effect> {
    val transformer =
        if (OWNED !in allSuperclasses.classNames()) {
          chain(
              attachToClassTransformer,
              table.transformers.fixEffectForUnownedContext(),
          )
        } else {
          attachToClassTransformer
        }

    return declaration.effects.map { transformer.transform(it) }
  }

  private val attachToClassTransformer: PetTransformer by lazy {
    val weirdExpression = className.has(Min(scaledEx(1, OK)))
    val xers = table.transformers
    chain(xers.insertDefaults(weirdExpression), xers.atomizer())
  }

  // OTHER

  private val specificThenGeneralInvars: Pair<List<Requirement>, List<Requirement>> by lazy {
    val requirements = declaration.invariants.map(attachToClassTransformer::transform)
    val deprodify = table.transformers.deprodify()
    split(requirements).map(deprodify::transform).partition { THIS in it }
  }

  private val specificInvars: Set<Requirement> by lazy {
    specificThenGeneralInvars.first.toSetStrict()
  }

  public val generalInvars: Set<Requirement> by lazy {
    specificThenGeneralInvars.second.toSetStrict()
  }

  /**
   * Returns a set of absolute invariants that must always be true; note that these contain `This`
   * expressions, which are to be substituted with the concrete type.
   */
  public val typeInvariants: Set<Requirement> by lazy {
    allSuperclasses.flatMap { it.specificInvars }.toSet()
  }

  public val componentCountRange: IntRange by lazy {
    val ranges: List<IntRange> =
        typeInvariants
            .filterIsInstance<Counting>()
            .filter { it.scaledEx.expression == THIS.expression }
            .map { it.range }
    (ranges + listOf(0..MAX_VALUE)).reduce { a, b -> max(a.first, b.first)..min(a.last, b.last) }
  }

  override fun toString() = "$className@$loader"

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
