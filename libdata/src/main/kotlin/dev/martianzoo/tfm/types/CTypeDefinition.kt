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

  override fun equals(other: Any?) = other is CTypeDefinition &&
      table == other.table && name == other.name

  override fun hashCode() = hash(table, name)
}
