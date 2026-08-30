package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal object JsonReader {

  /** Reads one bundle language file keyed by canonical class name. */
  public fun readDisplayNames(json5: String): Map<dev.martianzoo.pets.ast.ClassName, String> =
      fromJson5<Map<String, String>>(json5).mapKeys { (className) -> cn(className) }

  // HELPERS

  private inline fun <reified T : Any> fromJson5(input: String): T = JSON5.decodeFromString(input)

  @OptIn(ExperimentalSerializationApi::class)
  private val JSON5 = Json {
    allowComments = true
    allowTrailingComma = true
    ignoreUnknownKeys = true
    isLenient = true
  }
}
