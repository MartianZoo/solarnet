package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.types.DependencyMap.DependencyKey
import dev.martianzoo.util.toSetCareful
import java.util.Objects.hash

/**
 * Complete knowledge about a component class, irrespective of how it happened to be defined. This
 * data is relatively "cooked", but I'm still deciding how much inherited information it should
 * include.
 */
class PetClass(val loader: PetClassLoader, val def: ComponentDef) {
  val name = def.name
  val abstract = def.abstract

  init { loader.define(this) }

  val directSuperclassNames by lazy {
    def.supertypes.map { it.className }.toSetCareful()
  }

  val directSuperclasses by lazy {
    directSuperclassNames.map { loader.getOrDefine(it) } // TODO prevent circularity
  }

  val directSupertypes by lazy {
    def.supertypes.map { loader.resolve(it) }
  }

  val dependencies by lazy {
    val brandNewDeps = def.dependencies.withIndex().map { (i, typeExpr) ->
       DependencyKey(def.name, index = i + 1) to loader.resolve(typeExpr)
    }.toMap()
    DependencyMap.merge(directSupertypes.map { it.dependencies } + DependencyMap(brandNewDeps))
  }

  val directEffects = def.effects

  fun isSubclassOf(other: PetClass): Boolean {
    // TODO resolve em all early
    return other == this || directSuperclasses.any { it.isSubclassOf(other) }
  }

  fun glb(other: PetClass) = when {
    this.isSubclassOf(other) -> this
    other.isSubclassOf(this) -> other
    else -> error("ad-hoc intersection types not supported")
  }

  override fun equals(other: Any?) = other is PetClass &&
      loader == other.loader && name == other.name
  override fun hashCode() = hash(loader, name)

  override fun toString() = name

}
