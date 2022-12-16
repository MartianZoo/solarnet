package dev.martianzoo.tfm.types

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

  // Superclass stuff

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

  fun isSubclassOf(other: PetClass) = other in allSuperclasses
  fun isSuperclassOf(other: PetClass) = other.isSubclassOf(this)

  val directDependencyKeys: Set<DependencyKey> by lazy {
    def.dependencies.indices.map { DependencyKey(this, it + 1) }.toSet()
  }

  // Any dep declared in any supertype all goes together
  val allDependencyKeys: Set<DependencyKey> by lazy {
    val keys = mutableSetOf<DependencyKey>()
    visitSuperclasses { keys += it.directDependencyKeys }
    keys
  }

  private fun visitSuperclasses(fn: (PetClass) -> Unit) = visitSuperclasses(mutableSetOf(), fn)

  private fun visitSuperclasses(visited: MutableSet<PetClass>, fn: (PetClass) -> Unit) {
    if (visited.add(this)) {
      directSuperclasses.forEach { it.visitSuperclasses(visited, fn) }
      fn(this)
    }
  }

  //fun type(key: DependencyKey): PetClass {
  //  if (key.declaringClass == this) {
  //    return loader.getOrDefine(def.dependencies[key.index - 1].className)
  //  }
  //}

  val directEffects = def.effects

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
      candidates.maxBy { it.allSuperclasses.size } // TODO is this even right?
    }
  }

  override fun toString() = name
}
