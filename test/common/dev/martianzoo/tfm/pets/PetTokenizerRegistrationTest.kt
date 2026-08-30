package dev.martianzoo.tfm.pets

import dev.martianzoo.pets.AnchoredRegexToken
import dev.martianzoo.pets.PetTokenizer
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PetTokenizerRegistrationTest {
  @Test
  internal fun tokenRegisteredAfterFirstTokenizationIsRecognized() {
    PetTokenizer.TokenCache.tokenize("CLASS").toList()
    PetTokenizer.TokenCache.cacheLiteral("token~cache", "late token registration")

    PetTokenizer.TokenCache.tokenize("token~cache").toList()
  }

  @Test
  internal fun regexTokenDoesNotSkipAhead() {
    val token = AnchoredRegexToken("word", Regex("word"))

    assertEquals(0, token.match("not word", 0))
    assertEquals(4, token.match("not word", 4))
  }
}
