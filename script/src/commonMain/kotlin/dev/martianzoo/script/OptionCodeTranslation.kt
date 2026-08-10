package dev.martianzoo.script

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.canon.Canon.Option
import dev.martianzoo.util.toSetStrict

/** Keeps the REPL's legacy one-letter game-option syntax out of Canon and the engine API. */
internal object OptionCodeTranslation {
  data class Setup(
      val optionCodes: String,
      val players: Int,
      val options: Set<Option>,
      val excludedOptions: Set<Option>,
      val selectedColonies: Set<ClassName>,
  )

  fun setup(
      optionCodes: String,
      players: Int,
      selectedColonies: Set<ClassName> = emptySet(),
  ): Setup {
    require(players in 1..5) { "player count must be between 1 and 5" }
    val codes = optionCodes.asIterable().map(Char::toString).toSetStrict()
    require(optionsByCode.containsAll(codes)) {
      "supported option codes are: $optionsByCode"
    }
    require("B" in codes) { "include B for the base game" }
    val selectedMaps = codes.intersect(mapOptions.keys)
    require(selectedMaps.size == 1) { "select exactly one map: ${mapOptions.keys}" }

    val options = buildSet {
      add(Option.TerraformingMars)
      add(mapOptions.getValue(selectedMaps.single()))
      codes.mapNotNullTo(this) { positiveOptions[it] }
    }
    val excludedOptions =
        if (Option.CorporateEraExpansion in options) emptySet()
        else setOf(Option.CorporateEraExpansion)
    return Setup(optionCodes, players, options, excludedOptions, selectedColonies)
  }

  fun suggestions(current: Setup): List<String> {
    val nonMaps = optionsByCode - mapOptions.keys - "B"
    val common = listOf("BM", "BRM", "BRMVX", "BRMVPX", "BRMVPXT", current.optionCodes)
    val generated = mapOptions.keys.flatMap { map -> nonMaps.map { "B$it$map" } }
    return common + generated
  }

  fun optionCodes(options: Set<Option>): String =
      optionByCode.entries
          .filter { it.value in options }
          .joinToString(separator = "") { (code) -> code }

  private val mapOptions =
      linkedMapOf(
          "M" to Option.TharsisMapOption,
          "H" to Option.HellasMapOption,
          "E" to Option.ElysiumMapOption,
          "I" to Option.TerraCimmeriaMapOption,
          "U" to Option.UtopiaPlanitiaMapOption,
      )

  private val positiveOptions =
      mapOf(
          "R" to Option.CorporateEraExpansion,
          "V" to Option.VenusNextExpansion,
          "P" to Option.PreludeExpansion,
          "C" to Option.ColoniesExpansion,
          "T" to Option.TurmoilCardPack,
          "X" to Option.PromoCardPack,
      )

  private val optionsByCode =
      linkedSetOf("B", "R", "M", "H", "E", "I", "U", "V", "P", "C", "T", "X")

  private val optionByCode = optionsByCode.associateWith { code ->
    when (code) {
      "B" -> Option.TerraformingMars
      else -> mapOptions[code] ?: positiveOptions.getValue(code)
    }
  }
}
