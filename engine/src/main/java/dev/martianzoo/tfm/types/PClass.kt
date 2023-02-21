package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.END
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNED
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.SpecialClassNames.USE_ACTION
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenRemove
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.Dependency.ClassDependency
import dev.martianzoo.tfm.types.Dependency.ClassDependency.Companion.KEY
import dev.martianzoo.tfm.types.Dependency.TypeDependency
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
    /** The declaration this class was loaded from. */
    public val declaration: ClassDeclaration,
    /** The class loader that loaded this class. */
    internal val loader: PClassLoader,
    public val directSuperclasses: List<PClass> = superclasses(declaration, loader),
) : HasClassName {
  /** The name of this class, in UpperCamelCase. */
  public override val className: ClassName by declaration::name

  /**
   * A short name for this class, such as `"CT"` for `"CityTile"`; is often the same as [className].
   */
  public val id: ClassName by declaration::id

  /**
   * If true, all types with this as their root are abstract, even when all dependencies are
   * concrete. An example is `Tile`; one can never add a tile to the board without first deciding
   * *which kind* of tile to add.
   */
  public val abstract: Boolean by declaration::abstract

  // HIERARCHY

  /**
   * Returns `true` if this class is a subclass of [that], whether direct, indirect, or the same
   * class. Equivalent to `that.isSuperclassOf(this)`.
   */
  public fun isSubclassOf(that: PClass): Boolean =
      this == that || directSuperclasses.any { it.isSubclassOf(that) }

  /**
   * Returns `true` if this class is a superclass of [that], whether direct, indirect, or the same
   * class. Equivalent to `that.isSubclassOf(this)`.
   */
  public fun isSuperclassOf(that: PClass) = that.isSubclassOf(this)

  /** Every class `c` for which `c.isSuperclassOf(this)` is true, including this class itself. */
  public val allSuperclasses: Set<PClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  /** Every class `c` for which `this in c.directSuperclasses` */
  public val directSubclasses: Set<PClass> by lazy {
    loader.allClasses.filter { this in it.directSuperclasses }.toSet()
  }

  /** Every class `c` for which `c.isSubclassOf(this)` is true, including this class itself. */
  public val allSubclasses: Set<PClass> by lazy {
    loader.allClasses.filter { this in it.allSuperclasses }.toSet()
  }

  /**
   * The types listed as this class's supertypes. There is one for each of this class's
   * [directSuperclasses], but as full types rather than just classes; these types might or might
   * not narrow the dependencies (such as `GreeneryTile`, whose supertype is `Tile<MarsArea>` rather
   * than `Tile<Area>`).
   */
  public val directSupertypes: Set<PType> by lazy {
    declaration.supertypes.map(loader::resolveType).toSetStrict()
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
            .filter { pclass -> directSuperclasses.all(pclass::isSubclassOf) }
            .all(::isSuperclassOf)
  }

  /** Returns the greatest lower bound of `this` and [that], or null if there is no such class. */
  // TODO explain better
  public fun intersect(that: PClass): PClass? =
      when {
        this.isSubclassOf(that) -> this
        that.isSubclassOf(this) -> that
        else ->
          allSubclasses.singleOrNull {
            it.intersectionType &&
                this in it.directSuperclasses &&
                that in it.directSuperclasses
          }
      }

  // DEPENDENCIES

  internal val directDependencyKeys: Set<Dependency.Key> by lazy {
    declaration.dependencies.indices.map { Dependency.Key(className, it) }.toSet()
  }

  internal val allDependencyKeys: Set<Dependency.Key> by lazy {
    (directSuperclasses.flatMap { it.allDependencyKeys } + directDependencyKeys).toSet()
  }

  /**
   * Returns the special *class type* for this class; for example, for the class `Resource` returns
   * the type `Class<Resource>`.
   */
  public val classType: PType by lazy {
    PType.create(loader.classClass, DependencyMap(KEY to ClassDependency(this)))
  }

  /** Least upper bound of all types with pclass==this */
  public val baseType: PType by lazy {
    if (className == CLASS) {
      // base type of Class is Class<Component>
      loader.componentClass.classType
    } else {
      val newDeps =
          directDependencyKeys.associateWith {
            val depTypeExpr = declaration.dependencies[it.index].typeExpr
            TypeDependency(it, loader.resolveType(depTypeExpr))
          }
      val deps = DependencyMap.intersect(directSupertypes.map { it.allDependencies })
      val allDeps = deps.intersect(DependencyMap(newDeps))
      PType.create(this, allDeps, null)
    }
  }

  fun specialize(map: List<PType>): PType =
      baseType.copy(allDependencies = baseType.allDependencies.specialize(map))

  // DEFAULTS

  internal val defaults: Defaults by lazy {
    if (className == COMPONENT) {
      Defaults.from(declaration.defaultsDeclaration, this, loader)
    } else {
      val rootDefaults = loader.componentClass.defaults
      defaultsIgnoringRoot().overlayOn(listOf(rootDefaults))
    }
  }

  // TODO hack
  private fun defaultsIgnoringRoot(): Defaults =
      if (className == COMPONENT) {
        Defaults()
      } else {
        Defaults.from(declaration.defaultsDeclaration, this, loader)
            .overlayOn(directSuperclasses.map { it.defaultsIgnoringRoot() })
      }

  // EFFECTS

  /**
   * The effects belonging to this class; similar to those found on the [declaration], but processed
   * as far as we are able to. These effects will belong to every [PType] built from this class,
   * where they will be processed further.
   */
  val classEffects: List<Effect> by lazy {
    val xer = loader.transformer
    declaration.effectsRaw
        .map { effect ->
          var fx = effect
          fx = xer.deprodify(fx)
          fx = xer.applyDefaultsIn(fx)
          if (allSuperclasses.any { it.className == OWNED }) {
            fx = xer.addOwner(fx)
          }
          // fx = xer.simplifyTypes(fx, baseType)
          fx
        }
        .sortedWith(effectComparator)
  }

  private val effectComparator: Comparator<Effect> =
      compareBy(
          {
            val t = it.trigger
            when {
              t == WhenGain -> if (it.automatic) -1 else 0
              t == WhenRemove -> if (it.automatic) 1 else 2
              t is OnGainOf && "${t.typeExpr.className}".startsWith("$USE_ACTION") -> 4
              t == OnGainOf.create(END.type) -> 5
              else -> 3
            }
          },
          { it.trigger.toString() })

  // OTHER

  // internal fun doForEachConcreteType(thing: (PType) -> Unit): Unit {
  //   if (abstract) {
  //     directSubclasses.forEach { it.doForEachConcreteType(thing) }
  //   } else {
  //     baseType.doForEachConcreteTypeThisClass(thing)
  //   }
  // }

  // includes abstract
  internal fun isSingleton(): Boolean =
      declaration.otherInvariants.any { it.requiresThis() } ||
          directSuperclasses.any { it.isSingleton() }

  internal fun findMatchups(specs: List<PType>) = baseType.allDependencies.findMatchups(specs)

  /**
   * Returns a set of absolute invariants that must always be true; note that these can contain
   * references to the type `This`, which are to be substituted with the concrete type.
   */
  public val invariants: Set<Requirement> by lazy {
    val xer =
        object : PetTransformer() {
          override fun <P : PetNode> transform(node: P): P {
            // for now, add <This> indiscriminately to this type but don't recurse *its* refinement
            // TODO should we be doing this here?
            return if (node is TypeExpr) {
              @Suppress("UNCHECKED_CAST")
              node.addArgs(THIS) as P
            } else {
              defaultTransform(node)
            }
          }
        }
    val topInvariants = listOfNotNull(declaration.topInvariant).map { xer.transform(it) }
    declaration.otherInvariants + topInvariants
  }

  override fun toString() = "$className"

  companion object {
    fun superclasses(declaration: ClassDeclaration, loader: PClassLoader) =
        declaration.supertypes.classNames().map(loader::load)
  }
}
