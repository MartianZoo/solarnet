package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.Deprodifier.Companion.deprodify
import dev.martianzoo.tfm.types.DependencyMap.DependencyKey
import dev.martianzoo.util.toSetCareful

/**
 * Complete knowledge about a component class, irrespective of how it happened to be defined. This
 * data is relatively "cooked", but I'm still deciding how much inherited information it should
 * include.«
 */
data class PetClass(val def: ComponentDef, val loader: PetClassLoader) {
  val name by def::name
  val abstract by def::abstract

// SUPERCLASSES

  val directSuperclasses: Set<PetClass> get() = loader.superToSubDirect.predecessors(this)
  val directSubclasses: Set<PetClass> get() = loader.superToSubDirect.successors(this)

  val allSuperclasses: Set<PetClass> get() = loader.superToSubAll!!.predecessors(this)
  val allSubclasses: Set<PetClass> get() = loader.superToSubAll!!.successors(this)

  fun isSubclassOf(that: PetClass) = loader.superToSubAll.hasEdgeConnecting(that, this)
  fun isSuperclassOf(that: PetClass) = loader.superToSubAll.hasEdgeConnecting(this, that)

// DEPENDENCIES

  val directDependencyKeys: Set<DependencyKey> by lazy {
    def.dependencies.indices.map { DependencyKey(this, it + 1) }.toSet()
  }

  // Any dep declared in any supertype all goes together
  val allDependencyKeys: Set<DependencyKey> by lazy {
    allSuperclasses.flatMap { it.directDependencyKeys }.toSetCareful()
  }


// EFFECTS

  val directEffects by lazy {
    val sr = loader.get("StandardResource")
    val resourceNames = sr.allSubclasses.map { it.name }.toSetCareful()
    def.effects.map { deprodify(it, resourceNames, "Production") }
  }


// OTHER

  /** Returns the one of `this` or `that` that is a subclass of the other. */
  fun glb(that: PetClass) = when {
    this.isSubclassOf(that) -> this
    that.isSubclassOf(this) -> that
    else -> error("we ain't got no intersection types")
  }

  fun lub(other: PetClass) = when {
    isSubclassOf(other) -> other
    isSuperclassOf(other) -> this
    else -> {
      // has to be one of these, even if just Component
      val candidates = allSuperclasses.intersect(other.allSuperclasses)

      // well, there cannot be a nearer choice, so... good?
      candidates.maxBy { it.allSuperclasses.size }
    }
  }


  override fun toString() = name
}
