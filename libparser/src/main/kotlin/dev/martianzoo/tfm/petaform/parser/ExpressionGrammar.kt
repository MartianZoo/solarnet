package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.petaform.api.Expression

object ExpressionGrammar : Grammar<Expression>() {
  @Suppress("unused")
  val comment by regexToken("//[^\n]*", ignore = true)

  @Suppress("unused")
  val whitespace by regexToken("\\s+", ignore = true)

  val leftAngle by literalToken("<")
  val rightAngle by literalToken(">")
  val comma by literalToken(",")

  val ctypeName by regexToken("[A-Z][a-z]\\w*")

  val refinements: Parser<List<Expression>> by
      skip(leftAngle) and
      separatedTerms(reenter(), comma) and
      skip(rightAngle)

  private fun reenter() = parser(this::rootParser)

  override val rootParser by ctypeName and optional(refinements) map {
    (type, refs) -> Expression(type.text, refs ?: listOf())
  }
}
