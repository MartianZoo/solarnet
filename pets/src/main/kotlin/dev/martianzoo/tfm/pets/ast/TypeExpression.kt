package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.Parsing.parsePets
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.util.joinOrEmpty

/**
 * A noun expression. May be a simple type (`ClassName`), a parameterized type
 * (`Foo<Bar, Qux>`) or a *refined* type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`). A
 * refined type is the combination of a real type with various predicates.
 */
sealed class TypeExpression : PetNode() {
  abstract val className: ClassName

  data class GenericTypeExpression(
      override val className: ClassName,
      val specs: List<TypeExpression> = listOf(),
      val refinement: Requirement? = null,
  ) : TypeExpression() {
    override fun toString() =
        "$className" + specs.joinOrEmpty(wrap = "<>") + (refinement?.let { "(HAS $it)" } ?: "")

    fun specialize(specs: List<TypeExpression>): GenericTypeExpression {
      require(this.specs.isEmpty())
      return copy(specs = specs)
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

  data class ClassLiteral(override val className: ClassName) : TypeExpression() {
    override fun toString() = "$className.CLASS"
  }

  // TODO
  companion object {
    fun gte(className: ClassName, specs: List<TypeExpression>) =
        GenericTypeExpression(className, specs)
    fun gte(className: String, specs: List<TypeExpression>) = gte(ClassName(className), specs)

    @JvmName("gteFromStrings")
    fun gte(className: String, specs: List<String>) = gte(ClassName(className), specs)

    @JvmName("gteFromStrings")
    fun gte(className: ClassName, specs: List<String>) = gte(className, specs.map { parsePets(it) })

    fun gte(className: ClassName, first: String, vararg rest: String) =
        gte(className, listOf(first) + rest)

    fun gte(className: String, first: String, vararg rest: String) =
        gte(className, listOf(first) + rest)

    fun gte(className: ClassName, vararg specs: TypeExpression):GenericTypeExpression = gte(className, *specs)
    fun gte(className: String, vararg specs: TypeExpression) = gte(className, specs.toList())

    fun gte(className: ClassName) = gte(className, listOf<TypeExpression>())
    fun gte(className: String) = gte(className, listOf<String>())
  }

  override val kind = "TypeExpression"

  // This is handled differently from the others because so many of the individual parsers end up
  // being needed by the others. So we put them all in properties and pass the whole TypeParsers
  // object around.
  object TypeParsers : PetParser() {
    val classShortName = _allCapsWordRE map { ClassName(it.text) }
    val classFullName = _upperCamelRE map { ClassName(it.text) }
    val className = classFullName // or classShortName -- why does that break everything?

    val classLiteral =
        className and
        skipChar('.') and
        skip(_class) map TypeExpression::ClassLiteral

    val optlSpecs =
        optionalList(skipChar('<') and
        commaSeparated(parser { typeExpression }) and
        skipChar('>'))

    val refinement = group(skip(_has) and parser { Requirement.parser() })

    val genericType: Parser<GenericTypeExpression> =
        className and optlSpecs and optional(refinement) map { (type, specs, ref) ->
          GenericTypeExpression(type, specs, ref)
        }

    val typeExpression = classLiteral or genericType
  }
}
