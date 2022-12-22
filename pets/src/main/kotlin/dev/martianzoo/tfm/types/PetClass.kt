package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.Deprodifier.Companion.deprodify
import dev.martianzoo.util.toSetStrict

/**
 */
data class PetClass(val def: ComponentDef, val loader: PetClassLoader): DependencyTarget {
  val name by def::name
  override val abstract by def::abstract


// HIERARCHY

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
    allSuperclasses.flatMap { it.directDependencyKeys }.toSetStrict()
  }

  /** Common supertype of all types with petClass==this */
  val baseType: PetType by lazy {
    val map = mutableMapOf<DependencyKey, DependencyTarget>()
    mergeDepsInto(map) // TODO have DM handle this
    require(map.keys == allDependencyKeys)
    PetType(this, DependencyMap(map))
  }

  fun mergeDepsInto(map: MutableMap<DependencyKey, DependencyTarget>) {
    for (supe in directSuperclasses) {
      supe.mergeDepsInto(map)
    }
    for (key in directDependencyKeys) {
      val typeExpression = def.dependencies[key.index].type
      val depType = if (key.classDep) {
        loader.get(typeExpression.className)
      } else {
        loader.resolve(typeExpression)
      }
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
