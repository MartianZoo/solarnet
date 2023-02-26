package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.PetVisitor
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.className
import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.wrap

sealed class From : PetNode() {
  override val kind = "From"

  abstract val toType: TypeExpr
  abstract val fromType: TypeExpr

  data class TypeAsFrom(val typeExpr: TypeExpr) : From() {
    override val toType by this::typeExpr
    override val fromType by this::typeExpr

    override fun visitChildren(visitor: PetVisitor) = visitor.visit(typeExpr)
    override fun toString() = "$typeExpr"
  }

  data class SimpleFrom(
      override val toType: TypeExpr,
      override val fromType: TypeExpr,
  ) : From() {
    override fun visitChildren(visitor: PetVisitor) = visitor.visit(toType, fromType)
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

    override fun visitChildren(visitor: PetVisitor) =
        visitor.visit(arguments + className + refinement)

    override val toType = className.addArgs(arguments.map { it.toType })
    override val fromType = className.addArgs(arguments.map { it.fromType }).refine(refinement)

    override fun toString() =
        "$className${arguments.joinOrEmpty(wrap = "<>")}${refinement.wrap("(HAS ", ")")}"
  }

  companion object : PetParser() {
    fun from(text: String): From = Parsing.parse(parser(), text)

    internal fun parser(): Parser<From> {
      return parser {
        val typeAsFrom = TypeExpr.parser() map ::TypeAsFrom
        val simpleFrom =
            TypeExpr.parser() and
            skip(_from) and
            TypeExpr.parser() map { (to, from) -> SimpleFrom(to, from) }

        val argumentList =
            zeroOrMore(typeAsFrom and skipChar(',')) and
            parser() and
            zeroOrMore(skipChar(',') and typeAsFrom) map { (before, from, after) ->
              before + from + after
            }
        val arguments = skipChar('<') and argumentList and skipChar('>')
        val complexFrom =
            className and
            arguments and
            optional(TypeExpr.refinement()) map { (name, args, refins) ->
              ComplexFrom(name, args, refins)
            }

        simpleFrom or complexFrom
      }
    }
  }
}
