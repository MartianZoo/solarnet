package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.AstTransformer
import dev.martianzoo.tfm.pets.SpecialComponent.Component
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.pets.replaceThis
import dev.martianzoo.tfm.types.PetType.PetGenericType
import dev.martianzoo.util.toSetStrict

/**
 */
internal class PetClass(
    private val declaration: ClassDeclaration,
    val directSuperclasses: List<PetClass>,
    private val loader: PetClassLoader
) : PetType {
  val name by declaration::className
  override val abstract by declaration::abstract
  override val petClass = this

  // HIERARCHY

  // TODO collapse invariants right?

  val directSupertypes: Set<PetGenericType> by lazy {
    declaration.supertypes.map {
      loader.resolve(replaceThis(it, gte(name)))
    }.toSet()
  }

  fun isSubclassOf(that: PetClass): Boolean =
      this == that || directSuperclasses.any { it.isSubclassOf(that) }

  fun isSuperclassOf(that: PetClass) = that.isSubclassOf(this)

  override fun isSubtypeOf(that: PetType) =
      that is PetClass && isSubclassOf(that.petClass)

  val directSubclasses: Set<PetClass> by lazy {
    allClasses().filter { this in it.directSuperclasses }.toSet()
  }

  val allSubclasses: Set<PetClass> by lazy {
    allClasses().filter { this in it.allSuperclasses }.toSet()
  }

  private fun allClasses(): Set<PetClass> {
    require(loader.isFrozen())
    return loader.loadedClassNames().map { loader[it] }.toSetStrict()
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
  /** Returns the one of `this` or `that` that is a subclass of the other. */
  fun intersect(that: PetClass) = when {
    this.isSubclassOf(that) -> this
    that.isSubclassOf(this) -> that
    else -> error("no intersection: $this, $that")
  }

  fun canIntersect(that: PetClass) =
      this.isSubclassOf(that) ||
      that.isSubclassOf(this)

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
    allSuperclasses.flatMap { it.directDependencyKeys }.toSet()
  }

  fun resolveSpecializations(specs: List<PetType>) =
      baseType.dependencies.findMatchups(specs)

  @JvmName("whoCares")
  fun resolveSpecializations(specs: List<TypeExpression>) =
      resolveSpecializations(specs.map { loader.resolve(it) })

  private var reentryCheck = false
  /** Common supertype of all types with petClass==this */
  val baseType: PetGenericType by lazy {
    require(!reentryCheck)
    reentryCheck = true

    val deps = DependencyMap.intersect(directSupertypes.map { it.dependencies })

    val newDeps = directDependencyKeys.associateWith {
      val typeExpression = declaration.dependencies[it.index].upperBound
      Dependency(it, loader.resolve(typeExpression))
    }
    val allDeps = deps.intersect(DependencyMap(newDeps))
    require(allDeps.keys == allDependencyKeys)
    PetGenericType(this, allDeps).also {
      println("baseType is $it")
    }
  }

  internal fun toDependencyMap(specs: List<TypeExpression>?) =
      specs?.let {
        loader.resolve(GenericTypeExpression(name, it)).dependencies
      } ?: DependencyMap()


// DEFAULTS

  val defaults: Defaults by lazy {
    if (name == Component.name) {
      Defaults.from(declaration.defaultsDeclaration, this)
    } else {
      val rootDefaults = loader[Component.name].defaults
      defaultsIgnoringRoot.overlayOn(listOf(rootDefaults))
    }
  }

  private val defaultsIgnoringRoot: Defaults by lazy {
    if (name == Component.name) {
      Defaults()
    } else {
      Defaults.from(declaration.defaultsDeclaration, this)
          .overlayOn(directSuperclasses.map { it.defaultsIgnoringRoot })
    }
  }


// EFFECTS

  val directEffectsRaw by declaration::effectsRaw

  val directEffects by lazy {
    directEffectsRaw.asSequence()
        .map {
          println("\n0. Class $name, raw effect is: $it")
          it
        }
        .map { deprodify(it, loader.resourceNames()) }
        .map { replaceThis(it, gte(name)) }
        .map { applyDefaultsIn(it, loader) }
        .toList()
        .also { validateAllTypes(it) }
  }


// VALIDATION

  private fun validateAllTypes(effects: List<Effect>) {
    Validator(loader).transform(effects)
  }

  class Validator(private val table: PetClassTable) : AstTransformer() {
    override fun <P : PetsNode?> transform(node: P): P {
      if (node is TypeExpression) table.resolve(node)
      return super.transform(node)
    }
  }


// OTHER

  override fun equals(other: Any?): Boolean {
    return other is PetClass &&
        this.name == other.name &&
        this.loader === other.loader
  }

  override fun hashCode(): Int {
    return name.hashCode() xor loader.hashCode()
  }

  override fun toTypeExpressionFull() = ClassExpression(name)

  override fun toString() = name
}
