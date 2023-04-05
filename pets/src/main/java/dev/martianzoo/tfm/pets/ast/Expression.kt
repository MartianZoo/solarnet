package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.PetTokenizer
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.className
import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.wrap

/**
 * A particular representation of a type in the Pets language. Could be a simple type (`ClassName`),
 * a parameterized type (`Foo<Bar, Qux>`) or a refined type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`) (the
 * combination of a real type with one or more predicates).
 *
 * Caution is required when using [Expression], because in many cases different expressions will
 * represent the same "actual" type; for example `Microbe<This, Player1>` and `Microbe<Player1,
 * This`, or `Tile` and `Tile<Area>`. This class has no idea about that; they are different
 * *representations* so they are considered unequal.
 */
data class Expression(
    override val className: ClassName,
    val arguments: List<Expression> = listOf(),
    val refinement: Requirement? = null,
) : PetElement(), HasClassName {
  override fun visitChildren(visitor: Visitor) =
      visitor.visit(listOf(className) + arguments + refinement)

  override fun toString() =
      "$className" + arguments.joinOrEmpty(wrap = "<>") + refinement.wrap("(HAS ", ")")

  val simple = arguments.isEmpty() && refinement == null

  @JvmName("addArgsFromClassNames")
  fun addArgs(moreArgs: List<ClassName>): Expression = addArgs(moreArgs.map { it.expression })
  fun addArgs(vararg moreArgs: ClassName): Expression = addArgs(moreArgs.toList())

  fun addArgs(moreArgs: List<Expression>): Expression = replaceArgs(arguments + moreArgs)
  fun addArgs(vararg moreArgs: Expression): Expression = addArgs(moreArgs.toList())

  fun replaceArgs(newArgs: List<Expression>): Expression = copy(arguments = newArgs)
  fun replaceArgs(vararg newArgs: Expression): Expression = replaceArgs(newArgs.toList())

  fun refine(ref: Requirement?) =
      if (ref == null) {
        this
      } else {
        require(this.refinement == null)
        copy(refinement = ref)
      }

  fun hasAnyRefinements() = descendantsOfType<Requirement>().any()

  override val kind = Expression::class.simpleName!!

  internal companion object : PetTokenizer() {
    fun parser(): Parser<Expression> {
      return parser {
        val specs = skipChar('<') and commaSeparated(parser()) and skipChar('>')

        val result =
            className and
            optionalList(specs) and
            optional(group(skip(_has) and Requirement.parser())) map { (clazz, args, ref) ->
              Expression(clazz, args, ref)
            }
        result
      }
    }
  }
}
