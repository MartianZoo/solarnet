package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameStateReader
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.ResourceUtils.standardResourceNames
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Expression.Companion.expression
import dev.martianzoo.tfm.repl.TfmColor.ENERGY
import dev.martianzoo.tfm.repl.TfmColor.HEAT
import dev.martianzoo.tfm.repl.TfmColor.MEGACREDIT
import dev.martianzoo.tfm.repl.TfmColor.PLANT
import dev.martianzoo.tfm.repl.TfmColor.STEEL
import dev.martianzoo.tfm.repl.TfmColor.TITANIUM

internal class BoardToText(private val game: GameStateReader) {

  internal fun board(player: Expression, useColors: Boolean = true): List<String> {
    val prodMap = lookUpProductionLevels(game, player)
    val resourceMap =
        standardResourceNames(game).associateBy({ it }) {
          game.count(game.resolve(it.addArgs(player)))
        }

    fun prodAndResource(s: String) =
        prodMap[cn(s)].toString().padStart(2) to resourceMap[cn(s)].toString().padStart(3)

    val (m, mres) = prodAndResource("Megacredit")
    val (s, sres) = prodAndResource("Steel")
    val (t, tres) = prodAndResource("Titanium")
    val (p, pres) = prodAndResource("Plant")
    val (e, eres) = prodAndResource("Energy")
    val (h, hres) = prodAndResource("Heat")

    fun maybeColor(c: TfmColor, s: String) = if (useColors) c.color(s) else s

    val megac = maybeColor(MEGACREDIT, "M: $mres")
    val steel = maybeColor(STEEL, "S: $sres")
    val titan = maybeColor(TITANIUM, "T: $tres")
    val plant = maybeColor(PLANT, "P: $pres")
    val energ = maybeColor(ENERGY, "E: $eres")
    val heeat = maybeColor(HEAT, "H: $hres")

    val tr = game.count(game.resolve(expression("TerraformRating<$player>")))
    val tiles = game.count(game.resolve(expression("OwnedTile<$player>")))
    return listOf(
        "  $player   TR: $tr   Tiles: $tiles",
        "+---------+---------+---------+",
        "|  $megac |  $steel |  $titan |",
        "| prod $m | prod $s | prod $t |",
        "+---------+---------+---------+",
        "|  $plant |  $energ    $heeat |",
        "| prod $p | prod $e | prod $h |",
        "+---------+---------+---------+",
    )
  }
}
