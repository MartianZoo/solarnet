package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.replaceThis
import dev.martianzoo.tfm.types.Dependency.ClassDependency
import dev.martianzoo.tfm.types.Dependency.ClassDependency.Companion.KEY
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.toSetStrict

/**
 * A class that has been loaded by a [PClassLoader] based on a [ClassDeclaration]. Each loader has
 * its own separate universe of [PClass]es, and each of these [PClass]es knows what loader it came
 * from. This loaded class should be the source for any information you need to know about a class,
 * but the declaration itself can always be looked up if necessary.
 */
public data class PClass internal constructor(
    private val declaration: ClassDeclaration,
    private val loader: PClassLoader,
    internal val directSuperclasses: List<PClass> = declaration.superclassNames.map(loader::load),
) {
  /** The name of this class, in UpperCamelCase. */
  public val name: ClassName by declaration::name

  /** A short name for this class, such as `"CT"` for `CityTile`; is often the same as [name]. */
  public val id: ClassName by declaration::id

  /**
   * If true, all types with this as their root are abstract, even when all dependencies are
   * concrete.
   */
  public val abstract: Boolean by declaration::abstract

  // HIERARCHY

  public fun isSubclassOf(that: PClass): Boolean =
      this == that || directSuperclasses.any { it.isSubclassOf(that) }

  public fun isSuperclassOf(that: PClass) = that.isSubclassOf(this)

  public val allSuperclasses: Set<PClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  public val directSubclasses: Set<PClass> by lazy {
    loader.allClasses.filter { this in it.directSuperclasses }.toSet()
  }

  public val allSubclasses: Set<PClass> by lazy {
    loader.allClasses.filter { this in it.allSuperclasses }.toSet()
  }

  public val directSupertypes: Set<PType> by lazy {
    declaration.supertypes.map(loader::resolveType).toSetStrict()
  }

  public val intersectionType: Boolean by lazy {
    directSuperclasses.size >= 2 &&
        loader.allClasses
            .filter { pclass -> directSuperclasses.all(pclass::isSubclassOf) }
            .all(::isSuperclassOf)
  }

  public infix fun intersect(that: PClass): PClass? =
      when {
        this.isSubclassOf(that) -> this
        that.isSubclassOf(this) -> that
        else -> allSubclasses.filter {
          it.intersectionType &&
              this in it.directSuperclasses &&
              that in it.directSuperclasses
        }.singleOrNull()
      }

  // DEPENDENCIES

  internal val directDependencyKeys: Set<Dependency.Key> by lazy {
    declaration.dependencies.indices.map { Dependency.Key(name, it) }.toSet()
  }

  internal val allDependencyKeys: Set<Dependency.Key> by lazy {
    (directSuperclasses.flatMap { it.allDependencyKeys } + directDependencyKeys).toSet()
  }

  public fun toClassType() =
      PType(loader.classClass, DependencyMap(mapOf(KEY to ClassDependency(this))))

  /** Least upper bound of all types with pclass==this */
  public val baseType: PType by lazy {
    if (name == CLASS) {
      // base type of Class is Class<Component>
      loader.componentClass.toClassType()
    } else {
      val newDeps =
          directDependencyKeys.associateWith {
            val depTypeExpr = declaration.dependencies[it.index].typeExpr
            TypeDependency(it, loader.resolveType(depTypeExpr))
          }
      val deps = DependencyMap.intersect(directSupertypes.map { it.dependencies })
      val allDeps = deps.intersect(DependencyMap(newDeps))
      require(allDeps.keys == allDependencyKeys)
      PType(this, allDeps, null)
    }
  }

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

  val classEffects: List<Effect> by lazy {
    declaration.effectsRaw.map {
      var fx = it
      fx = deprodify(fx, loader)
      fx = replaceThis(fx, baseType.toTypeExprFull())
      fx = applyDefaultsIn(fx, loader)
      fx
    }
  }

  // OTHER

  // includes abstract
  internal fun isSingleton(): Boolean =
      declaration.otherInvariants.any { it.requiresThis() } ||
          directSuperclasses.any { it.isSingleton() }

  override fun toString() = "$name"
}
