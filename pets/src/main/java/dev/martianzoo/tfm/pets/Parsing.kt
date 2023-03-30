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
import dev.martianzoo.tfm.pets.PetTokenizer.TokenCache
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
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

/** Functions for parsing PETS elements or class declarations from source code. */
public object Parsing {
  public inline fun <reified P : PetElement> parseInput(
      elementSource: String
  ): Raw<P> = Raw(parseAsIs(elementSource))

  /**
   * Parses the PETS element in [elementSource], expecting a construct of type [P], and returning
   * the parsed [P]. [P] can only be one of the major element types like [Effect], [Action],
   * [Instruction], [Expression], etc.
   */
  public inline fun <reified P : PetNode> parseAsIs(elementSource: String): P =
      parseAsIs(P::class, elementSource)

  /**
   * Parses the PETS element in [elementSource], expecting a construct of type [P], and returning
   * the parsed [P]. [P] can only be one of the major elemental types like [Effect], [Action],
   * [Instruction], [Expression], etc.
   */
  public fun <P : PetNode> parseAsIs(expectedType: KClass<P>, elementSource: String): P {
    val matches: TokenMatchesSequence = TokenCache.tokenize(elementSource)
    require(expectedType != PetNode::class) { "missing type info" }

    // TODO: merge this with myThrow somehow
    val pet = parserGroup.parse(expectedType, elementSource, matches)
    return expectedType.cast(pet)
  }

  /** Version of [parseAsIs] for use from Java. */
  public fun <P : PetNode> parseAsIs(expectedType: Class<P>, source: String) =
      parseAsIs(expectedType.kotlin, source)

  /** A minor convenience function for parsing using a particular [Parser] instance. */
  public fun <T> parse(
      parser: Parser<T>,
      source: String,
      matches: TokenMatchesSequence,
      expectedTypeDesc: String? = null
  ): T {
    try {
      return parser.parseToEnd(matches)
    } catch (e: ParseException) {
      val tokenDesc = matches
          .filterNot { it.type.ignored }
          .joinToString(" ") { it.type.name?.replace("\n", "\\n") ?: "NULL" }

      // TODO probably make this a PetSyntaxException
      throw IllegalArgumentException(
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

  public fun <T> parse(parser: Parser<T>, source: String, expectedTypeDesc: String? = null): T =
      parse(parser, source, TokenCache.tokenize(source), expectedTypeDesc)

  // only used by ClassParsing.parseClassDeclarations
  internal fun <T> parseRepeated(
      listParser: Parser<List<T>>,
      tokens: TokenMatchesSequence
  ): List<T> {
    fun isEOF(result: ParseResult<*>?): Boolean =
        if (result is AlternativesFailure) {
          result.errors.any(::isEOF)
        } else {
          result is UnexpectedEof
        }

    var index = 0
    val parsed = mutableListOf<T>()
    while (true) {
      val result: ParseResult<List<T>> = listParser.tryParse(tokens, index)
      when (result) {
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
    pgb.publish(Cost.parser())
    pgb.publish(Effect.parser())
    pgb.publish(Instruction.parser())
    pgb.publish(Metric.parser())
    pgb.publish(Requirement.parser())
    pgb.publish(ScaledExpression.parser())
    pgb.publish(Trigger.parser())
    pgb.publish(Expression.parser())

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
}
