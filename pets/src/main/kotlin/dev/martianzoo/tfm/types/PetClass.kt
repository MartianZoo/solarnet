package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.NodeVisitor
import dev.martianzoo.tfm.pets.SpecialComponent.COMPONENT
import dev.martianzoo.tfm.pets.SpecialComponent.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.pets.replaceTypesIn

/**
 */
data class PetClass(val def: ComponentDef, val loader: PetClassLoader): DependencyTarget {
  val name by def::name
  override val abstract by def::abstract


// HIERARCHY

  val directSupertypes: Set<PetType> by lazy {
    def.supertypes.map { loader.resolve/*WithDefaults*/(it) }.toSet()
  }

  override fun isSubtypeOf(that: DependencyTarget) = loader.allSubclasses.hasEdgeConnecting(that as PetClass, this)

  fun isSuperclassOf(that: PetClass) = loader.allSubclasses.hasEdgeConnecting(this, that)
  val directSubclasses: Set<PetClass> get() = loader.directSubclasses.successors(this)

  val directSuperclasses: Set<PetClass> get() = loader.directSubclasses.predecessors(this)
  val allSubclasses: Set<PetClass> get() = loader.allSubclasses.successors(this)

  val allSuperclasses: Set<PetClass> get() = loader.allSubclasses.predecessors(this)

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
    PetType(this, allDeps)
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

  val directEffects by lazy {
    val resourceNames = loader["$STANDARD_RESOURCE"].allSubclasses.map { it.name }.toSet()
    def.effects
        .map { deprodify(it, resourceNames) }
        .map { replaceTypesIn(it, THIS.type, te(name)) }
        .map { applyDefaultsIn(it, loader) }
        .also { validateAllTypes(it, loader) }
  }

  private fun validateAllTypes(effects: List<Effect>, loader: PetClassLoader) {
    // val fx = effects.map { replaceTypesIn(it, THIS.type, te(name)) }
    Validator(loader).s(effects)
  }

  internal class Validator(val loader: PetClassLoader) : NodeVisitor() {
    override fun <P : PetsNode?> s(node: P): P {
      if (node is TypeExpression) loader.resolve(node)
      return super.s(node)
    }
  }

  override fun toTypeExpression() = te(name)


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

  override fun toString() = name
}
