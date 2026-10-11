package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal object JsonReader {

  /** Reads one bundle language file keyed by canonical class name. */
  public fun readDisplayNames(json5: String): Map<dev.martianzoo.pets.ast.ClassName, String> =
      fromJson5<Map<String, String>>(json5).mapKeys { (className) -> cn(className) }

  /** Reads replacement Content names, keyed by the replacement item. */
  public fun readReplacements(
      json5: String
  ): Map<dev.martianzoo.pets.ast.ClassName, dev.martianzoo.pets.ast.ClassName> =
      fromJson5<Map<String, String>>(json5)
          .mapKeys { (replacement) -> cn(replacement) }
          .mapValues { (_, replaced) -> cn(replaced) }

  /** Reads cards omitted only from automatic bundle pool selection. */
  public fun readExcludedDefaultCards(json5: String): Set<dev.martianzoo.pets.ast.ClassName> =
      fromJson5<List<String>>(json5).mapTo(linkedSetOf(), ::cn)

  // HELPERS

  private inline fun <reified T : Any> fromJson5(input: String): T = JSON5.decodeFromString(input)

  private val JSON5 = Json {
    allowComments = true
    allowTrailingComma = true
    ignoreUnknownKeys = true
    isLenient = true
  }
}
