package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.END
import dev.martianzoo.tfm.pets.SpecialClassNames.USE_ACTION
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenRemove
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.AstTransforms.addOwnerToOwned
import dev.martianzoo.tfm.types.AstTransforms.applyGainDefaultsIn
import dev.martianzoo.tfm.types.AstTransforms.deprodify
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
    private val loader: PClassLoader,
    public val directSuperclasses: List<PClass> = declaration.superclassNames.map(loader::load),
) {
  /** The name of this class, in UpperCamelCase. */
  public val name: ClassName by declaration::name

  /** A short name for this class, such as `"CT"` for `"CityTile"`; is often the same as [name]. */
  public val id: ClassName by declaration::id

  /**
   * If true, all types with this as their root are abstract, even when all dependencies are
   * concrete. An example is `Tile`; one can never add a tile to the board without first deciding
   * *which kind* of tile to add.
   */
  public val abstract: Boolean by declaration::abstract

  // HIERARCHY

  /**
   * Returns [true] if this class is a subclass of [that], whether direct, indirect, or the same
   * class. Equivalent to `that.isSuperclassOf(this)`.
   */
  public fun isSubclassOf(that: PClass): Boolean =
      this == that || directSuperclasses.any { it.isSubclassOf(that) }

  /**
   * Returns [true] if this class is a superclass of [that], whether direct, indirect, or the same
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
   * is also a subclass of [this]. An example is `OwnedTile`; since components like the `Landlord`
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

  /** Returns the greatest lower bound of [this] and [that], or null if there is no such class. */
  // TODO explain better
  public infix fun intersect(that: PClass): PClass? =
      when {
        this.isSubclassOf(that) -> this
        that.isSubclassOf(this) -> that
        else ->
            allSubclasses
                .filter {
                  it.intersectionType &&
                      this in it.directSuperclasses &&
                      that in it.directSuperclasses
                }
                .singleOrNull()
      }

  // DEPENDENCIES

  internal val directDependencyKeys: Set<Dependency.Key> by lazy {
    declaration.dependencies.indices.map { Dependency.Key(name, it) }.toSet()
  }

  internal val allDependencyKeys: Set<Dependency.Key> by lazy {
    (directSuperclasses.flatMap { it.allDependencyKeys } + directDependencyKeys).toSet()
  }

  /**
   * Returns the special *class type* for this class; for example, for the class `Resource` returns
   * the type `Class<Resource>`.
   */
  public val classType by lazy {
    PType(loader.classClass, DependencyMap(KEY to ClassDependency(this)))
  }

  /** Least upper bound of all types with pclass==this */
  public val baseType: PType by lazy {
    if (name == CLASS) {
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
      require(allDeps.keys == allDependencyKeys)
      PType(this, allDeps, null)
    }
  }

  fun toTypeExprFull() = baseType.toTypeExprFull()

  fun specialize(map: List<PType>): PType = baseType.specialize(map)

  // DEFAULTS

  internal val defaults: Defaults by lazy {
    if (name == COMPONENT) {
      Defaults.from(declaration.defaultsDeclaration, this, loader)
    } else {
      val rootDefaults = loader.componentClass.defaults
      defaultsIgnoringRoot().overlayOn(listOf(rootDefaults))
    }
  }

  private fun defaultsIgnoringRoot(): Defaults = // TODO hack
  if (name == COMPONENT) {
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
    declaration.effectsRaw.map {
      var fx = it
      fx = deprodify(fx, loader)
      fx = applyGainDefaultsIn(fx, loader)
      fx = addOwnerToOwned(fx, loader.ownedClassNames)
      fx
    }.sortedWith(effectComparator)
  }

  private val effectComparator: Comparator<Effect> =
      compareBy(
          {
            val t = it.trigger
            when {
              t == WhenGain -> if (it.automatic) -1 else 0
              t == WhenRemove -> if (it.automatic) 1 else 2
              t is OnGainOf && t.typeExpr.className.toString().startsWith(USE_ACTION.toString()) -> 4
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

  override fun toString() = "$name"

  /** A detailed multi-line description of the class. */
  // TODO this is a lot of presentation logic...
  public fun describe(): String {
    fun descendingBySubclassCount(classes: Iterable<PClass>) =
        classes.sortedWith(compareBy({ -it.allSubclasses.size }, { it.name }))

    val supers = descendingBySubclassCount(allSuperclasses - this - loader.componentClass)
    val superstring = if (supers.isEmpty()) "(none)" else supers.joinToString()

    val subs = descendingBySubclassCount(allSubclasses - this)
    val substring =
        when (subs.size) {
          0 -> "(none)"
          in 1..11 -> subs.joinToString()
          else -> subs.take(10).joinToString() + " (${subs.size - 10} others)"
        }
    val fx = classEffects.joinToString("\n                ")
    return """
      Name:     $name
      Id:       $id
      Abstract: $abstract
      Supers:   $superstring
      Subs:     $substring
      Deps:     ${baseType.allDependencies.types.joinToString()}
      Effects:  $fx
    """
        .trimIndent()
  }

  fun isBaseType(typeExpr: TypeExpr) = loader.resolveType(typeExpr) == baseType
}
