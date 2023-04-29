package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.PetTokenizer
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.className
import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.wrap

/**
 * A noun expression in Pets; a particular representation of a type. Could be a simple type (`Foo`),
 * a type with arguments (`Foo<Bar, Qux>`), or a refined type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`) (the
 * combination of a real type with a [Requirement]).
 *
 * **Caution:** in many cases different expressions can represent the same "actual" type; for
 * example `Microbe<This, Player1>` and `Microbe<Player1, This`, or for another example, `Tile` and
 * `Tile<Area>`. This class has no idea about that; they are different *representations* so they are
 * considered unequal.
 */
public data class Expression(
    override val className: ClassName,
    val arguments: List<Expression> = listOf(),
    val refinement: Requirement? = null,
) : PetElement(), HasClassName {
  override fun visitChildren(visitor: Visitor) =
      visitor.visit(listOf(className) + arguments + refinement)

  override fun toString() =
      "$className" + arguments.joinOrEmpty(wrap = "<>") + refinement.wrap("(HAS ", ")")

  /**
   * Is this just `ClassName.expression` for some class name? (That is, no arguments, no
   * refinement?)
   */
  val simple: Boolean = arguments.isEmpty() && refinement == null

  @JvmName("extractAndAppendArguments")
  fun appendArguments(moreArgs: List<HasExpression>): Expression =
      appendArguments(moreArgs.map { it.expression })

  fun appendArguments(vararg moreArgs: HasExpression): Expression =
      appendArguments(moreArgs.toList())

  fun appendArguments(moreArgs: List<Expression>): Expression =
      replaceArguments(arguments + moreArgs)

  fun appendArguments(vararg moreArgs: Expression): Expression = appendArguments(moreArgs.toList())

  fun replaceArguments(newArgs: List<Expression>): Expression = copy(arguments = newArgs)
  fun replaceArguments(vararg newArgs: Expression): Expression = replaceArguments(newArgs.toList())

  /** Returns this expression (which must have no refinement already), plus the given refinement. */
  fun has(refinement: Requirement?) =
      if (refinement == null) {
        this
      } else {
        require(this.refinement == null)
        copy(refinement = refinement)
      }

  override val kind = Expression::class.simpleName!!

  internal companion object : PetTokenizer() {
    fun parser(): Parser<Expression> {
      return parser {
        val specs = skipChar('<') and commaSeparated(parser()) and skipChar('>')

        val result =
            className and
                optionalList(specs) and
                optional(group(skip(_has) and Requirement.parser())) map
                { (clazz, args, ref) ->
                  Expression(clazz, args, ref)
                }
        result
      }
    }
  }
}
