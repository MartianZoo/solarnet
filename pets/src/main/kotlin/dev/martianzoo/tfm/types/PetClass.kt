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
import dev.martianzoo.tfm.types.Dependency.ClassDependency
import dev.martianzoo.tfm.types.Dependency.TypeDependency

/**
 */
class PetClass(val decl: ClassDeclaration, val loader: PetClassLoader) {
  val name by decl::className
  val abstract by decl::abstract


// HIERARCHY

  // TODO collapse invariants right?

  val directSupertypes: Set<PetType> by lazy {
    decl.supertypes.map { loader.resolve/*WithDefaults*/(it) }.toSet()
  }

  fun isSubtypeOf(that: PetClass) = that in allSuperclasses

  val directSubclasses: Set<PetClass> by lazy { loader.all().filter { this in it.directSuperclasses }.toSet() }
  val allSubclasses: Set<PetClass> by lazy { loader.all().filter { this in it.allSuperclasses }.toSet() }

  val directSuperclasses: Set<PetClass> by lazy { decl.superclassNames.map { loader.load(it) }.toSet() }
  val allSuperclasses: Set<PetClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

// DEPENDENCIES

  val directDependencyKeys: Set<DependencyKey> by lazy {
    decl.dependencies.withIndex().map {
      (i, dep) -> DependencyKey(this, i, dep.classDependency)
    }.toSet()
  }

  val allDependencyKeys: Set<DependencyKey> by lazy {
    allSuperclasses.flatMap { it.directDependencyKeys }.toSet()
  }

  fun resolveSpecializations(specs: List<TypeExpression>): DependencyMap {
    val dependencies = baseType.dependencies
    return dependencies.findMatchups(specs, loader)
  }

  /** Common supertype of all types with petClass==this */
  val baseType: PetType by lazy {
    val deps = DependencyMap.merge(directSupertypes.map { it.dependencies })

    val newDeps = directDependencyKeys.associateWith {
      val typeExpression = decl.dependencies[it.index].upperBound
      if (it.classDep) {
        ClassDependency(it, loader[typeExpression.className])
      } else {
        TypeDependency(it, loader.resolve(typeExpression))
      }
    }
    val allDeps = deps.merge(DependencyMap(newDeps))
    require(allDeps.keyToDep.keys == allDependencyKeys)
    PetType(this, allDeps)
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
          println("raw effect was: $it")
          it
        }
        .map { deprodify(it, loader.resourceNames) }
        .map { resolveSpecialThisType(it, te(name)) }
        .map { applyDefaultsIn(it, loader) }
        .toList()
        .also { validateAllTypes(it, loader) }
  }

  private fun validateAllTypes(effects: List<Effect>, loader: PetClassLoader) {
    // val fx = effects.map { replaceTypesIn(it, THIS.type, te(name)) }
    Validator(loader).transform(effects)
  }

  class Validator(val loader: PetClassLoader) : AstTransformer() {
    override fun <P : PetsNode?> transform(node: P): P {
      if (node is TypeExpression) loader.resolve(node)
      return super.transform(node)
    }
  }

  // OTHER

  /** Returns the one of `this` or `that` that is a subclass of the other. */
  fun glb(that: PetClass) = when {
    this.isSubtypeOf(that) -> this
    that.isSubtypeOf(this) -> that
    else -> error("we ain't got no intersection types")
  }

  fun lub(that: PetClass) = when {
    this.isSubtypeOf(that) -> that
    that.isSubtypeOf(this) -> this
    else -> allSuperclasses.intersect(that.allSuperclasses).maxBy { it.allSuperclasses.size }
  }

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
