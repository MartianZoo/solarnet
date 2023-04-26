package dev.martianzoo.tfm.repl

import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

enum class TfmColor(val hexString: String) {
  MEGACREDIT("f4d400"),
  STEEL("c8621e"),
  TITANIUM("777777"),
  PLANT("6dd248"),
  ENERGY("b23bcb"),
  HEAT("ef4320"),
  LAND_AREA("f68e5a"),
  VOLCANIC_AREA("f68e5a"), // TODO pick new color
  WATER_AREA("a4dcf9"),
  OCEAN_TILE("0e3f68"),
  GREENERY_TILE("6dd248"),
  CITY_TILE("999999"),
  SPECIAL_TILE("a87a58"),
  TERRAFORM_RATING("ea8845"),
  NOCTIS_AREA("dddddd"),
  PRODUCTION("794b2c"),
  NONE("cccccc"), // TODO ???
  ;

  fun foreground(string: String): String {
    val r = hexString.substring(0, 2).toInt(16)
    val g = hexString.substring(2, 4).toInt(16)
    val b = hexString.substring(4, 6).toInt(16)
    return AttributedStringBuilder()
        .style(AttributedStyle.DEFAULT.foreground(r, g, b))
        .append(string)
        .style(AttributedStyle.DEFAULT)
        .toAnsi()
  }
  fun background(string: String): String {
    val r = hexString.substring(0, 2).toInt(16)
    val g = hexString.substring(2, 4).toInt(16)
    val b = hexString.substring(4, 6).toInt(16)
    return AttributedStringBuilder()
        .style(AttributedStyle.DEFAULT.background(r, g, b))
        .append(string)
        .style(AttributedStyle.DEFAULT)
        .toAnsi()
  }
}
