package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.className
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.genericType
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.typeExpression
import dev.martianzoo.util.joinOrEmpty

sealed class FromExpression : PetNode() {
  override val kind = "FromExpression"

  abstract val toType: TypeExpression
  abstract val fromType: TypeExpression

  data class TypeInFrom(val type: TypeExpression) : FromExpression() {
    override val toType = type
    override val fromType = type

    override fun toString() = "$type"
  }

  data class SimpleFrom(
      override val toType: TypeExpression,
      override val fromType: TypeExpression,
  ) : FromExpression() {

    override fun toString() = "$toType FROM $fromType"
  }

  data class ComplexFrom(
      val className: ClassName,
      val specializations: List<FromExpression> = listOf(),
      val refinement: Requirement? = null, // TODO get rid of?
  ) : FromExpression() {
    init {
      if (specializations.count { it is ComplexFrom || it is SimpleFrom } != 1) {
        throw PetException("Can only have one FROM in an expression")
      }
    }

    override val toType = gte(className, specializations.map { it.toType })
    override val fromType = gte(className, specializations.map { it.fromType }).refine(refinement)

    override fun toString() =
        "$className" +
        specializations.joinOrEmpty(wrap="<>") +
        (refinement?.let { "(HAS $it)" } ?: "")
  }

  companion object : PetParser(){
    internal fun parser(): Parser<FromExpression> {
      return parser {
        val typeInFrom = typeExpression map ::TypeInFrom

        val fromElements =
            zeroOrMore(typeInFrom and skipChar(',')) and
            parser() and
            zeroOrMore(skipChar(',') and typeInFrom) map {
              (before, from, after) -> before + from + after
            }

        val simpleFrom = genericType and skip(_from) and genericType map {
          (to, from) -> SimpleFrom(to, from)
        }

        val complexFrom =
            className and
            skipChar('<') and fromElements and skipChar('>') and
            optional(TypeParsers.refinement) map {
              (name, specs, refins) -> ComplexFrom(name, specs, refins)
            }
        simpleFrom or complexFrom
      }
    }
  }
}
