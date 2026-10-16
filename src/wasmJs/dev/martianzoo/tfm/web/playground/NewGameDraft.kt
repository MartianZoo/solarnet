package dev.martianzoo.tfm.web.playground

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon

/** The dialog's editable input for a solo game; only [config] is sent to premise resolution. */
internal class NewGameDraft {
  val entries =
      mutableStateListOf(
          "TerraformingMars",
          "CorporateEraExpansion",
          "PreludeExpansion",
          "VenusNextExpansion",
          "promos:cards",
          "TharsisMap",
          "SoloMode",
      )

  /** The player's class name. A color word such as `Green` also colors the player. */
  var playerName by mutableStateOf("Green")

  var seedText by mutableStateOf("")

  private var checkedDraft: Pair<List<String>, String>? = null
  private val legalChoices = mutableMapOf<List<String>, Boolean>()

  fun config(proposedEntries: List<String> = entries): GameConfig {
    val requested = GameConfig(proposedEntries.joinToString("\n"), playerName)
    val resolved = Canon.gamePremise(requested)
    require(cn("SoloMode") in resolved.modules) { "Play requires SoloMode." }
    require(resolved.playerNames.size == 1) { "Play requires a single player." }
    val excluded =
        listOf("SearchForLife", "AsteroidDeflectionSystem").map(::cn).filter {
          resolved.classTable.isInhabited(resolved.classTable.getClass(it))
        }
    return requested.copy(
        includedClassNames = requested.includedClassNames - excluded.toSet(),
        excludedClassNames = requested.excludedClassNames + excluded,
    )
  }

  /** Cache only verdicts for this draft, not a class table for every possible menu choice. */
  fun accepts(proposedEntries: List<String>): Boolean {
    val key = entries.toList() to playerName
    if (checkedDraft != key) {
      legalChoices.clear()
      checkedDraft = key
    }
    return legalChoices.getOrPut(proposedEntries.toList()) {
      runCatching {
        // Apply the dealer's card exclusions to the chosen draft, not to every hypothetical choice.
        val proposed = Canon.gamePremise(GameConfig(proposedEntries.joinToString("\n"), playerName))
        require(cn("SoloMode") in proposed.modules && proposed.playerNames.size == 1)
        proposed.classTable
      }
          .isSuccess
    }
  }

  fun add(entry: String) {
    if (entry !in entries) entries.add(entry)
  }

  fun remove(entry: String) {
    entries.remove(entry)
  }

  fun chooseMap(name: String) {
    val maps = Canon.marsMapDefinitions.mapTo(hashSetOf()) { it.className.toString() }
    val old = entries.indexOfFirst { it in maps }
    entries.removeAll(maps)
    entries.add(if (old >= 0) old.coerceAtMost(entries.size) else entries.size, name)
  }
}
