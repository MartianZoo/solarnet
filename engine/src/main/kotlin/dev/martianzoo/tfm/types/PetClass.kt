package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.types.PetType.PetGenericType
import dev.martianzoo.util.Debug.d

data class PetClass(
    private val declaration: ClassDeclaration,
    val directSuperclasses: List<PetClass>,
    private val loader: PetClassLoader,
) {

  val name: ClassName by declaration::name
  val abstract by declaration::abstract

  val id by declaration::id
  val invariantsRaw by declaration::otherInvariants

  // HIERARCHY

  val directSupertypes: Set<PetGenericType> by lazy { loader.resolveAll(declaration.supertypes) }

  fun isSubclassOf(that: PetClass): Boolean =
      this == that || directSuperclasses.any { it.isSubclassOf(that) }

  fun isSuperclassOf(that: PetClass) = that.isSubclassOf(this)

  val directSubclasses: Set<PetClass> by lazy {
    require(loader.frozen)
    loader.loadedClasses().filter { this in it.directSuperclasses }.toSet().d("$this dirsubs")
  }

  val allSubclasses: Set<PetClass> by lazy {
    require(loader.frozen)
    loader.loadedClasses().filter { this in it.allSuperclasses }.toSet()
  }

  val allSuperclasses: Set<PetClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  val intersectionType: Boolean by lazy {
    if (directSuperclasses.size < 2) {
      false
    } else {
      require(loader.frozen)
      val sharesAllMySuperclasses = loader.loadedClasses().filter {
        directSuperclasses.all(it::isSubclassOf)
      }
      sharesAllMySuperclasses.all(::isSuperclassOf)
    }
  }

  infix fun intersect(that: PetClass): PetClass? = when {
    this.isSubclassOf(that) -> this
    that.isSubclassOf(this) -> that
    else -> {
      val inters = allSubclasses.filter {
        it.intersectionType && this in it.directSuperclasses && that in it.directSuperclasses
      }
      if (inters.size == 1) inters.first() else null
    }
  }

// DEPENDENCIES

  val directDependencyKeys: Set<Dependency.Key> by lazy {
    declaration.dependencies.indices.map { Dependency.Key(this, it) }.toSet()
  }

  val allDependencyKeys: Set<Dependency.Key> by lazy {
    (directSuperclasses.flatMap { it.allDependencyKeys } + directDependencyKeys).toSet()
  }

  private var reentryCheck = false

  /** Common supertype of all types with petClass==this */
  val baseType: PetGenericType by lazy {
    require(!reentryCheck)
    reentryCheck = true

    val deps = DependencyMap.intersect(directSupertypes.map { it.dependencies })

    val newDeps = directDependencyKeys.associateWith {
      val typeExpression = declaration.dependencies[it.index].type
      Dependency(it, loader.resolve(typeExpression))
    }
    val allDeps = deps.intersect(DependencyMap(newDeps))
    require(allDeps.keys == allDependencyKeys)
    PetGenericType(this, allDeps, null).d { "$this baseType: $it" }
  }

// DEFAULTS

  val defaults: Defaults by lazy {
    val result = if (name == COMPONENT) {
      Defaults.from(declaration.defaultsDeclaration, this, loader)
    } else {
      val rootDefaults = loader[COMPONENT].defaults
      defaultsIgnoringRoot.overlayOn(listOf(rootDefaults))
    }
    if (!result.isEmpty()) d("defaults: $result")
    result
  }

  private val defaultsIgnoringRoot: Defaults by lazy { // TODO hack
    if (name == COMPONENT) {
      Defaults()
    } else {
      Defaults.from(declaration.defaultsDeclaration, this, loader)
          .overlayOn(directSuperclasses.map { it.defaultsIgnoringRoot })
    }
  }

// EFFECTS

  val effectsRaw by declaration::effectsRaw
  override fun toString() = "$name"
}
