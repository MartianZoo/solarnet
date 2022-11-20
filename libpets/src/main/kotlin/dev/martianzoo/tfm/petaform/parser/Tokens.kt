package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken

object Tokens {
  val tokens = mutableListOf<Token>()

  val comment = regex("//[^\n]*", ignore = true)
  val whitespace = regex("\\s+", ignore = true)

  val leftAngle = literal("<")
  val rightAngle = literal(">")
  val comma = literal(",")
  val minus = literal("-")

  val leftParen = literal("(")
  val rightParen = literal(")")

  val max = literal("MAX")
  val or = literal("OR")
  val thiss = literal("This")

  val ident = regex("[A-Z][a-z][A-Za-z0-9_]*")
  val scalar = regex("[1-9][0-9]*")

  val prodStart = literal("PROD[")
  val prodEnd = literal("PROD]")

  private fun regex(r: String, ignore: Boolean = false) =
      regexToken(r, ignore).also { tokens += it }
  private fun literal(l: String, ignore: Boolean = false) =
      literalToken(l, ignore).also { tokens += it }

  val tokenizer = DefaultTokenizer(tokens)
}
