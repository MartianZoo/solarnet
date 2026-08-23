package dev.martianzoo.script

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.util.toSetStrict

/** Keeps the REPL's legacy one-letter game-option syntax out of Canon and the engine API. */
internal object OptionCodeTranslation {
  data class Setup(
      val optionCodes: String,
      val players: Int,
      val options: Set<ClassName>,
      val excludedOptions: Set<ClassName>,
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
    val selectedMapBundles = codes.intersect(bundleDefaultMapOptions.keys)
    require(selectedMapBundles.size <= 1) {
      "select at most one map bundle: ${bundleDefaultMapOptions.keys}"
    }

    val options = buildSet {
      add(TERRAFORMING_MARS)
      add(selectedMapBundles.singleOrNull()?.let(bundleDefaultMapOptions::getValue) ?: THARSIS_MAP)
      codes.mapNotNullTo(this) { positiveOptions[it] }
    }
    val excludedOptions = if (CORPORATE_ERA in options) emptySet() else setOf(CORPORATE_ERA)
    return Setup(optionCodes, players, options, excludedOptions, selectedColonies)
  }

  fun suggestions(currentOptionCodes: String): List<String> {
    val nonMaps = optionsByCode - bundleDefaultMapOptions.keys - "B"
    val common = listOf("B", "BR", "BRVX", "BRVPX", "BRVPXT", currentOptionCodes)
    val generated = bundleDefaultMapOptions.keys.flatMap { map -> nonMaps.map { "B$it$map" } }
    return common + generated
  }

  fun optionCodes(options: Set<ClassName>): String =
      optionByCode.entries
          .filter { it.value in options }
          .joinToString(separator = "") { (code) -> code }

  fun recognizedOptions(classNames: Set<ClassName>): Set<ClassName> =
      optionByCode.values.filterTo(linkedSetOf()) { it in classNames }

  private val TERRAFORMING_MARS = cn("TerraformingMars")
  private val CORPORATE_ERA = cn("CorporateEraExpansion")
  private val THARSIS_MAP = cn("TharsisMap")

  private val bundleDefaultMapOptions =
      linkedMapOf(
          "H" to cn("HellasMap"),
          "U" to cn("UtopiaMap"),
      )

  private val positiveOptions =
      mapOf(
          "R" to CORPORATE_ERA,
          "V" to cn("VenusNextExpansion"),
          "P" to cn("PreludeExpansion"),
          "C" to cn("ColoniesExpansion"),
          "T" to cn("TurmoilCardPack"),
          "X" to cn("PromoCardPack"),
      )

  private val optionsByCode = linkedSetOf("B", "R", "H", "U", "V", "P", "C", "T", "X")

  private val optionByCode = optionsByCode.associateWith { code ->
    when (code) {
      "B" -> TERRAFORMING_MARS
      else -> bundleDefaultMapOptions[code] ?: positiveOptions.getValue(code)
    }
  }
}
