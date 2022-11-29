package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.CTypeDefinition
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Instruction
import java.util.Objects.hash

class CTypeClass(
    val name: String,
    val superclasses: Set<CTypeClass>,
    val dependencies: DependencyMap,
    val immediate: Instruction?, // TODO: specialize these 3?
    val actions: Set<Action>,
    val effects: Set<Effect>,
    val definition: CTypeDefinition,
    val table: CTypeTable
) {
  init {
    require(name !in table)
    if (name == "Component") {
      require(superclasses.isEmpty())
    } else {
      require(superclasses.isNotEmpty())
    }
  }

  override fun equals(other: Any?) = other is CTypeClass &&
      table == other.table && name == other.name

  override fun hashCode() = hash(table, name)
  fun isSubclassOf(other: CTypeClass) = other in superclasses

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
