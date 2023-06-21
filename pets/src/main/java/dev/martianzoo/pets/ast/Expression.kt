package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.utils.Tuple2
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.api.Type
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.ast.Requirement.Exact
import dev.martianzoo.pets.ast.Requirement.Min

/**
 * A noun expression in Pets language, which is a particular *representation* of a type. An
 * expression might have arguments (as in `Microbe<Player1, Ants>`), where each (like `Ants`) is
 * itself an expression. It also might have a refinement (as in `Card(HAS VenusTag)`), which is of
 * type [Requirement]. (It could have either, neither, or both.)
 *
 * Many types can have different representations; for example `Microbe<This, Player1>` and
 * `Microbe<Player1, This>` represent the same actual type, as do `Tile` and `Tile<Area>`. As
 * [Expression]s these four example types are all distinct, which could produce unexpected behavior.
 * A class in the *engine* module (`MClassLoader`) resolves expressions into [Type] instances, and
 * does resolve the distinct expressions `Tile` and `Tile<Area>` into the same type.
 */
public data class Expression(
    override val className: ClassName,
    val arguments: List<Expression> = listOf(),
    val refinement: Requirement? = null,
    val forgiving: Boolean = false
) : PetElement(), HasClassName {

  init {
    if (forgiving && refinement !is Min && refinement !is Exact) {
      throw PetSyntaxException("HAS? can only be used with Min/Exact requirements")
    }
  }

  override fun visitChildren(visitor: Visitor) =
      visitor.visit(listOf(className) + arguments + refinement)

  override fun toString() = buildString {
    append(className)
    if (arguments.any()) append(arguments.joinToString(", ", "<", ">"))
    if (refinement != null) {
      append("(HAS")
      if (forgiving) append ("?")
      append(" $refinement)")
    }
  }

  /** Does this expression consist only of a class name, with no arguments and no refinement? */
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

  /**
   * Returns this expression with the given refinement. This expression must not already have a
   * refinement.
   */
  fun has(refinement: Requirement?, forgiving: Boolean): Expression {
    require(this.refinement == null)
    return if (refinement != null) copy(refinement = refinement, forgiving = forgiving) else this
  }

  override val kind = Expression::class

  internal companion object : PetTokenizer() {
    fun parser(): Parser<Expression> {
      return parser {
        val argumentList = skipChar('<') and commaSeparated(parser()) and skipChar('>')
        val refinement: Parser<Tuple2<Boolean, Requirement>> =
            group(skip(_has) and isPresent(char('?')) and Requirement.parser())

        ClassName.parser() and
            optionalList(argumentList) and
            optional(refinement) map
            { (clazz, args, ref) ->
              Expression(clazz, args, ref?.t2, ref?.t1 == true)
            }
      }
    }
  }
}
