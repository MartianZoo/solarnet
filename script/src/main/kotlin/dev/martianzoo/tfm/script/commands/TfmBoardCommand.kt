package dev.martianzoo.tfm.script.commands

import dev.martianzoo.data.Player
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.tfm.api.ApiUtils
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.script.TfmColor
import dev.martianzoo.tfm.script.TfmColor.ENERGY
import dev.martianzoo.tfm.script.TfmColor.HEAT
import dev.martianzoo.tfm.script.TfmColor.MEGACREDIT
import dev.martianzoo.tfm.script.TfmColor.PLANT
import dev.martianzoo.tfm.script.TfmColor.STEEL
import dev.martianzoo.tfm.script.TfmColor.TITANIUM

internal class TfmBoardCommand(repl: ScriptSession) : AbstractTfmCommand(repl, "tfm_board") {
  override val usage = "tfm_board [PlayerN]"
  override val help =
      """
        Shows a crappy player board for the named player, or the current player by default.
      """
  override val isReadOnly = true

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.playerNames(includeEngine = false)

  override fun noArgs(): List<String> = PlayerBoardToText(tfm(), useColors = false).board()

  override fun withArgs(args: String) =
      PlayerBoardToText(tfm().asPlayer(repl.player(args)), useColors = false).board()

  internal class PlayerBoardToText(
      private val tfm: TfmGameplay,
      private val useColors: Boolean = true,
  ) {

    internal fun board(): List<String> {
      val player = tfm.actor as? Player ?: error("a player board requires a Player actor")
      val prodMap = ApiUtils.lookUpProductionLevels(tfm.reader, player)
      val resourceMap =
          ApiUtils.standardResourceNames(tfm.reader).associateBy({ it }) {
            tfm.reader.count(Count(it.of(player.className)))
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
