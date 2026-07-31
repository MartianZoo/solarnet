package dev.martianzoo.script

import dev.martianzoo.util.toSetStrict

/** Keeps the REPL's legacy one-letter game-option syntax out of Canon and the engine API. */
internal object OptionCodeTranslation {
  data class Setup(val optionCodes: String, val players: Int, val instruction: String)

  fun setup(optionCodes: String, players: Int): Setup {
    require(players in 1..5) { "player count must be between 1 and 5" }
    val effectiveCodes = if (players == 1 && 'S' !in optionCodes) optionCodes + "S" else optionCodes
    val codes = effectiveCodes.asIterable().map(Char::toString).toSetStrict()
    require(optionsByCode.containsAll(codes)) {
      "supported option codes are: $optionsByCode"
    }
    val selectedMaps = codes.intersect(mapInstructions.keys)
    require(selectedMaps.size == 1) { "select exactly one map: ${mapInstructions.keys}" }

    val instructions = buildList {
      add("$players Player")
      if ("R" !in codes) add("-CorporateEraExpansion")
      mapInstructions.getValue(selectedMaps.single())?.let(::add)
      codes.mapNotNullTo(this) { positiveOptionInstructions[it] }
      if ("C" in codes) add("DeferredColonySelection")
    }
    return Setup(effectiveCodes, players, instructions.joinToString(", "))
  }

  fun suggestions(current: Setup): List<String> {
    val nonMaps = optionsByCode - mapInstructions.keys
    val common = listOf("M", "RM", "RMVX", "RMVPX", "RMVPXT", current.optionCodes)
    val generated = mapInstructions.keys.flatMap { map -> nonMaps.map { "$it$map" } }
    return common + generated
  }

  private val optionsByCode =
      linkedSetOf("S", "R", "M", "H", "E", "I", "U", "V", "P", "C", "T", "X")

  private val mapInstructions =
      linkedMapOf(
          "M" to null,
          "H" to "HellasMapOption FROM TharsisMapOption",
          "E" to "ElysiumMapOption FROM TharsisMapOption",
          "I" to "TerraCimmeriaMapOption FROM TharsisMapOption",
          "U" to "UtopiaPlanitiaMapOption FROM TharsisMapOption",
      )

  private val positiveOptionInstructions =
      mapOf(
          "S" to "SoloMode",
          "V" to "VenusNextExpansion",
          "P" to "PreludeExpansion",
          "C" to "ColoniesExpansion",
          "T" to "TurmoilCardPack",
          "X" to "PromoCardPack",
      )
}
