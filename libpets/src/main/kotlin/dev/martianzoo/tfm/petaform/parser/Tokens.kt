package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken

object Tokens {
  val tokens = mutableListOf<Token>()

  val comment = regex("//[^\n]*", ignore = true)
  val whitespace = regex("\\s+", ignore = true)

  val arrow = literal("->")
  val comma = literal(",")
  val minus = literal("-")
  val colon = literal(":")
  val twoColons = literal("::")

  val leftParen = literal("(")
  val rightParen = literal(")")
  val leftBracket = literal("[")
  val rightBracket = literal("]")
  val leftAngle = literal("<")
  val rightAngle = literal(">")

  val max = literal("MAX")
  val or = literal("OR")
  val prod = literal("PROD")
  val `this` = literal("This")

  val scalar = regex("[1-9][0-9]*")
  val ident = regex("[A-Z][a-z][A-Za-z0-9_]*")

  private fun regex(r: String, ignore: Boolean = false) =
      regexToken(r, ignore).also { tokens += it }
  private fun literal(l: String, ignore: Boolean = false) =
      literalToken(l, ignore).also { tokens += it }

  val tokenizer = DefaultTokenizer(tokens)
}
