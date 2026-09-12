package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition

internal object RepeatPlacementBonus : CustomClass() {
  override fun translate(game: GameReader, type0: Type, type1: Type): InstructionTree {
    val map = mapDefinition(game)
    val areaNames = map.areas.mapTo(hashSetOf()) { it.className }
    val area = listOf(type0, type1).single { it.className in areaNames }
    val bonus = map.areas.single { it.className == area.className }.bonus ?: return NoOp
    return InstructionGroup.createTree(bonus.instructions + bonus.instructions)
  }
}
