package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.ReadOnlyGameState
import dev.martianzoo.tfm.api.lookUpProductionLevels
import dev.martianzoo.tfm.api.standardResourceNames
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpr

internal class BoardToText(private val game: GameState) { // TODO why not Game??

  internal fun board(player: TypeExpr): List<String> {
    val prodMap = lookUpProductionLevels(game, player)
    val resMap = lookUpResourceLevels(game, player)

    fun prodAndRes(s: String) =
        prodMap[cn(s)].toString().padStart(2) to resMap[cn(s)].toString().padStart(3)

    val (m, mr) = prodAndRes("Megacredit")
    val (s, sr) = prodAndRes("Steel")
    val (t, tr) = prodAndRes("Titanium")
    val (p, pr) = prodAndRes("Plant")
    val (e, er) = prodAndRes("Energy")
    val (h, hr) = prodAndRes("Heat")

    return listOf(
        "+---------+---------+---------+",
        "|  M: $mr |  S: $sr |  T: $tr |",
        "| prod $m | prod $s | prod $t |",
        "+---------+---------+---------+",
        "|  P: $pr |  E: $er    H: $hr |",
        "| prod $p | prod $e | prod $h |",
        "+---------+---------+---------+",
    )
  }

  private fun lookUpResourceLevels(game: ReadOnlyGameState, player: TypeExpr) =
      standardResourceNames(game).associateBy({ it }) { game.countComponents(it.addArgs(player)) }
}
