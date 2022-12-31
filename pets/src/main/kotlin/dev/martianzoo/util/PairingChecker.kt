package dev.martianzoo.util

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore

object PairingChecker {
  fun check(s: String) { parsers.parse<Ok>(s) }
  fun isValid(s: String) = parsers.isValid<Ok>(s)

  private val parsers: ParserGroup<Ok> by lazy {
    val p = ParserGroupBuilder<Ok>()

    val parenL = p.literal("("); val parenR = p.literal(")")
    val brackL = p.literal("["); val brackR = p.literal("]")
    val braceL = p.literal("{"); val braceR = p.literal("}")
    val angleL = p.literal("<"); val angleR = p.literal(">")

    val pairChar =
        parenL or parenR or
        brackL or brackR or
        braceL or braceR or
        angleL or angleR

    val any = p.parser<Ok>()

    val parend = skip(parenL) and any and skip(parenR)
    val brackd = skip(brackL) and any and skip(brackR)
    val braced = skip(braceL) and any and skip(braceR)
    val angled = skip(angleL) and any and skip(angleR)

    val paired = parend or brackd or braced or angled

    val quote = p.literal("\"")
    val backslash = p.literal("\\")
    val filler = p.regex("""[^()\[\]{}<>"\\]*""")

    val quotable =
        (backslash and quote) or
        (backslash and backslash) or
        pairChar or
        filler

    val quoted = skip(quote) and zeroOrMore(quotable) and skip(quote)

    val whole = zeroOrMore(paired or quoted or filler) map { Ok.OK }
    p.publish(whole)
    p.freeze()
  }

  enum class Ok { OK }
}
