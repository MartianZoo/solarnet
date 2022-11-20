package dev.martianzoo.tfm.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.text.RegexOption.MULTILINE

internal object MoshiReader {
  private val MOSHI = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  internal data class CardList(val cards: List<Card>) {
    fun toMap() = cards.associateBy { it.id }.also { require(it.size == cards.size) }
  }

  private val MOSHI_CARD = MOSHI.adapter(CardList::class.java).nullSafe().lenient()

  internal fun readCards(json5: String) = MOSHI_CARD.fromJson(stripComments(json5))!!.toMap()

  internal data class ComponentList(val components: List<Component>) {
    fun toMap() = components.associateBy { it.name }.also { require(it.size == components.size) }
  }

  private val MOSHI_COMPONENT = MOSHI.adapter(ComponentList::class.java).nullSafe().lenient()

  internal fun readComponents(json5: String) = MOSHI_COMPONENT.fromJson(stripComments(json5))!!.toMap()

  private fun stripComments(s: String) = COMMENT_REGEX.replace(s, "")
  private val COMMENT_REGEX = Regex("//.*$", MULTILINE)
}
