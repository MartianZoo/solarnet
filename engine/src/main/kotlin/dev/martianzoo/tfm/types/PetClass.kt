package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.AstTransformer
import dev.martianzoo.tfm.pets.SpecialComponent.COMPONENT
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.pets.resolveSpecialThisType
import dev.martianzoo.tfm.types.PetType.PetGenericType

/**
 */
class PetClass(private val declaration: ClassDeclaration, private val loader: PetClassLoader) {
  val name by declaration::className
  val abstract by declaration::abstract

// HIERARCHY

  // TODO collapse invariants right?

  val directSupertypes: Set<PetGenericType> by lazy {
    declaration.supertypes.map { loader.resolve(resolveSpecialThisType(it, te(name))) }.toSet()
  }

  fun isSubtypeOf(that: PetClass) = that in allSuperclasses

  val directSubclasses: Set<PetClass> by lazy { loader.all().filter { this in it.directSuperclasses }.toSet() }
  val allSubclasses: Set<PetClass> by lazy { loader.all().filter { this in it.allSuperclasses }.toSet() }

  val directSuperclasses: Set<PetClass> by lazy { declaration.superclassNames.map { loader.load(it) }.toSet() }
  val allSuperclasses: Set<PetClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  /** Returns the one of `this` or `that` that is a subclass of the other. */
  fun intersect(that: PetClass) = when {
    this.isSubtypeOf(that) -> this
    that.isSubtypeOf(this) -> that
    else -> error("no intersection: $this, $that")
  }

  fun canIntersect(that: PetClass) =
      this.isSubtypeOf(that) ||
      that.isSubtypeOf(this)

  fun lub(that: PetClass) = when {
    this.isSubtypeOf(that) -> that
    that.isSubtypeOf(this) -> this
    else -> allSuperclasses.intersect(that.allSuperclasses).maxBy { it.allSuperclasses.size }
  }


// DEPENDENCIES

  val directDependencyKeys: Set<Dependency.Key> by lazy {
    declaration.dependencies.indices.map { Dependency.Key(this, it) }.toSet()
  }

  val allDependencyKeys: Set<Dependency.Key> by lazy {
    allSuperclasses.flatMap { it.directDependencyKeys }.toSet()
  }

  fun resolveSpecializations(specs: List<PetType>): DependencyMap {
    return baseType.dependencies.findMatchups(specs)
  }

  @JvmName("whoCares")
  fun resolveSpecializations(specs: List<TypeExpression>): DependencyMap {
    return resolveSpecializations(specs.map { loader.resolve(it) })
  }

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
    if (name == "$COMPONENT") {
      Defaults.from(declaration.defaultsDeclaration, this)
    } else {
      val rootDefaults = loader["$COMPONENT"].defaults
      defaultsIgnoringRoot.overlayOn(listOf(rootDefaults))
    }
  }

  private val defaultsIgnoringRoot: Defaults by lazy {
    if (name == "$COMPONENT") {
      Defaults()
    } else {
      Defaults.from(declaration.defaultsDeclaration, this)
          .overlayOn(directSuperclasses.map { it.defaultsIgnoringRoot })
    }
  }


// EFFECTS

  val directEffectsRaw by declaration::effects

  val directEffects by lazy {
    directEffectsRaw.asSequence()
        .map {
          println("\n0. Class $name, raw effect is: $it")
          it
        }
        .map { deprodify(it, loader.resourceNames()) }
        .map { resolveSpecialThisType(it, te(name)) }
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

  override fun equals(that: Any?): Boolean {
    return that is PetClass &&
        this.name == that.name &&
        this.loader === that.loader
  }

  override fun hashCode(): Int {
    return name.hashCode() xor loader.hashCode()
  }

  override fun toString() = name
}
