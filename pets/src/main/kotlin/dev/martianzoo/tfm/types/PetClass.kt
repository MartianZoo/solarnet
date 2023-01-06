package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.AstTransformer
import dev.martianzoo.tfm.pets.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialComponent.COMPONENT
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.pets.resolveSpecialThisType
import dev.martianzoo.tfm.types.PetType.PetGenericType

/**
 */
class PetClass(val decl: ClassDeclaration, val loader: PetClassLoader) {
  val name by decl::className
  val abstract by decl::abstract

// HIERARCHY

  // TODO collapse invariants right?

  val directSupertypes: Set<PetGenericType> by lazy {
    decl.supertypes.map { loader.resolve/*WithDefaults*/(it) as PetGenericType }.toSet()
  }

  fun isSubtypeOf(that: PetClass) = that in allSuperclasses

  val directSubclasses: Set<PetClass> by lazy { loader.all().filter { this in it.directSuperclasses }.toSet() }
  val allSubclasses: Set<PetClass> by lazy { loader.all().filter { this in it.allSuperclasses }.toSet() }

  val directSuperclasses: Set<PetClass> by lazy { decl.superclassNames.map { loader.load(it) }.toSet() }
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
    decl.dependencies.indices.map { Dependency.Key(this, it) }.toSet()
  }

  val allDependencyKeys: Set<Dependency.Key> by lazy {
    allSuperclasses.flatMap { it.directDependencyKeys }.toSet()
  }

  fun resolveSpecializations(specs: List<TypeExpression>): DependencyMap {
    val dependencies = baseType.dependencies
    return dependencies.findMatchups(specs, loader)
  }

  private var reentryCheck = false
  /** Common supertype of all types with petClass==this */
  val baseType: PetGenericType by lazy {
    require(!reentryCheck)
    reentryCheck = true

    val deps = DependencyMap.intersect(directSupertypes.map { it.dependencies })

    val newDeps = directDependencyKeys.associateWith {
      val typeExpression = decl.dependencies[it.index].upperBound
      Dependency(it, loader.resolve(typeExpression))
    }
    val allDeps = deps.intersect(DependencyMap(newDeps))
    require(allDeps.keys == allDependencyKeys)
    PetGenericType(this, allDeps).also {
      println("baseType is $it")
    }
  }

// DEFAULTS

  val defaults: Defaults by lazy {
    if (name == "$COMPONENT") {
      Defaults.from(decl.defaultsDeclaration, this)
    } else {
      val rootDefaults = loader["$COMPONENT"].defaults
      defaultsIgnoringRoot.overlayOn(listOf(rootDefaults))
    }
  }

  val defaultsIgnoringRoot: Defaults by lazy {
    if (name == "$COMPONENT") {
      Defaults()
    } else {
      Defaults.from(decl.defaultsDeclaration, this)
          .overlayOn(directSuperclasses.map { it.defaultsIgnoringRoot })
    }
  }


// EFFECTS

  val directEffectsRaw by decl::effects

  val directEffects by lazy {
    directEffectsRaw.asSequence()
        .map {
          println("\n0. Class $name, raw effect is: $it")
          it
        }
        .map { deprodify(it, loader.resourceNames) }
        .map { resolveSpecialThisType(it, te(name)) }
        .map { applyDefaultsIn(it, loader) }
        .toList()
        .also { validateAllTypes(it, loader) }
  }


// VALIDATION

  private fun validateAllTypes(effects: List<Effect>, loader: PetClassLoader) {
    Validator(loader).transform(effects)
  }

  class Validator(val loader: PetClassLoader) : AstTransformer() {
    override fun <P : PetsNode?> transform(node: P): P {
      if (node is TypeExpression) loader.resolve(node)
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
