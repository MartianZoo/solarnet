package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.utils.Tuple2

/** Parses the Petaform language. */
open class PetTokenizer {
  internal fun tokenize(input: String) = TokenCache.tokenize(input)

  internal val arrow = literal("->", "arrow")
  internal val doubleColon = literal("::", "doublecolon")

  // I simply don't want to name all of these and would rather look them up by the char itself
  private val characters = "!$+,-./:;=?()[]{}<>\n".map { it to literal("$it") }.toMap()

  internal val _by = literal("BY")
  internal val _from = literal("FROM")
  internal val _has = literal("HAS")
  internal val _max = literal("MAX")
  internal val _or = literal("OR")
  internal val _then = literal("THEN")

  // class declarations
  internal val _abstract = literal("ABSTRACT")
  internal val _class = literal("CLASS")
  internal val _default = literal("DEFAULT")

  // scripts
  internal val _become = literal("BECOME")
  internal val _count = literal("COUNT")
  internal val _exec = literal("EXEC")
  internal val _require = literal("REQUIRE")

  // regexes - could leave the `Regex()` out, but it loses IDEA syntax highlighting!
  internal val upperCamelRE = regex(Regex("""\b[A-Z][a-z][A-Za-z0-9_]*\b"""), "UpperCamel")
  internal val scalarRE = regex(Regex("""\b(0|[1-9][0-9]*)\b"""), "scalar")
  internal val lowerCamelRE = regex(Regex("""\b[a-z][a-zA-Z0-9]*\b"""), "lowerCamel")
  internal val allCapsWordRE = regex(Regex("""\b[A-Z]+\b"""), "ALLCAPS")

  internal inline fun <reified T> optionalList(parser: Parser<List<T>>) =
      optional(parser) map { it ?: listOf() }

  internal inline fun <reified T> transform(interior: Parser<T>) =
      allCapsWordRE and skipChar('[') and interior and skipChar(']') map {
        (trans, inter) -> Tuple2(inter, trans.text.removeSuffix("["))
      }

  internal inline fun <reified P> commaSeparated(p: Parser<P>) = separatedTerms(p, char(','))

  internal inline fun <reified T> parens(contents: Parser<T>) =
      skipChar('(') and contents and skipChar(')')

  internal inline fun <reified T> maybeGroup(contents: Parser<T>) = contents or parens(contents)

  internal fun char(c: Char): Token = characters[c] ?: error("add $c to `characters`")

  internal fun skipChar(c: Char) = skip(char(c))

  private object TokenCache {
    val ignoreList = listOf<Token>(
        regexToken("backslash-newline", "\\\\\n", true), // ignore these
        regexToken("spaces", " +", true)
    )

    val map = mutableMapOf<Pair<String, Boolean>, Token>()

    fun cacheLiteral(text: String, name: String) =
        map.computeIfAbsent(name to false) { literalToken(name, text) }
    fun cacheRegex(regex: Regex, name: String) =
        map.computeIfAbsent(name to true) { regexToken(name, regex) }

    val toke by lazy { DefaultTokenizer(ignoreList + map.values) }

    fun tokenize(input: String) = toke.tokenize(input)
  }

  internal fun literal(text: String, name: String = text) = TokenCache.cacheLiteral(text, name)
  internal fun regex(regex: Regex, name: String = "$regex") = TokenCache.cacheRegex(regex, name)
}
