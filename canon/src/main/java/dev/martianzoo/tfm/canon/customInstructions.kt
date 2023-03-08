package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.GameStateReader
import dev.martianzoo.tfm.api.GameStateWriter
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.util.filterWithoutNulls

internal val allCustomInstructions =
    setOf(
        ForceLoad,
        CreateAll,
        GainLowestProduction,
        CopyProductionBox,
        CopyPrelude,
    )

private object ForceLoad : CustomInstruction("forceLoad") { // TODO include @ ?
  override fun execute(game: GameStateReader, writer: GameStateWriter, arguments: List<Type>) {
    // This one legitimately doesn't have to do anything!
  }
}

// Used like `@createAll(This, Border)` instead of `@createBorders(This)` just because
// that forces the Border class to be loaded!
private object CreateAll : CustomInstruction("createAll") {
  override fun translate(game: GameStateReader, arguments: List<Type>): Instruction {
    val (mapClass, toCreate) = arguments
    val map = game.authority.marsMap(mapClass.className)
    val border = cn("Border")
    return when (toCreate.className) {
      border -> {
        Multi(map.areas
            .let { it.rows() + it.columns() + it.diagonals() }
            .flatMap { it.windowed(2).filterWithoutNulls() }
            .map { pair -> pair.map { it.className.expr } }
            .flatMap { (area1, area2) ->
              listOf(
                  border.addArgs(area1, area2),
                  border.addArgs(area2, area1)
              )
            }
            .map { Gain(scaledEx(it)) })
      }
      else -> TODO()
    }
  }
}

// For Robinson Industries
private object GainLowestProduction : CustomInstruction("gainLowestProduction") {

  override fun translate(game: GameStateReader, arguments: List<Type>): Instruction {
    val player = arguments.single()
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game, player.expression)
    val lowest: Int = prods.values.min()
    val keys: Set<ClassName> = prods.filterValues { it == lowest }.keys
    val lowestProds = keys.joinToString(" OR ") { "$it<${player.expression}>" }
    return instruction("PROD[$lowestProds]")
  }
}

// For Robotic Workforce
private object CopyProductionBox : CustomInstruction("copyProductionBox") {
  override fun translate(game: GameStateReader, arguments: List<Type>): Instruction {
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
  override fun execute(game: GameStateReader, writer: GameStateWriter, arguments: List<Type>) {
    TODO()
  }
}
