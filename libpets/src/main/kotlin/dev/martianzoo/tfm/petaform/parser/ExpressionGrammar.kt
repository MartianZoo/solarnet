package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.petaform.api.ByName
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.RootType
import dev.martianzoo.tfm.petaform.api.This
import dev.martianzoo.tfm.petaform.parser.Tokens.comma
import dev.martianzoo.tfm.petaform.parser.Tokens.ident
import dev.martianzoo.tfm.petaform.parser.Tokens.leftAngle
import dev.martianzoo.tfm.petaform.parser.Tokens.rightAngle
import dev.martianzoo.tfm.petaform.parser.Tokens.thiss

object ExpressionGrammar {
  val ctypeName: Parser<ByName> =
      ident map { ByName(it.text) }

  val rootType: Parser<RootType> =
      thiss map { This } or ctypeName

  val refinements: Parser<List<Expression>> =
      skip(leftAngle) and
      separatedTerms(parser { expression }, comma) and
      skip(rightAngle)

  val expression: Parser<Expression> = (rootType and optional(refinements)) map {
    (type, refs) -> Expression(type, refs ?: listOf())
  }
}
