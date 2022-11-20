package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.petaform.api.ByName
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.RootType
import dev.martianzoo.tfm.petaform.api.This

object ExpressionGrammar : Grammar<Expression>() {
  @Suppress("unused")
  val comment by regexToken("//[^\n]*", ignore = true)

  @Suppress("unused")
  val whitespace by regexToken("\\s+", ignore = true)

  val leftAngle by literalToken("<")
  val rightAngle by literalToken(">")
  val comma by literalToken(",")
  val `this` by literalToken("This")
  val ident by regexToken("[A-Z][a-z][A-Za-z0-9_]*")

  // trick for enabling reentrancy
  private val expression = parser(this::rootParser)

  private val ctypeName: Parser<ByName> by ident map { ByName(it.text) }
  private val rootType: Parser<RootType> by `this` map { This } or ctypeName

  private val refinements: Parser<List<Expression>> by
      skip(leftAngle) and
      separatedTerms(expression, comma) and
      skip(rightAngle)

  override val rootParser: Parser<Expression> by (rootType and optional(refinements)) map {
    (type, refs) -> Expression(type, refs ?: listOf())
  }
}
