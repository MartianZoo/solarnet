package dev.martianzoo.tfm.petstools

import dev.martianzoo.tfm.canon.MarsMapDefinition
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.readLines
import kotlin.math.hypot
import kotlin.math.roundToInt

// Geometry, colors and icon placement follow the Playground's MarsTab.
internal fun renderMapPng(map: MarsMapDefinition, marsPath: Path, iconsPath: Path, output: Path) {
  val mars = ImageIO.read(marsPath.toFile())
  val atlas = ImageIO.read(iconsPath.resolve("atlas.png").toFile())
  val icons =
      iconsPath
          .resolve("index.tsv")
          .readLines()
          .drop(1)
          .filter { it.isNotBlank() }
          .associate { line ->
            val (name, x, y, width, height) = line.split('\t')
            name to atlas.getSubimage(x.toInt(), y.toInt(), width.toInt(), height.toInt())
          }
  val iconNames =
      mapOf(
          'P' to "Plant",
          'S' to "Steel",
          'T' to "Titanium",
          'C' to "ProjectCard",
          'E' to "Energy",
          'H' to "Heat",
          'D' to "PartyDelegate",
          'R' to "StandardResource",
          'F' to "TemperatureStep",
          'O' to "OceanTile",
          '4' to "MC/MC-4",
          '6' to "MC/MC-6",
      )
  val image = BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB)
  val g = image.createGraphics()
  try {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g.drawImage(mars, 0, 0, 1000, 1000, null)
    val areas = map.areas.toList()
    val xs = areas.map { 107.0 * (it.column - it.row / 2.0) }
    val ys = areas.map { 93.5 * it.row }
    val mx = (xs.min() + xs.max()) / 2
    val my = (ys.min() + ys.max()) / 2
    val scale = 475.0 / areas.indices.maxOf { hypot(xs[it] - mx, ys[it] - my) + 58.0 }
    areas.forEachIndexed { i, area ->
      val x = 500 + (xs[i] - mx) * scale
      val y = 500 + (ys[i] - my) * scale
      val kind = area.kind.toString()
      val water = kind == "WaterArea"
      val noctis = kind == "NoctisArea"
      val stroke = if (water || noctis) 1.92 else 1.32
      val inset = (107 - 2 * 50.2) * scale * 0.1 + stroke * 0.15
      val hexScale = scale * (1 - inset / (50.2 * scale))
      val hex =
          Path2D.Double().apply {
            moveTo(x, y - 58 * hexScale)
            lineTo(x + 50.2 * hexScale, y - 29 * hexScale)
            lineTo(x + 50.2 * hexScale, y + 29 * hexScale)
            lineTo(x, y + 58 * hexScale)
            lineTo(x - 50.2 * hexScale, y + 29 * hexScale)
            lineTo(x - 50.2 * hexScale, y - 29 * hexScale)
            closePath()
          }
      g.color =
          when {
            water -> Color(0x49, 0xB9, 0xF0, 0x21)
            noctis -> Color(0xFF, 0xC9, 0x3B, 0x21)
            else -> Color(255, 255, 255, 6)
          }
      g.fill(hex)
      g.color =
          when {
            water -> Color(0x5B, 0xC8, 0xF5)
            noctis -> Color(0xFF, 0xD5, 0x57)
            else -> Color(0xE1, 0xE7, 0xEF, 224)
          }
      g.stroke = BasicStroke((stroke * 1.5).toFloat())
      g.draw(hex)
      if (kind == "VolcanicArea") {
        val volcano =
            Path2D.Double().apply {
              moveTo(x - 17.2 * scale, y + 29.6 * scale)
              lineTo(x - 6.8 * scale, y + 8.8 * scale)
              lineTo(x - 1.2 * scale, y + 16 * scale)
              lineTo(x + 6.8 * scale, y + 5.6 * scale)
              lineTo(x + 17.2 * scale, y + 29.6 * scale)
              closePath()
            }
        g.color = Color(0x19, 0x13, 0x11, 0xD2)
        g.fill(volcano)
      }
      if (noctis) {
        g.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, (16 * scale).roundToInt())
        g.color = Color(0x22, 0x1D, 0x12)
        listOf("Noctis", "City").forEachIndexed { line, text ->
          val width = g.fontMetrics.stringWidth(text)
          g.drawString(text, (x - width / 2).toFloat(), (y + (18 + line * 17) * scale).toFloat())
        }
      }
      val names =
          area.expandedBonusCodes.flatMap { symbol ->
            when (symbol) {
              'X' -> listOf("Colony", "MC/MC-5")
              'V' -> listOf("VenusStep", "MC/MC-5")
              else -> listOf(iconNames.getValue(symbol))
            }
          }
      names.forEachIndexed { n, name ->
        val icon = icons.getValue(name)
        val size = 25 * scale
        val factor = minOf(size / icon.width, size / icon.height)
        val width = (icon.width * factor).roundToInt().coerceAtLeast(1)
        val height = (icon.height * factor).roundToInt().coerceAtLeast(1)
        val left = x + (-45.7 + n * 25) * scale + (size - width) / 2
        val top = y - 23 * scale + (size - height) / 2
        g.drawImage(icon, left.roundToInt(), top.roundToInt(), width, height, null)
      }
    }
  } finally {
    g.dispose()
  }
  check(ImageIO.write(image, "png", output.toFile()))
}
