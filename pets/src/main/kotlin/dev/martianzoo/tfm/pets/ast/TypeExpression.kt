package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.className
import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.pre
import dev.martianzoo.util.wrap

/**
 * A noun expression. May be a simple type (`ClassName`), a parameterized type (`Foo<Bar, Qux>`) or
 * a *refined* type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`). A refined type is the combination of a real
 * type with various predicates.
 */
sealed class TypeExpression : PetNode() {
  abstract val link: Int?

  companion object {
    fun typeExpression(text: String): TypeExpression =
        Parsing.parse(TypeParsers.typeExpression, text)

    fun genericTypeExpression(text: String): GenericTypeExpression =
        Parsing.parse(TypeParsers.genericType, text)
  }

  abstract fun asGeneric(): GenericTypeExpression

  data class GenericTypeExpression(
      val root: ClassName,
      val args: List<TypeExpression> = listOf(),
      val refinement: Requirement? = null,
      override val link: Int? = null,
  ) : TypeExpression() {
    override fun toString() =
        "$root${args.joinOrEmpty(wrap = "<>")}${refinement.wrap("(HAS ", ")")}${link.pre("^")}"

    override fun asGeneric() = this

    val isTypeOnly = args.isEmpty() && refinement == null

    fun addArgs(moreArgs: List<TypeExpression>): GenericTypeExpression {
      return copy(args = args + moreArgs)
    }

    fun replaceArgs(args: List<TypeExpression>): GenericTypeExpression {
      return copy(args = args)
    }

    fun refine(ref: Requirement?): GenericTypeExpression {
      return if (ref == null) {
        this
      } else {
        require(this.refinement == null)
        copy(refinement = ref)
      }
    }
  }

  data class ClassLiteral(val className: ClassName, override val link: Int? = null) :
      TypeExpression() {
    override fun toString() = "$className${link.pre("^")}.CLASS"
    override fun asGeneric() =
        error("Bzzt, this is not a generic type expression, it's a class literal")
  }

  override val kind = "TypeExpression"

  // This is handled differently from the others because so many of the individual parsers end up
  // being needed by the others. So we put them all in properties and pass the whole TypeParsers
  // object around.
  object TypeParsers : PetParser() {
    private val link: Parser<Int> = skipChar('^') and scalar

    private val classLiteral =
        className and
            optional(link) and
            skipChar('.') and
            skip(_class) map
            { (name, link) ->
              ClassLiteral(name, link)
            }

    private val typeArgs =
        skipChar('<') and commaSeparated(parser { typeExpression }) and skipChar('>')

    val refinement = group(skip(_has) and parser { Requirement.parser() })

    val genericType: Parser<GenericTypeExpression> =
        className and
            optionalList(typeArgs) and
            optional(refinement) and
            optional(link) map
            { (type, args, ref, link) ->
              GenericTypeExpression(type, args, ref, link)
            }

    val typeExpression = classLiteral or genericType
  }
}
