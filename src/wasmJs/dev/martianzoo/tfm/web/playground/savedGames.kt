@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.martianzoo.tfm.web.playground

import kotlin.js.JsString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class SavedGameSetup(
    val entries: List<String>,
    val playerName: String,
    val seed: Int,
) {
  val label: String
    get() =
        "1P - " +
            entries.joinToString(",") { entry ->
              entry
                  .filter { it in 'A'..'Z' || it in '0'..'9' }
                  .ifEmpty {
                    entry
                        .split(':', '-')
                        .filter(String::isNotEmpty)
                        .map { it.first().uppercaseChar() }
                        .joinToString("")
                  }
            } +
            " - ${formatPlaySeed(seed)}"
}

@JsFun("() => globalThis.localStorage.getItem('mars-playground-saved-games-v1')")
private external fun readSavedGames(): JsString?

@JsFun("(text) => globalThis.localStorage.setItem('mars-playground-saved-games-v1', text)")
private external fun writeSavedGames(text: String)

internal fun savedGameSetups(): List<SavedGameSetup> =
    readSavedGames()
        ?.toString()
        ?.let {
          runCatching { Json.decodeFromString<List<SavedGameSetup>>(it) }.getOrDefault(emptyList())
        }
        .orEmpty()

internal fun rememberGameSetup(setup: SavedGameSetup): List<SavedGameSetup> {
  val saved = listOf(setup) + savedGameSetups().filterNot { it == setup }
  writeSavedGames(Json.encodeToString(saved))
  return saved
}
