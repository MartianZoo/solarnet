package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import dev.martianzoo.tfm.petaform.api.Expression

class BetterParser : PetaformParser {
  override fun parseExpression(petaformSource: String): Expression {
    return ExpressionGrammar.parseToEnd(petaformSource)
  }
}
