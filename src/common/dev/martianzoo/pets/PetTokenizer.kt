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

/**
 * A base class for parsing objects. The tokens here are the lexical level of language-spec sections
 * 1 and 2: reserved keywords (L2-2), the name grammars (L2-1, L2-3, L2-4), and the whitespace,
 * comment and line-continuation rules (L1-10).
 */
internal abstract class PetTokenizer {

  private val _quotedText = regex(Regex("""  "[^"]*"  """.trim()))

  /** Parses quote-delimited text. Quotes cannot appear in the contents (L1-6, L1-8). */
  internal val quotedText: Parser<String> = _quotedText map { it.text.removeSurrounding("\"") }

  internal val _arrow = literal("->", "arrow")
  internal val _doubleColon = literal("::", "doubleColon")

  // I simply don't want to name all of these and would rather look them up by the char itself
  private val characters = "!@^+,-./:;=?()[]{}<>\n".map { it to literal("$it") }.toMap()

  // Rule L2-2: these are the words the grammar itself uses, and none may be a class name. The
  // spellings are exact, so `Max`, `By` and `Has` remain perfectly good class names.
  internal val _by = word("BY")
  internal val _count = word("COUNT")
  internal val _each = word("EACH")
  internal val _eval = word("EVAL")
  internal val _from = word("FROM")
  internal val _has = word("HAS")
  internal val _if = word("IF")
  internal val _max = word("MAX")
  internal val _not = word("NOT")
  internal val _or = word("OR")
  internal val _rank = word("RANK")
  internal val _then = word("THEN")
  internal val _x = regex(Regex("""X\b"""), "X")

  // class declarations - making these ignore case causes trouble with `Class<...>`
  internal val _abstract = word("ABSTRACT")
  internal val _class = word("CLASS")
  internal val _default = word("DEFAULT")
  internal val _metric = regex(Regex("""Metric\b"""), "Metric")
  internal val _number = regex(Regex("""Number\b"""), "Number")
  internal val _requirement = regex(Regex("""Requirement\b"""), "Requirement")

  // regexes - could leave the `Regex()` out, but it loses IDEA syntax highlighting!
  // Rules L2-1 (class names), L2-3 (property names), and L2-4 (transform kinds).
  internal val _mixedCaseClassNameRE =
      regex(
          Regex("""\b[A-Z](?=[A-Za-z0-9_]*[a-z])[A-Za-z0-9_]*\b"""),
          "mixed-case class name",
      )
  internal val _allCapsWordRE = regex(Regex("""\b[A-Z][A-Z0-9_]*\b"""), "ALLCAPS")
  internal val _lowerCamelRE = regex(Regex("""\b[a-z][A-Za-z0-9]*\b"""), "lowerCamel")
  private val _scalarRE = regex(Regex("""\b(0|[1-9][0-9]*)"""), "scalar")

  internal val rawScalar: Parser<Int> = _scalarRE map { it.text.toInt() }

  internal val intensity =
      optional(
          (char('!') asJust MANDATORY) or (char('.') asJust AMAP) or (char('?') asJust OPTIONAL)
      )

  internal inline fun <reified T> optionalList(parser: Parser<List<T>>) =
      optional(parser) map { it.orEmpty() }

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
    // Rule L1-10: horizontal whitespace is insignificant, `//` runs to end of line, and a backslash
    // before a line ending continues the line, so one body element may span several source lines.
    // Newlines themselves are significant, as separators only, so they are not ignored here.
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
