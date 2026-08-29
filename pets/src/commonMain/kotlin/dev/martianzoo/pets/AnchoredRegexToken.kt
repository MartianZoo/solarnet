package dev.martianzoo.pets

import com.github.h0tk3y.betterParse.lexer.Token

/** A regex token that accepts only a match beginning at the tokenizer's current position. */
internal class AnchoredRegexToken(
    name: String,
    private val regex: Regex,
    ignored: Boolean = false,
) : Token(name, ignored) {
  override fun match(input: CharSequence, fromIndex: Int): Int {
    val match = regex.find(input, fromIndex) ?: return 0
    return if (match.range.first == fromIndex) match.value.length else 0
  }
}
