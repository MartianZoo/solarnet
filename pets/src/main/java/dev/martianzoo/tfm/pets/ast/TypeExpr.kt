package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.PetVisitor
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.className
import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.pre
import dev.martianzoo.util.wrap

/**
 * A particular representation of a type in the Pets language. Could be a simple type (`ClassName`),
 * a parameterized type (`Foo<Bar, Qux>`) or a refined type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`) (the
 * combination of a real type with one or more predicates).
 *
 * Caution is required when using this type, because in many cases different type expressions will
 * represent the same "actual" type; for example `Microbe<This, Player1>` and `Microbe<Player1,
 * This`, or `Tile` and `Tile<Area>`. This type has no idea about that; they are different
 * *representations* so they are considered unequal.
 */
data class TypeExpr(
    override val className: ClassName, // TODO renames?
    val arguments: List<TypeExpr> = listOf(),
    val refinement: Requirement? = null,
    val link: Int? = null,
) : PetNode(), HasClassName {
  companion object {
    fun typeExpr(text: String): TypeExpr = Parsing.parse(TypeParsers.typeExpr, text)
  }

  override fun visitChildren(visitor: PetVisitor) =
      visitor.visit(listOf(className) + arguments + refinement)

  override fun toString() =
      "$className" +
          arguments.joinOrEmpty(wrap = "<>") +
          refinement.wrap("(HAS ", ")") +
          link.pre("^")

  init {
    if (className == CLASS) {
      require(link == null)
      when (arguments.size) {
        0 -> {}
        1 -> require(arguments.first().isTypeOnly)
        else -> error("")
      }
    }
  }

  val isTypeOnly = arguments.isEmpty() && refinement == null && link == null

  @JvmName("addArgsFromClassNames")
  fun addArgs(moreArgs: List<ClassName>): TypeExpr = addArgs(moreArgs.map { it.type })
  fun addArgs(vararg moreArgs: ClassName): TypeExpr = addArgs(moreArgs.toList())

  fun addArgs(moreArgs: List<TypeExpr>): TypeExpr = replaceArgs(arguments + moreArgs)
  fun addArgs(vararg moreArgs: TypeExpr): TypeExpr = addArgs(moreArgs.toList())

  fun replaceArgs(newArgs: List<TypeExpr>): TypeExpr = copy(arguments = newArgs)
  fun replaceArgs(vararg newArgs: TypeExpr): TypeExpr = replaceArgs(newArgs.toList())

  fun refine(ref: Requirement?) =
      if (ref == null) {
        this
      } else {
        require(this.refinement == null)
        copy(refinement = ref)
      }

  override val kind = "TypeExpr"

  // TODO no
  // This is handled differently from the others because so many of the individual parsers end up
  // being needed by the others. So we put them all in properties and pass the whole TypeParsers
  // object around.
  object TypeParsers : PetParser() {
    private val link: Parser<Int> = skipChar('^') and scalar

    private val typeArgs = skipChar('<') and commaSeparated(parser { typeExpr }) and skipChar('>')

    val refinement = parser { group(skip(_has) and Requirement.parser()) }

    val typeExpr: Parser<TypeExpr> =
        className and
        optionalList(typeArgs) and
        optional(refinement) and
        optional(link) map { (clazz, args, ref, link) ->
          TypeExpr(clazz, args, ref, link)
        }
  }
}
