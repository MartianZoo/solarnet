package dev.martianzoo.tfm.carddata

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/** Card definitions embedded for data consumers and Pets generation. */
public object CardData {
  public val bundleNames: Set<String>
    get() =
        GeneratedCardDataResources.filenames.mapTo(linkedSetOf()) { filename ->
          filename.removePrefix("carddata/").substringBefore('/')
        }

  public fun definitions(bundleName: String): List<CardDefinition> =
      JSON5.decodeFromString<CardFile>(
              GeneratedCardDataResources.read("carddata/$bundleName/cards.json5")
          )
          .decks
          .flatMap { deck ->
            deck.cards.map { card ->
              require("deck" !in card) { "Card ${card["name"]} repeats its enclosing deck" }
              JSON5.decodeFromJsonElement<CardDefinition>(
                  JsonObject(card + ("deck" to JsonPrimitive(deck.deck)))
              )
            }
          }

  @Serializable private data class CardFile(val decks: List<DeckDefinition>)

  @Serializable private data class DeckDefinition(val deck: String, val cards: List<JsonObject>)

  @OptIn(ExperimentalSerializationApi::class)
  private val JSON5 = Json {
    allowComments = true
    allowTrailingComma = true
    ignoreUnknownKeys = false
    isLenient = true
  }
}
