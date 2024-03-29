package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.tfm.api.ApiUtils
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.repl.TfmColor
import dev.martianzoo.tfm.repl.TfmColor.ENERGY
import dev.martianzoo.tfm.repl.TfmColor.HEAT
import dev.martianzoo.tfm.repl.TfmColor.MEGACREDIT
import dev.martianzoo.tfm.repl.TfmColor.PLANT
import dev.martianzoo.tfm.repl.TfmColor.STEEL
import dev.martianzoo.tfm.repl.TfmColor.TITANIUM

internal class TfmBoardCommand(repl: ReplSession) : AbstractTfmCommand(repl, "tfm_board") {
  override val usage = "tfm_board [PlayerN]"
  override val help =
      """
        Shows a crappy player board for the named player, or the current player by default.
      """
  override val isReadOnly = true

  override fun noArgs(): List<String> = PlayerBoardToText(tfm(), repl.jline != null).board()

  override fun withArgs(args: String) =
      PlayerBoardToText(tfm().asPlayer(repl.player(args)), repl.jline != null).board()

  internal class PlayerBoardToText(
      private val tfm: TfmGameplay,
      private val useColors: Boolean = true,
  ) {

    internal fun board(): List<String> {
      val prodMap = ApiUtils.lookUpProductionLevels(tfm.reader, tfm.player)
      val resourceMap =
          ApiUtils.standardResourceNames(tfm.reader).associateBy({ it }) {
            tfm.reader.count(Count(it.of(tfm.player.className)))
          }

      fun prodAndResource(s: String) =
          prodMap[ClassName.cn(s)].toString() to resourceMap[ClassName.cn(s)].toString().padStart(3)

      val (mp, mres) = prodAndResource("Megacredit")
      val (sp, sres) = prodAndResource("Steel")
      val (tp, tres) = prodAndResource("Titanium")
      val (pp, pres) = prodAndResource("Plant")
      val (ep, eres) = prodAndResource("Energy")
      val (hp, hres) = prodAndResource("Heat")

      val m = mp.padStart(2)
      val s = sp.padStart(2)
      val t = tp.padStart(2)
      val p = pp.padStart(2)
      val e = ep.padStart(2)
      val h = hp.padStart(2)

      fun maybeColor(c: TfmColor, s: String) = if (useColors) c.foreground(s) else s

      val megac = maybeColor(MEGACREDIT, "M: $mres")
      val steel = maybeColor(STEEL, "S: $sres")
      val titan = maybeColor(TITANIUM, "T: $tres")
      val plant = maybeColor(PLANT, "P: $pres")
      val energ = maybeColor(ENERGY, "E: $eres")
      val heeat = maybeColor(HEAT, "H: $hres")

      val r = tfm.count("TerraformRating")
      val tiles = tfm.count("OwnedTile")
      val player = "${tfm.player}"

      return """
          $player   TR: $r   Tiles: $tiles
        +---------+---------+---------+
        |  $megac |  $steel |  $titan |
        | prod $m | prod $s | prod $t |
        +---------+---------+---------+
        |  $plant |  $energ    $heeat |
        | prod $p | prod $e | prod $h |
        +---------+---------+---------+
    """
          .trimIndent()
          .split("\n")
    }
  }
}
