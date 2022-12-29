package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.AstTransformer
import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.SpecialComponent.COMPONENT
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.pets.resolveThisIn
import dev.martianzoo.tfm.pets.spellOutQes

/**
 */
class PetClass(val def: ComponentDef, val loader: PetClassLoader): DependencyTarget {
  val name by def::className
  override val abstract by def::abstract


// HIERARCHY

  val directSupertypes: Set<PetType> by lazy {
    def.supertypes.map { loader.resolve/*WithDefaults*/(it) }.toSet()
  }

  override fun isSubtypeOf(that: DependencyTarget) = that in allSuperclasses

  fun isSuperclassOf(that: PetClass) = that.isSubtypeOf(this)

  val directSubclasses: Set<PetClass> by lazy { loader.all().filter { this in it.directSuperclasses }.toSet() }
  val allSubclasses: Set<PetClass> by lazy { loader.all().filter { this in it.allSuperclasses }.toSet() }

  val directSuperclasses: Set<PetClass> by lazy { def.superclassNames.map { loader.load(it) }.toSet() }
  val allSuperclasses: Set<PetClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

// DEPENDENCIES

  val directDependencyKeys: Set<DependencyKey> by lazy {
    def.dependencies.withIndex().map {
      (i, dep) -> DependencyKey(this, i, dep.classDep)
    }.toSet()
  }

  val allDependencyKeys: Set<DependencyKey> by lazy {
    allSuperclasses.flatMap { it.directDependencyKeys }.toSet()
  }

  fun resolveSpecializations(specs: List<TypeExpression>): DependencyMap =
      baseType.dependencies.findMatchups(specs.map { loader.resolve(it) })

  /** Common supertype of all types with petClass==this */
  val baseType: PetType by lazy {
    val deps = DependencyMap.merge(directSupertypes.map { it.dependencies })

    val newDeps = directDependencyKeys.map {
      val typeExpression = def.dependencies[it.index].type
      val depType = if (it.classDep) {
        loader.get(typeExpression.className)
      } else {
        loader.resolve(typeExpression)
      }
      it to depType
    }.toMap()
    val allDeps = deps.merge(DependencyMap(newDeps))
    require(allDeps.keyToType.keys == allDependencyKeys)
    PetType(this, allDeps).also { println("$this baseType is $it") }
  }

// DEFAULTS

  val defaults: Defaults by lazy {
    if (name == "$COMPONENT") {
      Defaults.from(def.rawDefaults, this)
    } else {
      val rootDefaults = loader["$COMPONENT"].defaults
      defaultsIgnoringRoot.overlayOn(listOf(rootDefaults))
    }
  }

  val defaultsIgnoringRoot: Defaults by lazy {
    if (name == "$COMPONENT") {
      Defaults()
    } else {
      Defaults.from(def.rawDefaults, this)
          .overlayOn(directSuperclasses.map { it.defaultsIgnoringRoot })
    }
  }


// EFFECTS

  val directEffectsRaw by def::effects

  val directEffects by lazy {
    directEffectsRaw
        .map {
          println("raw effect was: $it")
          it
        }
        .map { spellOutQes(it) }
        .map { deprodify(it, loader.resourceNames) }
        .map { resolveThisIn(it, te(name)) }
        .map { applyDefaultsIn(it, loader) }
        .also { validateAllTypes(it, loader) }
  }

  private fun validateAllTypes(effects: List<Effect>, loader: PetClassLoader) {
    // val fx = effects.map { replaceTypesIn(it, THIS.type, te(name)) }
    Validator(loader).transform(effects)
  }

  internal class Validator(val loader: PetClassLoader) : AstTransformer() {
    override fun <P : PetsNode?> transform(node: P): P {
      if (node is TypeExpression) loader.resolve(node)
      return super.transform(node)
    }
  }

  override fun toTypeExpression() = te(name)
  override val isClassOnly = true

  // OTHER

  /** Returns the one of `this` or `that` that is a subclass of the other. */
  override fun glb(that: DependencyTarget) = when {
    that !is PetClass -> error("")
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
