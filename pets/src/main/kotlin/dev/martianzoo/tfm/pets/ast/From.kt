package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.className
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.genericType
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.typeExpression
import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.wrap

sealed class From : PetNode() {
  override val kind = "From"

  abstract val toType: TypeExpression
  abstract val fromType: TypeExpression

  data class TypeAsFrom(val type: TypeExpression) : From() {
    override val toType by ::type
    override val fromType by ::type

    override fun toString() = "$type"
  }

  data class SimpleFrom(
      override val toType: TypeExpression,
      override val fromType: TypeExpression
  ) : From() {
    override fun toString() = "$toType FROM $fromType"
  }

  data class ComplexFrom(
      val className: ClassName,
      val arguments: List<From> = listOf(),
      val refinement: Requirement? = null, // TODO get rid of?
  ) : From() {
    init {
      if (arguments.count { it is SimpleFrom || it is ComplexFrom } != 1) {
        throw PetException("Can only have one FROM in an expression")
      }
    }

    override val toType = className.addArgs(arguments.map { it.toType })
    override val fromType = className.addArgs(arguments.map { it.fromType }).refine(refinement)

    override fun toString() =
        "$className${arguments.joinOrEmpty(wrap = "<>")}${refinement.wrap("(HAS ", ")")}"
  }

  companion object : PetParser() {
    fun from(text: String): From = Parsing.parse(parser(), text)

    internal fun parser(): Parser<From> {
      return parser {
        val typeAsFrom = typeExpression map ::TypeAsFrom

        val arguments =
            zeroOrMore(typeAsFrom and skipChar(',')) and
            parser() and
            zeroOrMore(skipChar(',') and typeAsFrom) map { (before, from, after) ->
              before + from + after
            }

        val simpleFrom = genericType and skip(_from) and genericType map { (to, from) -> SimpleFrom(to, from) }

        val complexFrom =
            className and
            skipChar('<') and
            arguments and
            skipChar('>') and
            optional(TypeParsers.refinement) map { (name, args, refins) ->
              ComplexFrom(name, args, refins)
            }

        simpleFrom or complexFrom
      }
    }
  }
}
