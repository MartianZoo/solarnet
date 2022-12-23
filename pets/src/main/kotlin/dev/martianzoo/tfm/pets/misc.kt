package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.AlternativesFailure
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.UnexpectedEof
import dev.martianzoo.tfm.pets.Effect.Trigger.OnGain

fun <T> Parser<List<T>>.parseRepeated(tokens: TokenMatchesSequence): List<T> {
  var index = 0
  val components = mutableListOf<T>()
  var isEOF = false
  while (!isEOF) {
    val parseResult = tryParse(tokens, index)
    when (parseResult) {
      is Parsed -> {
        components += parseResult.value
        index = parseResult.nextPosition
      }
      is UnexpectedEof -> isEOF = true
      is AlternativesFailure -> parseResult.errors.any(::isEOF)
      is ErrorResult -> throw ParseException(parseResult)
    }
  }
  return components
}

private fun isEOF(result: ParseResult<*>?): Boolean =
    when (result) {
      is UnexpectedEof -> true
      is AlternativesFailure -> result.errors.any(::isEOF)
      else -> false
    }

fun classNamePattern(): Regex {
  val CLASS_NAME_PATTERN by lazy { Regex("^[A-Z][a-z][A-Za-z0-9_]*$") }
  return CLASS_NAME_PATTERN
}

internal fun actionToEffect(action: Action, index: Int) : Effect  {
  val merged = if (action.cost == null) {
    action.instruction
  } else {
    Instruction.then(action.cost.toInstruction(), action.instruction)
  }
  return Effect(PetsParser.parse("UseAction${index + 1}<This>"), merged)
}

fun pad(s: Any, width: Int) = ("$s" + " ".repeat(width)).substring(0, width)

fun te(s: String) = TypeExpression(s)

internal val rootName = "Component"

internal val rootEx = te(rootName)

