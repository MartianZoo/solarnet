package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.ReadOnlyGameState
import dev.martianzoo.tfm.api.lookUpProductionLevels
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.childNodesOfType

internal val allCustomInstructions =
    listOf(
        GainLowestProduction,
        CopyProductionBox,
    )

// For Robinson Industries
private object GainLowestProduction : CustomInstruction("gainLowestProduction") {

  override fun translate(game: ReadOnlyGameState, arguments: List<TypeExpr>): Instruction {
    val player = arguments.single()
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game, player)
    val lowest = prods.values.min()
    val lowestProds =
        prods.filterValues { it == lowest }.keys.joinToString(" OR ") { "$it<$player>" }
    return instruction("PROD[$lowestProds]")
  }
}

// For Robotic Workforce
private object CopyProductionBox : CustomInstruction("copyProductionBox") {
  override fun translate(game: ReadOnlyGameState, arguments: List<TypeExpr>): Instruction {
    val chosenCardName = arguments.single().className
    val def = game.authority.card(chosenCardName)

    val nodes: Set<Transform> = def.immediateRaw?.let(::childNodesOfType) ?: setOf()
    val matches = nodes.filter { it.transform == "PROD" }

    when (matches.size) {
      1 -> return matches.first()
      0 -> throw RuntimeException("There is no immediate PROD box on $chosenCardName")
      else ->
          throw RuntimeException(
              "The immediate instructions on $chosenCardName " +
                  "have multiple PROD boxes, which should never happen")
    }
  }
}
