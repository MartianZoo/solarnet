package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.ReadOnlyGameState
import dev.martianzoo.tfm.api.lookUpProductionLevels
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser.parsePets
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.onlyElement

val allCustomInstructions = listOf(
    GainLowestProduction
)

// For Robinson Industries
object GainLowestProduction : CustomInstruction("gainLowestProduction") {

  override fun translate(game: ReadOnlyGameState, arguments: List<TypeExpression>): Instruction {
    val player = arguments.onlyElement()
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game, player)
    val lowest = prods.values.min()
    val lowestProds = prods.filterValues { it == lowest }
        .keys
        .map { "$it<$player>" }
        .joinToString(" OR ")
    return parsePets("PROD[$lowestProds]")
  }
}

// For Robotic Workforce
object CopyProductionBox : CustomInstruction("copyProductionBox") {
  override fun translate(game: ReadOnlyGameState, arguments: List<TypeExpression>): Instruction {
    val chosenCardName = arguments.onlyElement().className
    val def = game.authority.cardsByClassName[chosenCardName]!!
    val matches = def.immediateRaw
        ?.childNodesOfType<Transform>()
        ?.filter { it.transform == "PROD" }
        ?: listOf()
    when (matches.size) {
      1 -> return matches.first()
      0 -> throw PetException("There is no immediate PROD box on $chosenCardName")
      else -> throw PetException("The immediate instructions on $chosenCardName " +
          "have multiple PROD boxes, which should never happen")
    }
  }
}
