package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.ReadOnlyGameState
import dev.martianzoo.tfm.api.lookUpProductionLevels
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.onlyElement

val allCustomInstructions = listOf(
    GainLowestProduction
)

// For Robinson Industries
object GainLowestProduction : CustomInstruction("gainLowestProduction") {

  override fun translate(game: ReadOnlyGameState, arguments: List<TypeExpression>): Instruction {
    val player = arguments.onlyElement()
    val prods: Map<String, Int> = lookUpProductionLevels(game, player)
    val lowest = prods.values.min()
    val lowestProds = prods.filterValues { it == lowest }
        .keys
        .map { "$it<$player>" }
        .joinToString(" OR ")
    return parse("PROD[$lowestProds]")
  }
}
