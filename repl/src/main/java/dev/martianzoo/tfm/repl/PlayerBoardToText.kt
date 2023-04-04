package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.ResourceUtils.standardResourceNames
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.repl.TfmColor.ENERGY
import dev.martianzoo.tfm.repl.TfmColor.HEAT
import dev.martianzoo.tfm.repl.TfmColor.MEGACREDIT
import dev.martianzoo.tfm.repl.TfmColor.PLANT
import dev.martianzoo.tfm.repl.TfmColor.PRODUCTION
import dev.martianzoo.tfm.repl.TfmColor.STEEL
import dev.martianzoo.tfm.repl.TfmColor.TITANIUM

internal class PlayerBoardToText(
    private val session: InteractiveSession,
    val useColors: Boolean = true
) {

  internal fun board(): List<String> {
    require(session.player != Player.ENGINE)
    val prodMap = lookUpProductionLevels(session.agent.reader, session.player)
    val resourceMap =
        standardResourceNames(session.agent.reader).associateBy({ it }) {
          session.count(Count(it.addArgs(session.player.className)))
        }

    fun prodAndResource(s: String) =
        prodMap[cn(s)].toString() to resourceMap[cn(s)].toString().padStart(3)

    val (mp, mres) = prodAndResource("Megacredit")
    val (sp, sres) = prodAndResource("Steel")
    val (tp, tres) = prodAndResource("Titanium")
    val (pp, pres) = prodAndResource("Plant")
    val (ep, eres) = prodAndResource("Energy")
    val (hp, hres) = prodAndResource("Heat")

    val m = prod(mp)
    val s = prod(sp)
    val t = prod(tp)
    val p = prod(pp)
    val e = prod(ep)
    val h = prod(hp)

    fun maybeColor(c: TfmColor, s: String) = if (useColors) c.foreground(s) else s

    val megac = maybeColor(MEGACREDIT, "M: $mres")
    val steel = maybeColor(STEEL, "S: $sres")
    val titan = maybeColor(TITANIUM, "T: $tres")
    val plant = maybeColor(PLANT, "P: $pres")
    val energ = maybeColor(ENERGY, "E: $eres")
    val heeat = maybeColor(HEAT, "H: $hres")

    // TODO this Raw business is a mess
    val r = session.count("TerraformRating")
    val tiles = session.count("OwnedTile")
    val player: String = "${session.player}"

    return """
          $player   TR: $r   Tiles: $tiles
        +---------+---------+---------+
        |  $megac |  $steel |  $titan |
        | prod $m | prod $s | prod $t |
        +---------+---------+---------+
        |  $plant |  $energ    $heeat |
        | prod $p | prod $e | prod $h |
        +---------+---------+---------+
    """.trimIndent().split("\n")
  }

  private fun prod(num: String): String {
    val prefix = " ".repeat(2 - num.length)
    return prefix + if (false) PRODUCTION.background(num) else num
  }
}