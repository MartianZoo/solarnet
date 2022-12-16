package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.Deprodifier.Companion.deprodify
import dev.martianzoo.tfm.types.DependencyMap.DependencyKey
import dev.martianzoo.util.toSetCareful

/**
 * Complete knowledge about a component class, irrespective of how it happened to be defined. This
 * data is relatively "cooked", but I'm still deciding how much inherited information it should
 * include.
 */
data class PetClass(val name: String, val loader: PetClassLoader) {
  val def = loader.definitions[name]!!
  val abstract = def.abstract

  // Not usually a fan of the escaping `this` but this is fairly controlled
  init { loader.define(this) }


// SUPERCLASSES

  val directSuperclasses: Set<PetClass> by lazy {
    val directSuperclassNames: Set<String> = def.supertypes.map { it.className }.toSetCareful()
    // TODO prune
    directSuperclassNames.map { loader.getOrDefine(it) }.toSetCareful()
  }

  val allSuperclasses: Set<PetClass> by lazy {
    allSuperclasses(poison = this)
  }

  fun allSuperclasses(poison: PetClass): Set<PetClass> {
    return (directSuperclasses.flatMap {
      require(it != poison)
      it.allSuperclasses(poison)
    } + this).toSet()
  }

  fun allSubclasses() = loader.all().filter { it.isSubclassOf(this) }.toSetCareful()
  fun isSubclassOf(other: PetClass) = other in allSuperclasses
  fun isSuperclassOf(other: PetClass) = other.isSubclassOf(this)


// DEPENDENCIES

  val directDependencyKeys: Set<DependencyKey> by lazy {
    def.dependencies.indices.map { DependencyKey(this, it + 1) }.toSet()
  }

  // Any dep declared in any supertype all goes together
  val allDependencyKeys: Set<DependencyKey> by lazy {
    val keys = mutableSetOf<DependencyKey>()
    visitSuperclasses { keys += it.directDependencyKeys }
    keys
  }


// EFFECTS

  val directEffects by lazy {
    val sr = loader.get("StandardResource")
    val stdResNames = sr.allSubclasses().map { it.name }.toSetCareful()
    def.effects.map { deprodify(it, stdResNames, "Production") }
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


// HELPERS

  private fun visitSuperclasses(fn: (PetClass) -> Unit) = visitSuperclasses(mutableSetOf(), fn)

  private fun visitSuperclasses(visited: MutableSet<PetClass>, fn: (PetClass) -> Unit) {
    if (visited.add(this)) {
      directSuperclasses.forEach { it.visitSuperclasses(visited, fn) }
      fn(this)
    }
  }


  override fun toString() = name
}
