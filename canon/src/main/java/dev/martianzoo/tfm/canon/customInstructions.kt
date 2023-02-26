package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.ReadOnlyGameState
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.childNodesOfType

internal val allCustomInstructions =
    setOf(
        GainLowestProduction,
        CopyProductionBox,
    )

// For Robinson Industries
private object GainLowestProduction : CustomInstruction("gainLowestProduction") {

  override fun translate(game: ReadOnlyGameState, arguments: List<Type>): Instruction {
    val player = arguments.single()
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game, player.typeExpr)
    val lowest = prods.values.min()
    val lowestProds =
        prods.filterValues { it == lowest }.keys.joinToString(" OR ") { "$it<$player>" }
    return instruction("PROD[$lowestProds]")
  }
}

// For Robotic Workforce
private object CopyProductionBox : CustomInstruction("copyProductionBox") {
  override fun translate(game: ReadOnlyGameState, arguments: List<Type>): Instruction {
    val chosenCardName = arguments.single().typeExpr.className
    val def = game.setup.authority.card(chosenCardName)

    val nodes: Set<Transform> = def.immediateRaw?.let(::childNodesOfType) ?: setOf()
    val matches = nodes.filter { it.transformKind == "PROD" }

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
