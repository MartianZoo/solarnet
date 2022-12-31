package dev.martianzoo.util

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore

const val allowEmpty = false
const val allowRedundant = false

object PairingChecker {
  fun check(s: String) {
    require(parsers.parse<String>(s) != "ERR")
  }

  fun isValid(s: String) = try {
    check(s)
    true
  } catch (e: Exception) {
    false
  }

  private val parsers: ParserGroup<String> by lazy {
    val p = ParserGroupBuilder<String>()

    val parenL = p.literal("("); val parenR = p.literal(")")
    val brackL = p.literal("["); val brackR = p.literal("]")
    val braceL = p.literal("{"); val braceR = p.literal("}")
    val angleL = p.literal("<"); val angleR = p.literal(">")

    val pairChar =
        parenL or parenR or
        brackL or brackR or
        braceL or braceR or
        angleL or angleR

    val any = p.parser<String>()

    val parend = parenL and any and skip(parenR)
    val brackd = brackL and any and skip(brackR)
    val braced = braceL and any and skip(braceR)
    val angled = angleL and any and skip(angleR)

    val paired = parend or brackd or braced or angled map {
      (opener, contains) ->
          when {
            contains == "ERR" -> "ERR"
            !allowEmpty && contains == "mt" -> "ERR"
            !allowRedundant && contains == opener.text -> "ERR"
            else -> opener.text
          }
    }

    val quote = p.literal("\"")
    val backslash = p.literal("\\")
    val filler = p.regex("""[^()\[\]{}<>"\\]*""") map { "ok" }

    val quotable =
        (backslash and quote) or
        (backslash and backslash) or
        pairChar or
        filler

    val quoted = skip(quote) and zeroOrMore(quotable) and skip(quote) map { "ok" }

    val whole = zeroOrMore(paired or quoted or filler) map {
      when {
        it.isEmpty() -> "mt"
        it.contains("ERR") -> "ERR"
        it.size == 1 -> it.first()
        else -> "ok"
      }
    }
    p.publish(whole)
    p.freeze()
  }
}
