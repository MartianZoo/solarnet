package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.AlternativesFailure
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.MismatchedToken
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.UnexpectedEof
import com.github.h0tk3y.betterParse.parser.parseToEnd
import dev.martianzoo.tfm.api.Exceptions.PetSyntaxException
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.ClassParsing.Declarations
import dev.martianzoo.tfm.pets.PetTokenizer.TokenCache
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledExpression
import dev.martianzoo.util.ParserGroup
import kotlin.reflect.KClass
import kotlin.reflect.cast

/** Various functions for parsing [PetElement]s or [ClassDeclaration]s from text. */
public object Parsing {
  /**
   * Parses a series of Pets class declarations. The syntax is currently not documented (sorry), but
   * examples can be reviewed in `components.pets` and `player.pets`.
   */
  public fun parseClasses(declarationsSource: String): List<ClassDeclaration> {
    val tokens = TokenCache.tokenize(stripLineComments(declarationsSource))
    return parseRepeated(Declarations.topLevelGroup, tokens).flatten()
  }

  /**
   * Parses a *single-line* class declaration. If it has a body with multiple elements, they are
   * semicolon-separated. Syntax examples can be seen in `"components"` fields of `cards.json`.
   */
  public fun parseOneLinerClass(declarationSource: String): ClassDeclaration =
      parse(Declarations.oneLineDecl, declarationSource)

  /**
   * Parses the Pets element of type [P] from [elementSource], and returns it *not* surrounded by a
   * `RAW` block. [P] can only be one of the major element types like [Effect], [Action],
   * [Instruction], [Expression], etc.
   */
  public inline fun <reified P : PetNode> parse(elementSource: String): P =
      parse(P::class, elementSource)

  /** Non-reified form of [parse]. */
  public fun <P : PetNode> parse(expectedType: KClass<P>, elementSource: String): P {
    val matches: TokenMatchesSequence = TokenCache.tokenize(elementSource)
    require(expectedType != PetNode::class) { "missing type info" }

    // TODO: merge this with myThrow somehow
    val pet = parserGroup.parse(expectedType, elementSource, matches)
    return expectedType.cast(pet)
  }

  /** Version of [parse] for use from Java. */
  public fun <P : PetNode> parse(expectedType: Class<P>, source: String) =
      parse(expectedType.kotlin, source)

  internal fun <T> parse(
      parser: Parser<T>,
      source: String,
      matches: TokenMatchesSequence,
      expectedTypeDesc: String? = null,
  ): T {
    try {
      return parser.parseToEnd(matches)
    } catch (e: ParseException) {
      val tokenDesc =
          matches
              .filterNot { it.type.ignored }
              .joinToString(" ") { it.type.name?.replace("\n", "\\n") ?: "NULL" }

      throw PetSyntaxException(
          """
            Expecting: $expectedTypeDesc
            Token stream: $tokenDesc
            Input was:
            ${source.replaceIndent("  ")}
          """
              .trimIndent(),
          e)
    }
  }

  internal fun <T> parse(parser: Parser<T>, source: String, expectedTypeDesc: String? = null): T =
      parse(parser, source, TokenCache.tokenize(source), expectedTypeDesc)

  private fun <T> parseRepeated(listParser: Parser<T>, tokens: TokenMatchesSequence): List<T> {
    fun isEOF(result: ParseResult<*>?): Boolean =
        if (result is AlternativesFailure) {
          result.errors.any(::isEOF)
        } else {
          result is UnexpectedEof
        }

    var index = 0
    val parsed = mutableListOf<T>()
    while (true) {
      when (val result = listParser.tryParse(tokens, index)) {
        is Parsed -> {
          parsed += result.value
          require(result.nextPosition != index) { index }
          index = result.nextPosition
        }
        is ErrorResult -> {
          when {
            result is UnexpectedEof -> break
            result is AlternativesFailure && result.errors.any(::isEOF) -> break
            else -> myThrow(result)
          }
        }
      }
    }
    return parsed
  }

  private val parserGroup by lazy {
    val pgb = ParserGroup.Builder<PetNode>()
    pgb.publish(Action.parser())
    pgb.publish(ClassName.parser())
    pgb.publish(Cost.parser())
    pgb.publish(Effect.parser())
    pgb.publish(Expression.parser())
    pgb.publish(Instruction.parser())
    pgb.publish(Metric.parser())
    pgb.publish(Requirement.parser())
    pgb.publish(ScaledExpression.parser())
    pgb.publish(Trigger.parser())

    pgb.finish()
  }

  // TODO consolidate with other version of this
  private fun myThrow(result: ErrorResult) {
    val message = StringBuilder()
    var ctr = 0
    val locations = mutableMapOf<Pair<Int, Int>, Int>()
    var input: String? = null
    fun visit(result: ErrorResult) {
      when (result) {
        is AlternativesFailure -> result.errors.forEach(::visit)
        is MismatchedToken -> {
          val match: TokenMatch = result.found
          val loc = match.row to match.column
          val thisLoc =
              if (loc in locations) {
                locations[loc]
              } else {
                locations[loc] = ctr
                ctr++
              }
          input = match.input.toString()
          val found = match.text.replace("\n", "\\n")
          val expected = result.expected.name?.replace("\n", "\\n")
          message.append(
              "$thisLoc: at ${match.row}:${match.column}," +
                  " looking for $expected, but found $found\n")
        }
        else -> message.append(result.toString())
      }
    }

    visit(result)

    message.append("\nNow, here is the input:\n")
    input!!.split("\n").forEachIndexed { lineNum, line ->
      message.append("$line\n")
      (1..100).forEach { columnNum ->
        val loc = (lineNum + 1) to columnNum
        message.append(if (loc in locations) "${locations[loc]}" else " ")
      }
      message.append("\n")
    }

    throw RuntimeException(message.toString())
  }

  private val lineCommentRegex = Regex(""" *(//[^\n]*)*\n""")

  private fun stripLineComments(text: String) = lineCommentRegex.replace(text, "\n")
}
