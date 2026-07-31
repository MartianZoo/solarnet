package dev.martianzoo.script

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.GameOptions
import dev.martianzoo.util.toSetStrict

/** Keeps the REPL's legacy one-letter game-option syntax out of Canon and the engine API. */
internal object OptionCodeTranslation {
  fun options(optionCodes: String, players: Int): GameOptions {
    val effectiveCodes = if (players == 1 && 'S' !in optionCodes) optionCodes + "S" else optionCodes
    val codes = effectiveCodes.asIterable().map(Char::toString).toSetStrict()
    require(optionsByCode.keys.containsAll(codes)) {
      "supported option codes are: ${optionsByCode.keys}"
    }
    return GameOptions(players, codes.mapTo(linkedSetOf()) { optionsByCode.getValue(it) })
  }

  fun optionCodes(options: GameOptions): String =
      options.enabled.mapNotNull(codesByOption::get).joinToString("")

  fun suggestions(current: GameOptions): List<String> {
    val maps = setOf("E", "H", "I", "M")
    val nonMaps = optionsByCode.keys - maps
    val common = listOf("BM", "BRM", "BRMVX", "BRMVPX", "BRMVPXT", optionCodes(current))
    val generated = maps.flatMap { map -> nonMaps.map { "$it$map" } }
    return common + generated
  }

  private val optionsByCode: Map<String, ClassName> =
      linkedMapOf(
          "B" to cn("TerraformingMars"),
          "S" to cn("SoloMode"),
          "R" to cn("CorporateEraExpansion"),
          "M" to cn("TharsisMapOption"),
          "H" to cn("HellasMapOption"),
          "E" to cn("ElysiumMapOption"),
          "I" to cn("TerraCimmeriaMapOption"),
          "V" to cn("VenusNextExpansion"),
          "P" to cn("PreludeExpansion"),
          "C" to cn("ColoniesExpansion"),
          "T" to cn("TurmoilCardPack"),
          "X" to cn("PromoCardPack"),
      )

  private val codesByOption = optionsByCode.entries.associate { (code, option) -> option to code }
}
