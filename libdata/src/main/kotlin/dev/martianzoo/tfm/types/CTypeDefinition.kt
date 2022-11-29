package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.CTypeData
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import java.util.Objects.hash

class CTypeDefinition(
    val name: String,
    val supertypes: Set<Expression>,
    val dependencies: List<BaseDependency>,
    val immediate: Instruction?,
    val actions: Set<Action>,
    val effects: Set<Effect>,
    val data: CTypeData,
    val table: CTypeTable
) {
  fun isSubtypeOf(other: Expression) = other in supertypes // TODO

  override fun equals(other: Any?) = other is CTypeDefinition &&
      table == other.table && name == other.name

  override fun hashCode() = hash(table, name)

  // ("Owned", table.resolve("Anyone"))
  // ("Tile", table.resolve("Area"))
  // ("Cardbound", table.resolve("CardFront"))
  // ("Production", table.resolve("StandardResource"), true)
  // ("Adjacency", table.resolve("Tile"), 0)
  // ("Adjacency", table.resolve("Tile"), 1)
  data class BaseDependency(
      val dependentTypeName: String,
      val dependencyType: CType,
      val isTypeOnly: Boolean = false,
      val index: Int = 0,
  )

}
