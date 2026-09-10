package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
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
 * itself an expression. It also might have a refinement: a conjunction of state-aware requirements
 * (as in `Card(HAS VenusTag)`) and structural differences (as in `Owner(NOT Player1)`). It could
 * have arguments, a refinement, both, or neither.
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
    /** Whether the source wrote angle brackets, including an explicit empty `<>`. */
    val argumentsSpecified: Boolean = arguments.isNotEmpty(),
) : PetElement(), HasClassName, HasExpression, Specification<Expression> {
  // Expressions are immutable after parsing; zero is the uncached sentinel.
  private var cachedHashCode: Int = 0

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
    cachedHashCode = 0
  }

  override fun equals(other: Any?): Boolean =
      this === other ||
          (other is Expression &&
              className == other.className &&
              arguments == other.arguments &&
              refinement == other.refinement &&
              argumentsSpecified == other.argumentsSpecified &&
              derivedClassBody == other.derivedClassBody)

  override fun hashCode(): Int {
    if (cachedHashCode != 0) return cachedHashCode
    var result = className.hashCode()
    result = 31 * result + arguments.hashCode()
    result = 31 * result + (refinement?.hashCode() ?: 0)
    result = 31 * result + argumentsSpecified.hashCode()
    result = 31 * result + (derivedClassBody?.hashCode() ?: 0)
    cachedHashCode = result
    return result
  }

  override val expression: Expression
    get() = this

  override fun isAbstract(info: TypeInfo): Boolean = info.isAbstract(this)

  override fun ensureNarrows(that: Expression, info: TypeInfo): Unit =
      info.ensureNarrows(that, this)

  override fun visitChildren(visitor: Visitor) {
    visitor.visit(className)
    visitor.visit(arguments)
    visitor.visit(refinement)
  }

  override fun toString(): String = buildString {
    append(className)
    if (argumentsSpecified) append(arguments.joinToString(", ", "<", ">"))
    refinement?.let { append("($it)") }
  }

  /** Does this expression consist only of a class name, with no arguments and no refinement? */
  val simple: Boolean = arguments.isEmpty() && refinement == null && !argumentsSpecified

  /**
   * Is this just the name [name], with no arguments and no refinement, however the empty argument
   * list was written? `This` and `This<>` are both the bare `This` placeholder; they are not equal
   * as expressions, because they render differently, but neither one carries an argument.
   */
  internal fun isBare(name: ClassName): Boolean =
      className == name && arguments.isEmpty() && refinement == null

  public fun appendArguments(moreArgs: List<Expression>): Expression =
      replaceArguments(arguments + moreArgs)

  internal fun replaceArguments(newArgs: List<Expression>): Expression =
      copy(
          arguments = newArgs,
          argumentsSpecified = argumentsSpecified || newArgs.isNotEmpty(),
      )

  /**
   * Returns this expression with the given refinement. This expression must not already have a
   * refinement.
   */
  internal fun has(refinement: Refinement?): Expression {
    require(this.refinement == null)
    return if (refinement == null) this else copy(refinement = refinement)
  }

  internal fun has(refinement: Requirement?): Expression {
    require(this.refinement == null)
    return if (refinement != null) copy(refinement = Refinement.has(refinement)) else this
  }

  override val kind: KClass<out PetNode> = Expression::class

  public sealed class Refinement : PetNode() {
    override val kind: KClass<out PetNode> = Refinement::class

    public data class Has(val requirement: Requirement) : Refinement() {
      init {
        require(requirement !is Requirement.And) {
          "a HAS clause cannot contain a top-level requirement conjunction"
        }
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(requirement)

      override fun toString(): String = "HAS $requirement"
    }

    /** Excludes every Type overlapping [excluded] from the explicitly written outer domain. */
    public data class Not(val excluded: Expression) : Refinement() {
      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(excluded)

      override fun toString(): String = "NOT $excluded"
    }

    @ConsistentCopyVisibility
    public data class And internal constructor(val refinements: List<Refinement>) : Refinement() {
      init {
        require(refinements.size >= 2)
        require(refinements.none { it is And })
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(refinements)

      override fun toString(): String = refinements.joinToString()
    }

    public companion object {
      /** The one refinement meaning "both". */
      internal fun join(ref1: Refinement, ref2: Refinement): Refinement {
        if (ref1 == ref2) return ref1
        return create(ref1.conjuncts() + ref2.conjuncts())
      }

      public fun create(refinements: Collection<Refinement>): Refinement {
        val flattened = refinements.flatMap { it.conjuncts() }
        require(flattened.isNotEmpty())
        return if (flattened.size == 1) flattened.single() else And(flattened)
      }

      internal fun has(requirement: Requirement): Refinement =
          create(Requirement.split(requirement).map(::Has))
    }

    internal fun conjuncts(): List<Refinement> = if (this is And) refinements else listOf(this)

    internal fun retaining(predicate: (Refinement) -> Boolean): Refinement? =
        conjuncts().filter(predicate).takeIf { it.isNotEmpty() }?.let(Companion::create)
  }

  internal companion object : PetTokenizer() {
    internal fun refinementParser(): Parser<Refinement> {
      val has = (skip(_has) and Requirement.disjunctionParser()) map Refinement.Companion::has
      val not = (skip(_not) and parser(allowDerivedClass = false)) map { Refinement.Not(it) }
      return group(commaSeparated(has or not) map Refinement.Companion::create)
    }

    fun parser(allowDerivedClass: Boolean = true): Parser<Expression> {
      return parser {
        val argumentList =
            skipChar('<') and
                optionalList(commaSeparated(parser(allowDerivedClass))) and
                skipChar('>')
        val refinement = refinementParser()

        if (allowDerivedClass) {
          ClassName.parser() and
              optional(argumentList) and
              optional(refinement) and
              optional(ClassParsing.Declarations.derivedClassBody) map
              { (clazz, args, ref, body) ->
                Expression(clazz, args.orEmpty(), ref, args != null).let {
                  if (body == null) it else it.withDerivedClassBody(body)
                }
              }
        } else {
          ClassName.parser() and
              optional(argumentList) and
              optional(refinement) map
              { (clazz, args, ref) ->
                Expression(clazz, args.orEmpty(), ref, args != null)
              }
        }
      }
    }
  }
}
