package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.ReadOnlyGameState
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Transform

internal val allCustomInstructions =
    setOf(
        ForceLoad,
        CreateSingletons,
        CreateAll,
        GainLowestProduction,
        CopyProductionBox,
        CopyPrelude,
    )

private object ForceLoad : CustomInstruction("forceLoad") { // TODO include @ ?
  override fun execute(game: GameState, arguments: List<Type>) {
    // This one legitimately doesn't have to do anything!
  }
}

private object CreateSingletons : CustomInstruction("createSingletons") {
  override fun execute(game: GameState, arguments: List<Type>) {
    TODO()
  }
}

private object CreateAll : CustomInstruction("createAll") {
  override fun execute(game: GameState, arguments: List<Type>) {
    TODO()
  }
}

// For Robinson Industries
private object GainLowestProduction : CustomInstruction("gainLowestProduction") {

  override fun translate(game: ReadOnlyGameState, arguments: List<Type>): Instruction {
    val player = arguments.single()
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game, player.expression)
    val lowest = prods.values.min()
    val lowestProds =
        prods.filterValues { it == lowest }.keys.joinToString(" OR ") { "$it<$player>" }
    return instruction("PROD[$lowestProds]")
  }
}

// For Robotic Workforce
private object CopyProductionBox : CustomInstruction("copyProductionBox") {
  override fun translate(game: ReadOnlyGameState, arguments: List<Type>): Instruction {
    val chosenCardName = arguments.single().expression.className
    val def = game.authority.card(chosenCardName)

    val nodes: Set<Transform> = def.immediate?.descendantsOfType() ?: setOf()
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

private object CopyPrelude : CustomInstruction("copyPrelude") {
  override fun execute(game: GameState, arguments: List<Type>) {
    TODO()
  }
}
