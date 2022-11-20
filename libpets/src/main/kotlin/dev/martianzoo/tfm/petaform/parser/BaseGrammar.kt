package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import dev.martianzoo.tfm.petaform.api.Expression

abstract class BaseGrammar<T> : Grammar<T>() {
  @Suppress("unused")
  val comment by regexToken("//[^\n]*", ignore = true)

  @Suppress("unused")
  val whitespace by regexToken("\\s+", ignore = true)

  val leftAngle by literalToken("<")
  val rightAngle by literalToken(">")
  val comma by literalToken(",")
  val `this` by literalToken("This")
  val ident by regexToken("[A-Z][a-z][A-Za-z0-9_]*")
}
