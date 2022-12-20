package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.Deprodifier.Companion.deprodify
import dev.martianzoo.tfm.types.PetType.DependencyKey
import dev.martianzoo.util.toSetStrict

/**
 */
data class PetClass(val def: ComponentDef, val loader: PetClassLoader) {
  val name by def::name
  val abstract by def::abstract


// HIERARCHY

  fun isSubclassOf(that: PetClass) = loader.allSubclasses.hasEdgeConnecting(that, this)
  fun isSuperclassOf(that: PetClass) = loader.allSubclasses.hasEdgeConnecting(this, that)

  val directSubclasses: Set<PetClass> get() = loader.directSubclasses.successors(this)
  val directSuperclasses: Set<PetClass> get() = loader.directSubclasses.predecessors(this)

  val allSubclasses: Set<PetClass> get() = loader.allSubclasses.successors(this)
  val allSuperclasses: Set<PetClass> get() = loader.allSubclasses.predecessors(this)


// DEPENDENCIES

  val directDependencyKeys: Set<DependencyKey> by lazy {
    def.dependencies.indices.map { DependencyKey(this, it) }.toSet()
  }

  val allDependencyKeys: Set<DependencyKey> by lazy {
    allSuperclasses.flatMap { it.directDependencyKeys }.toSetStrict()
  }

  /** Common supertype of all types with petClass==this */
  val baseType: PetType by lazy {
    val map = mutableMapOf<DependencyKey, PetType>()
    mergeDepsInto(map)
    require(map.keys == allDependencyKeys)
    PetType(this, map, _doNotPassThis = false)
  }

  fun mergeDepsInto(map: MutableMap<DependencyKey, PetType>) {
    for (supe in directSuperclasses) {
      supe.mergeDepsInto(map)
    }
    for (key in directDependencyKeys) {
      val depType = loader.resolve(def.dependencies[key.index])
      map.merge(key, depType) { type1, type2 -> type1.glb(type2) }
    }
  }

// EFFECTS

  val directEffects by lazy {
    val resourceNames = loader["StandardResource"].allSubclasses.map { it.name }.toSet()
    def.effects.map { deprodify(it, resourceNames, "Production") }
  }


// OTHER

  /** Returns the one of `this` or `that` that is a subclass of the other. */
  fun glb(that: PetClass) = when {
    this.isSubclassOf(that) -> this
    that.isSubclassOf(this) -> that
    else -> error("we ain't got no intersection types")
  }

  fun lub(that: PetClass) = when {
    this.isSubclassOf(that) -> that
    that.isSubclassOf(this) -> this
    else -> allSuperclasses.intersect(that.allSuperclasses).maxBy { it.allSuperclasses.size }
  }


  override fun toString() = name
}
