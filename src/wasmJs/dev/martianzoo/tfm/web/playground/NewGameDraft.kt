package dev.martianzoo.tfm.web.playground

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon

/** The dialog's editable input for a solo game. */
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

  fun resolve(proposedEntries: List<String> = entries): Pair<GameConfig, GamePremise> {
    val requested = GameConfig(proposedEntries.joinToString("\n"), playerName)
    val resolved = Canon.gamePremise(requested)
    require(cn("SoloMode") in resolved.modules) { "Play requires SoloMode." }
    require(resolved.playerNames.size == 1) { "Play requires a single player." }
    val excluded =
        listOf("SearchForLife", "AsteroidDeflectionSystem").map(::cn).filter {
          resolved.classTable.isInhabited(resolved.classTable.getClass(it))
        }
    val config =
        requested.copy(
            includedClassNames = requested.includedClassNames - excluded.toSet(),
            excludedClassNames = requested.excludedClassNames + excluded,
        )
    return config to Canon.gamePremise(config).also { it.classTable }
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
