package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.petaform.Action
import dev.martianzoo.tfm.petaform.Effect
import dev.martianzoo.tfm.petaform.Instruction
import java.util.Objects.hash

/**
 * Complete knowledge about a component class, irrespective of how it happened to be defined. This
 * data is relatively "cooked", but I'm still deciding how much inherited information it should
 * include.
 */
class ComponentClass(
    val loader: ComponentClassLoader,
    val name: String,
    val abstract: Boolean,
    val superclasses: Set<ComponentClass> = setOf(),
    val dependencies: DependencyMap = DependencyMap(),
    val immediate: Instruction? = null, // TODO: specialize these 3? null instruction?
    val actions: Set<Action> = setOf(), // TODO: Map<Trigger, Effect>?
    val effects: Set<Effect> = setOf()
) {
  init {
    require((name == "Component") == superclasses.isEmpty())
    require(loader.table.put(name, this) == null)
  }

  fun isSubclassOf(other: ComponentClass): Boolean {
    return other == this || superclasses.any { it.isSubclassOf(other) }
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

}
