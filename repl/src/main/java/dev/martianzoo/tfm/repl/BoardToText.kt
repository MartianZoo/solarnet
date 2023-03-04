package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.ReadOnlyGameState
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.ResourceUtils.standardResourceNames
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle.DEFAULT

internal class BoardToText(private val game: ReadOnlyGameState) {

  internal fun board(player: Expression, colors: Boolean = true): List<String> {
    val prodMap = lookUpProductionLevels(game, player)
    val resourceMap =
        standardResourceNames(game).associateBy({ it }) {
          game.count(game.resolve(it.addArgs(player)))
        }

    fun prodAndResource(s: String) =
        prodMap[cn(s)].toString().padStart(2) to resourceMap[cn(s)].toString().padStart(3)

    val (m, mr) = prodAndResource("Megacredit")
    val (s, sr) = prodAndResource("Steel")
    val (t, tr) = prodAndResource("Titanium")
    val (p, pr) = prodAndResource("Plant")
    val (e, er) = prodAndResource("Energy")
    val (h, hr) = prodAndResource("Heat")

    fun colorIt(color: String, string: String): String? {
      if (!colors) return string
      val r = color.substring(0, 2).toInt(16)
      val g = color.substring(2, 4).toInt(16)
      val b = color.substring(4, 6).toInt(16)
      return AttributedStringBuilder()
          .style(DEFAULT.foreground(r, g, b))
          .append(string)
          .style(DEFAULT)
          .toAnsi()
    }

    val megac = colorIt(mColor, "M: $mr")
    val steel = colorIt(sColor, "S: $sr")
    val titan = colorIt(tColor, "T: $tr")
    val plant = colorIt(pColor, "P: $pr")
    val energ = colorIt(eColor, "E: $er")
    val heeat = colorIt(hColor, "H: $hr")

    return listOf(
        "+---------+---------+---------+",
        "|  $megac |  $steel |  $titan |",
        "| prod $m | prod $s | prod $t |",
        "+---------+---------+---------+",
        "|  $plant |  $energ    $heeat |",
        "| prod $p | prod $e | prod $h |",
        "+---------+---------+---------+",
    )
  }

  internal val mColor = "f4d400"
  internal val sColor = "c8621e"
  internal val tColor = "777777"
  internal val pColor = "6dd248"
  internal val eColor = "b23bcb"
  internal val hColor = "ef4320"
}
