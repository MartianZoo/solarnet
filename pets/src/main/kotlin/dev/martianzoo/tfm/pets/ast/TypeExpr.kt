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
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.className
import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.pre
import dev.martianzoo.util.wrap

/**
 * A noun expression. May be a simple type (`ClassName`), a parameterized type (`Foo<Bar, Qux>`) or
 * a *refined* type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`). A refined type is the combination of a real
 * type with various predicates.
 */
sealed class TypeExpr : PetNode() {
  abstract val link: Int?

  companion object {
    fun typeExpr(text: String): TypeExpr = Parsing.parse(TypeParsers.typeExpr, text)
  }

  abstract fun asGeneric(): GenericTypeExpr

  data class GenericTypeExpr(
      val root: ClassName,
      val args: List<TypeExpr> = listOf(),
      val refinement: Requirement? = null,
      override val link: Int? = null,
  ) : TypeExpr() {
    override fun visitChildren(v: PetVisitor) = v.visit(args + root + refinement)

    override fun toString() =
        "$root" +
        args.joinOrEmpty(wrap = "<>") +
        refinement.wrap("(HAS ", ")") +
        link.pre("^")

    init {
      require(root != CLASS) // Class<...> can never be a generic type
    }

    override fun asGeneric() = this

    val isTypeOnly = args.isEmpty() && refinement == null

    fun addArgs(moreArgs: List<TypeExpr>) = copy(args = args + moreArgs)
    fun replaceArgs(args: List<TypeExpr>) = copy(args = args)

    fun refine(ref: Requirement?) =
        if (ref == null) {
          this
        } else {
          require(this.refinement == null)
          copy(refinement = ref)
        }
  }

  data class ClassLiteral(val className: ClassName, override val link: Int? = null) : TypeExpr() {
    override fun visitChildren(v: PetVisitor) = v.visit(className)
    override fun toString() = "Class<$className${link.pre("^")}>"

    override fun asGeneric() =
        error("Bzzt, this is not a generic type expression, it's a class literal")
  }

  override val kind = "TypeExpr"

  // This is handled differently from the others because so many of the individual parsers end up
  // being needed by the others. So we put them all in properties and pass the whole TypeParsers
  // object around.
  object TypeParsers : PetParser() {
    private val link: Parser<Int> = skipChar('^') and scalar

    private val typeArgs = skipChar('<') and commaSeparated(parser { typeExpr }) and skipChar('>')

    val refinement = group(skip(_has) and parser { Requirement.parser() })

    val typeExpr: Parser<TypeExpr> =
        className and
            optionalList(typeArgs) and
            optional(refinement) and
            optional(link) map
            { (clazz, args, ref, link) ->
              convert(clazz, args, ref, link)
            }

    val genericTypeExpr: Parser<GenericTypeExpr> = typeExpr map { it as GenericTypeExpr }

    // TODO this NASSSTY
    private fun convert(
        rootName: ClassName,
        arguments: List<TypeExpr>,
        refinement: Requirement?,
        link: Int?
    ): TypeExpr {
      if (rootName != CLASS) return GenericTypeExpr(rootName, arguments, refinement, link)

      require(refinement == null && link == null)
      if (arguments.isEmpty()) return COMPONENT.literal // `Class`

      val oneArg = arguments.single()
      if (oneArg == COMPONENT.literal) return CLASS.literal // `Class<Class>`

      val generic = oneArg.asGeneric() // `Class<SomethingElse>`
      require(generic.isTypeOnly)
      return ClassLiteral(generic.root, generic.link)
    }
  }
}
