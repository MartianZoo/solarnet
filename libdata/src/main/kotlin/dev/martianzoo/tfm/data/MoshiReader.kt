package dev.martianzoo.tfm.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.text.RegexOption.DOT_MATCHES_ALL

internal object MoshiReader {
  private val MOSHI = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  internal data class CardList(val cards: List<Card>) {
    fun toMap() = cards.associateBy { it.id }.also { require(it.size == cards.size) }
  }

  private val MOSHI_CARD = MOSHI.adapter(CardList::class.java).nullSafe().lenient()

  internal fun readCards(json5: String) = MOSHI_CARD.fromJson(json5ToJson(json5))!!.toMap()

  internal data class ComponentList(val components: List<Component>) {
    fun toMap() = components.associateBy { it.name }.also { require(it.size == components.size) }
  }

  private val MOSHI_COMPONENT = MOSHI.adapter(ComponentList::class.java).nullSafe().lenient()

  internal fun readComponents(json5: String) = MOSHI_COMPONENT.fromJson(json5ToJson(json5))!!.toMap()

  private fun json5ToJson(json5: String): String {
    return TRAILING_COMMA_REGEX.replace(json5, "")
  }

  private val TRAILING_COMMA_REGEX = Regex(""",(?=\s*(//[^\n]*\n\s*)?[\]}])""", DOT_MATCHES_ALL)
}
