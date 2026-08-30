@file:Suppress("PropertyName", "VariableNaming")

package dev.martianzoo.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.asJust
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.utils.Tuple2
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL

/** A base class for parsing objects. */
internal abstract class PetTokenizer {

  private val _quotedText = regex(Regex("""  "[^"]*"  """.trim()))

  /** Parses quote-delimited text. Quotes cannot appear in the contents. */
  internal val quotedText: Parser<String> = _quotedText map { it.text.removeSurrounding("\"") }

  internal val _arrow = literal("->", "arrow")
  internal val _doubleColon = literal("::", "doubleColon")

  // I simply don't want to name all of these and would rather look them up by the char itself
  private val characters = "!@^+,-./:;=?()[]{}<>\n".map { it to literal("$it") }.toMap()

  internal val _by = word("BY")
  internal val _count = word("COUNT")
  internal val _eval = word("EVAL")
  internal val _from = word("FROM")
  internal val _has = word("HAS")
  internal val _if = word("IF")
  internal val _max = word("MAX")
  internal val _or = word("OR")
  internal val _then = word("THEN")
  internal val _x = regex(Regex("""X\b"""), "X")

  // class declarations - making these ignore case causes trouble with `Class<...>`
  internal val _abstract = literal("ABSTRACT")
  internal val _class = literal("CLASS")
  internal val _default = literal("DEFAULT")
  internal val _metric = regex(Regex("""Metric\b"""), "Metric")
  internal val _number = regex(Regex("""Number\b"""), "Number")
  internal val _requirement = regex(Regex("""Requirement\b"""), "Requirement")

  // regexes - could leave the `Regex()` out, but it loses IDEA syntax highlighting!
  internal val _upperCamelRE = regex(Regex("""\b[A-Z][a-z_][A-Za-z0-9_]*\b"""), "UpperCamel")
  internal val _allCapsWordRE = regex(Regex("""([A-Z][A-Z0-9]{0,5})\b"""), "ALLCAPS")
  internal val _lowerCamelRE = regex(Regex("""\b[a-z][A-Za-z0-9]*\b"""), "lowerCamel")
  private val _scalarRE = regex(Regex("""\b(0|[1-9][0-9]*)"""), "scalar")

  internal val rawScalar: Parser<Int> = _scalarRE map { it.text.toInt() }

  internal val intensity =
      optional(
          (char('!') asJust MANDATORY) or (char('.') asJust AMAP) or (char('?') asJust OPTIONAL)
      )

  internal inline fun <reified T> optionalList(parser: Parser<List<T>>) =
      optional(parser) map { it.orEmpty() }

  internal fun isPresent(parser: Parser<*>) = optional(parser) map { it != null }

  internal inline fun <reified T> transform(interior: Parser<T>) =
      _allCapsWordRE and
          skipChar('[') and
          interior and
          skipChar(']') map
          { (trans, inter) ->
            Tuple2(inter, trans.text.removeSuffix("["))
          }

  internal inline fun <reified P> commaSeparated(p: Parser<P>) = separatedTerms(p, char(','))

  internal inline fun <reified T> group(contents: Parser<T>) =
      skipChar('(') and contents and skipChar(')')

  internal inline fun <reified T> maybeGroup(contents: Parser<T>) = contents or group(contents)

  internal fun char(c: Char): Token = characters[c] ?: error("add $c to `characters`")

  internal fun skipChar(c: Char) = skip(char(c))

  internal object TokenCache {
    private val ignoreList =
        listOf<Token>(
            AnchoredRegexToken("backslash-newline", Regex("\\\\\r?\n"), true), // ignore these
            AnchoredRegexToken("horizontal-whitespace", Regex("[ \\t\\r]+"), true),
            AnchoredRegexToken("line-comment", Regex("//[^\\r\\n]*"), true),
        )

    private val map = mutableMapOf<Pair<String, Boolean>, Token>()
    private var tokenizer: DefaultTokenizer? = null

    fun cacheLiteral(text: String, name: String) =
        map[name to false]
            ?: literalToken(name, text).also {
              map[name to false] = it
              tokenizer = null
            }

    fun cacheRegex(regex: Regex, name: String) =
        map[name to true]
            ?: AnchoredRegexToken(name, regex).also {
              map[name to true] = it
              tokenizer = null
            }

    fun tokenize(input: String): TokenMatchesSequence =
        (tokenizer ?: DefaultTokenizer(ignoreList + map.values).also { tokenizer = it }).tokenize(
            input
        )
  }

  private fun literal(text: String, name: String = text) = TokenCache.cacheLiteral(text, name)

  private fun regex(regex: Regex, name: String = "$regex") = TokenCache.cacheRegex(regex, name)

  private fun word(word: String, name: String = word) = regex(Regex("$word\\b"), name)
}
