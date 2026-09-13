package dev.martianzoo.tfm.carddata

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Card definitions embedded for data consumers and Pets generation. */
public object CardData {
  public val bundleNames: Set<String>
    get() =
        GeneratedCardDataResources.filenames.mapTo(linkedSetOf()) { filename ->
          filename.removePrefix("carddata/").substringBefore('/')
        }

  public fun definitions(bundleName: String): List<CardDefinition> =
      JSON5.decodeFromString<CardList>(
              GeneratedCardDataResources.read("carddata/$bundleName/cards.json5")
          )
          .cards

  @Serializable private data class CardList(val cards: List<CardDefinition>)

  @OptIn(ExperimentalSerializationApi::class)
  private val JSON5 = Json {
    allowComments = true
    allowTrailingComma = true
    ignoreUnknownKeys = false
    isLenient = true
  }
}
