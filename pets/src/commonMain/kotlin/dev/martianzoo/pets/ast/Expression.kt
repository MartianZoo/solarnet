package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.ClassParsing
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.types.ClassLoader
import dev.martianzoo.pets.types.Type
import kotlin.reflect.KClass

/**
 * A noun expression in Pets language, which is a particular *representation* of a type. An
 * expression might have arguments (as in `Microbe<Player1, Ants>`), where each (like `Ants`) is
 * itself an expression. It also might have a refinement (as in `Card(HAS VenusTag)`), which is of
 * type [Requirement]. (It could have either, neither, or both.)
 *
 * Many types can have different representations; for example `Microbe<This, Player1>` and
 * `Microbe<Player1, This>` represent the same actual type, as do `Tile` and `Tile<Area>`. As
 * [Expression]s these four example types are all distinct, which could produce unexpected behavior.
 * [ClassLoader] resolves expressions into [Type] instances, and does resolve the distinct
 * expressions `Tile` and `Tile<Area>` into the same type.
 */
public data class Expression(
    override val className: ClassName,
    val arguments: List<Expression> = emptyList(),
    val refinement: Refinement? = null,
    val complement: Boolean = false,
    /** Whether the source wrote angle brackets, including an explicit empty `<>`. */
    val argumentsSpecified: Boolean = arguments.isNotEmpty(),
) : PetElement(), HasClassName, HasExpression, Specification<Expression> {

  internal var derivedClassBody: ClassParsing.Body? = null
    private set

  /**
   * Adds parser-only source information while this expression is being constructed. It is set at
   * most once, before the expression can enter an AST collection, and removed before a parsed AST
   * leaves [dev.martianzoo.pets.Parsing].
   */
  internal fun withDerivedClassBody(body: ClassParsing.Body): Expression = apply {
    check(derivedClassBody == null)
    derivedClassBody = body
  }

  override fun equals(other: Any?): Boolean =
      this === other ||
          (other is Expression &&
              className == other.className &&
              arguments == other.arguments &&
              refinement == other.refinement &&
              complement == other.complement &&
              derivedClassBody == other.derivedClassBody)

  override fun hashCode(): Int {
    var result = className.hashCode()
    result = 31 * result + arguments.hashCode()
    result = 31 * result + (refinement?.hashCode() ?: 0)
    result = 31 * result + complement.hashCode()
    return 31 * result + (derivedClassBody?.hashCode() ?: 0)
  }

  override val expression: Expression
    get() = this

  override fun isAbstract(info: TypeInfo): Boolean = info.isAbstract(this)

  override fun ensureNarrows(that: Expression, info: TypeInfo): Unit =
      info.ensureNarrows(that, this)

  override fun visitChildren(visitor: Visitor): Unit =
      visitor.visit(listOf(className) + arguments + refinement)

  override fun toString(): String = buildString {
    if (complement) append("!")
    append(className)
    if (argumentsSpecified) append(arguments.joinToString(", ", "<", ">"))
    refinement?.let { append("($it)") }
  }

  /** Does this expression consist only of a class name, with no arguments and no refinement? */
  val simple: Boolean =
      !complement && arguments.isEmpty() && refinement == null && !argumentsSpecified

  public fun appendArguments(moreArgs: List<Expression>): Expression =
      replaceArguments(arguments + moreArgs)

  internal fun replaceArguments(newArgs: List<Expression>): Expression =
      copy(
          arguments = newArgs,
          argumentsSpecified = argumentsSpecified || newArgs.isNotEmpty(),
      )

  internal fun uncomplemented(): Expression = copy(complement = false)

  /**
   * Returns this expression with the given refinement. This expression must not already have a
   * refinement.
   */
  internal fun has(refinement: Refinement?) =
      has(refinement?.requirement, refinement?.forgiving ?: false)

  internal fun has(refinement: Requirement?, forgiving: Boolean): Expression {
    require(this.refinement == null)
    return if (refinement != null) copy(refinement = Refinement(refinement, forgiving)) else this
  }

  override val kind: KClass<out PetNode> = Expression::class

  public data class Refinement(
      val requirement: Requirement,
      val forgiving: Boolean,
  ) : PetNode() {
    override val kind: KClass<out PetNode> = Refinement::class

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(requirement)

    override fun toString(): String = if (forgiving) "HAS? $requirement" else "HAS $requirement"

    internal companion object {
      internal fun join(ref1: Refinement, ref2: Refinement): Refinement? {
        if (ref1.forgiving != ref2.forgiving) return null
        return Refinement(
            Requirement.join(ref1.requirement, ref2.requirement)!!,
            ref1.forgiving,
        )
      }
    }
  }

  internal companion object : PetTokenizer() {
    internal fun refinementParser(): Parser<Refinement> =
        group(skip(_has) and isPresent(char('?')) and Requirement.parser()) map
            { (forgiving, requirement) ->
              Refinement(requirement, forgiving)
            }

    fun parser(allowDerivedClass: Boolean = true): Parser<Expression> {
      return parser {
        val argumentList =
            skipChar('<') and
                optionalList(commaSeparated(parser(allowDerivedClass))) and
                skipChar('>')
        val refinement = refinementParser()

        if (allowDerivedClass) {
          isPresent(char('!')) and
              ClassName.parser() and
              optional(argumentList) and
              optional(refinement) and
              optional(ClassParsing.Declarations.derivedClassBody) map
              { (not, clazz, args, ref, body) ->
                Expression(clazz, args.orEmpty(), ref, not, args != null).let {
                  if (body == null) it else it.withDerivedClassBody(body)
                }
              }
        } else {
          isPresent(char('!')) and
              ClassName.parser() and
              optional(argumentList) and
              optional(refinement) map
              { (not, clazz, args, ref) ->
                Expression(clazz, args.orEmpty(), ref, not, args != null)
              }
        }
      }
    }
  }
}
