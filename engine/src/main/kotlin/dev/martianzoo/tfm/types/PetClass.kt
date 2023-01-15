package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.PetNodeVisitor
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScalarAndType
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.pets.replaceThis
import dev.martianzoo.tfm.types.PetType.PetGenericType
import dev.martianzoo.util.Debug.d

internal class PetClass(
    declaration: ClassDeclaration,
    val directSuperclasses: List<PetClass>,
    private val loader: PetClassLoader,
) : PetType {
  val name: ClassName by declaration::name
  override val abstract by declaration::abstract
  override val petClass = this

  val shortName by declaration::id
  val invariants by declaration::otherInvariants

  // HIERARCHY

  // TODO collapse invariants right?

  val directSupertypes: Set<PetGenericType> by lazy {
    declaration.supertypes.map {
      loader.resolve(replaceThis(it, gte(name))) // TODO eh?
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
  fun intersect(that: PetClass) = when {
    this.isSubclassOf(that) -> this
    that.isSubclassOf(this) -> that
    else -> error("no intersection: $this, $that")
  }

  fun canIntersect(that: PetClass) = this.isSubclassOf(that) || that.isSubclassOf(this)

  fun lub(that: PetClass) = when {
    this.isSubclassOf(that) -> that
    that.isSubclassOf(this) -> this
    else -> allSuperclasses.intersect(that.allSuperclasses).maxBy { it.allSuperclasses.size }
  }

  override fun canIntersect(that: PetType): Boolean {
    return that is PetClass && this.canIntersect(that.petClass)
  }

  override fun intersect(that: PetType): PetClass {
    return intersect(that as PetClass)
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

  fun formGenericType(specs: List<PetType>, ref: Requirement?) =
      PetGenericType(this, baseType.dependencies.specialize(specs), ref)

  internal fun toDependencyMap(specs: List<TypeExpression>?) = specs?.let {
    loader.resolve(GenericTypeExpression(name, it)).dependencies
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
        .map { replaceThis(it, gte(name)) }
        .map { applyDefaultsIn(it, loader).d("$this effect") }
        .toList()
        .also { validateAllTypes(it) }
  }

// VALIDATION

  private fun validateAllTypes(effects: List<Effect>) {
    class Validator(private val table: PetClassTable) : PetNodeVisitor() {
      override fun <P : PetNode?> transform(node: P): P {
        if (node is TypeExpression) table.resolve(node)
        return super.transform(node)
      }
    }
    Validator(loader).transform(effects)
  }

// OTHER

  // includes abstract
  fun isSingleton(): Boolean =
      invariants.any { requiresAnInstance(it) } ||
          directSuperclasses.any { it.isSingleton() }

  private fun requiresAnInstance(r: Requirement): Boolean {
    return r is Min && r.sat == ScalarAndType(1, gte("This")) ||
        r is Exact && r.sat == ScalarAndType(1, gte("This")) ||
        r is And && r.requirements.any { requiresAnInstance(it) }
  }

  override fun toTypeExpressionFull() = ClassLiteral(name)

  override fun equals(other: Any?) =
      other is PetClass &&
      this.name == other.name &&
      this.loader === other.loader

  override fun hashCode() = name.hashCode() xor loader.hashCode()

  override fun toString() = name.asString
}
