package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.PetNodeVisitor
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.pets.replaceThis
import dev.martianzoo.tfm.types.PetType.PetGenericType
import dev.martianzoo.util.Debug.d

class PetClass(
    declaration: ClassDeclaration,
    val directSuperclasses: List<PetClass>,
    private val loader: PetClassLoader,
) : PetType {

  val name: ClassName by declaration::name
  override val abstract by declaration::abstract
  override val petClass = this

  // These are for when this is used as a class type / class literal
  override val dependencies = DependencyMap()
  override val refinement = null

  val shortName by declaration::id
  val invariants by declaration::otherInvariants

  // HIERARCHY

  // TODO collapse invariants right?

  val directSupertypes: Set<PetGenericType> by lazy {
    declaration.supertypes.map {
      loader.resolve(replaceThis(it, name.type)) // TODO eh?
    }.toSet().also {
      if (it.size > 1) (it - COMPONENT.type).d("$this supertypes")
    }
  }

  fun isSubclassOf(that: PetClass): Boolean =
      this == that || directSuperclasses.any { it.isSubclassOf(that) }

  fun isSuperclassOf(that: PetClass) = that.isSubclassOf(this)

  override fun isSubtypeOf(that: PetType) = that is PetClass && isSubclassOf(that.petClass)

  val directSubclasses: Set<PetClass> by lazy {
    allClasses().filter { this in it.directSuperclasses }.toSet().d("$this dirsubs")
  }

  val allSubclasses: Set<PetClass> by lazy {
    allClasses().filter { this in it.allSuperclasses }.toSet()
  }

  private fun allClasses(): Set<PetClass> {
    require(loader.isFrozen())
    return loader.loadedClasses()
  }

  val allSuperclasses: Set<PetClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  val intersectionType: Boolean by lazy {
    if (directSuperclasses.size < 2) {
      false
    } else {
      val sharesAllMySuperclasses = allClasses().filter {
        directSuperclasses.all(it::isSubclassOf)
      }
      sharesAllMySuperclasses.all(::isSuperclassOf)
    }
  }

  /**
   * Returns the one of `this` or `that` that is a subclass of the other.
   * In practice some types like `OwnedTile` and `ActionCard` could serve as intersection types.
   * TODO
   */
  fun intersect(that: PetClass): PetClass? = when {
    this.isSubclassOf(that) -> this
    that.isSubclassOf(this) -> that
    else -> {
      val inters = allSubclasses.filter {
        it.intersectionType && this in it.directSuperclasses && that in it.directSuperclasses
      }
      if (inters.size == 1) inters.first() else null
    }
  }

  fun lub(that: PetClass) = when {
    this.isSubclassOf(that) -> that
    that.isSubclassOf(this) -> this
    else -> allSuperclasses.intersect(that.allSuperclasses).maxBy { it.allSuperclasses.size }
  }

  override fun intersect(that: PetType): PetClass {
    val tried = intersect(that as PetClass)
    require(tried != null) { "can't intersect $this and $that" }
    return tried
  }

// DEPENDENCIES

  val directDependencyKeys: Set<Dependency.Key> by lazy {
    declaration.dependencies.indices.map { Dependency.Key(this, it) }.toSet()
  }

  val allDependencyKeys: Set<Dependency.Key> by lazy {
    (directSuperclasses.flatMap { it.allDependencyKeys } + directDependencyKeys).toSet()
        .d { "$this has ${it.size} deps" }
  }

  fun resolveSpecializations(specs: List<PetType>) = baseType.dependencies.findMatchups(specs)

  @JvmName("resolveSpecializations2")
  fun resolveSpecializations(specs: List<TypeExpression>) =
      resolveSpecializations(specs.map { loader.resolve(it) })

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

  fun toDependencyMap(specs: List<TypeExpression>?) = specs?.let {
    loader.resolve(name.addArgs(it)).dependencies
  } ?: DependencyMap()

// DEFAULTS

  val defaults: Defaults by lazy {
    val result = if (name == COMPONENT) {
      Defaults.from(declaration.defaultsDeclaration, this)
    } else {
      val rootDefaults = loader[COMPONENT].defaults
      defaultsIgnoringRoot.overlayOn(listOf(rootDefaults))
    }
    if (!result.isEmpty()) d("defaults: $result")
    result
  }

  private val defaultsIgnoringRoot: Defaults by lazy {
    if (name == COMPONENT) {
      Defaults()
    } else {
      Defaults.from(declaration.defaultsDeclaration, this)
          .overlayOn(directSuperclasses.map { it.defaultsIgnoringRoot })
    }
  }

// EFFECTS

  val effectsRaw by declaration::effectsRaw

  val effects: List<Effect> by lazy {
    effectsRaw
        .map { deprodify(it, loader.resourceNames()) }
        .map { replaceThis(it, name.type) }
        .map { applyDefaultsIn(it, loader).d("$this effect") }
        .toList()
  }

// OTHER

  // includes abstract
  fun isSingleton(): Boolean =
      invariants.any { requiresAnInstance(it) } ||
          directSuperclasses.any { it.isSingleton() }

  private fun requiresAnInstance(r: Requirement): Boolean {
    return r is Min && r.sat == sat(1, THIS.type) ||
        r is Exact && r.sat == sat(1, THIS.type) ||
        r is And && r.requirements.any { requiresAnInstance(it) }
  }

  override fun toTypeExpression() = name.literal

  override fun equals(other: Any?) =
      other is PetClass &&
      this.name == other.name &&
      this.loader === other.loader

  override fun hashCode() = name.hashCode() xor loader.hashCode()

  override fun toString() = "$name"
}
