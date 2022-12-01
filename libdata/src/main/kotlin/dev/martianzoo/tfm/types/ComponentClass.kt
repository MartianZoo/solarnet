package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Instruction
import java.util.Objects.hash

/**
 * Complete knowledge about a component class, irrespective of how it happened to be defined.
 * This data is "cooked" as much as possible, such as including all inherited information.
 */
class ComponentClass(
    val name: String,
    val superclasses: Set<ComponentClass>,
    val dependencies: DependencyMap,
    val abstract: Boolean,
    val immediate: Instruction?, // TODO: specialize these 3? null instruction?
    val actions: Set<Action>,
    val effects: Set<Effect>, // TODO: Map<Trigger, Effect>?
    val loader: ComponentClassLoader
) {
  init {
    require((name == "Component") == superclasses.isEmpty())
    require(loader.table.put(name, this) == null)
  }

  fun isSubclassOf(other: ComponentClass): Boolean {
    return other in superclasses
  }

  fun glb(other: ComponentClass) = when {
    this.isSubclassOf(other) -> this
    other.isSubclassOf(this) -> other
    else -> error("ad-hoc intersection types not supported")
  }

  override fun equals(other: Any?) = other is ComponentClass &&
      loader == other.loader && name == other.name
  override fun hashCode() = hash(loader, name)

  override fun toString() = name

  // ("Owned", table.resolve("Anyone"))
  // ("Tile", table.resolve("Area"))
  // ("Production", table.resolve("StandardResource"), true)
  // ("Adjacency", table.resolve("Tile"), 0)
  // ("Adjacency", table.resolve("Tile"), 1)
  data class DependencyKey(
      val dependentTypeName: String,
      val isTypeOnly: Boolean = false,
      val index: Int = 0,
  ) {
    override fun toString() = "DEP:$dependentTypeName[$index]" + if (isTypeOnly) " (TYPE)" else ""
  }
}
