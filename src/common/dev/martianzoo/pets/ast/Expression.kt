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
 * The noun of the Pets language: a particular *representation* of a type, as defined by
 * [section 3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions).
 * It appears in every other element, and everywhere it appears it identifies a type.
 *
 * An expression is a class name, an optional argument list and an optional refinement ([rule
 * L3-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions)).
 * Each argument (like `Ants` in `Microbe<Player1, Ants>`) is itself an expression; a refinement is
 * a conjunction of state-aware requirements (as in `Card(HAS VenusTag)`) and structural differences
 * (as in `Owner(NOT Player1)`) ([rule
 * L3-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions)).
 *
 * Two expressions are equal only when their spellings agree ([rule
 * L3-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions)),
 * so a single type has many unequal representations: `Microbe<This, Player1>` and `Microbe<Player1,
 * This>` are different expressions, as are `Tile` and `Tile<Area>`. The type system, not the
 * syntax, is the authority on identity — [ClassLoader] resolves all four into their [Type]s,
 * collapsing the distinctions the syntax deliberately keeps ([rule
 * L3-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions)).
 */
public data class Expression(
    override val className: ClassName,

    /** The written argument list, each argument an expression in its own right. */
    val arguments: List<Expression> = emptyList(),

    /** The written refinement, or null if none was written. */
    val refinement: Refinement? = null,

    /**
     * Whether the source wrote angle brackets, including an explicit empty `<>`. Writing an empty
     * argument list is not the same as writing none: the two denote one type but stay
     * distinguishable, because `<>` says "I accept this use's defaults on purpose" ([rule
     * L3-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions)).
     */
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

  /**
   * Returns this expression with [moreArgs] added after its existing [arguments]. Any resulting
   * non-empty list counts as written, so the result has [argumentsSpecified] set.
   */
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

  /**
   * One clause of an expression's refinement, or a conjunction of them. Each clause repeats its own
   * keyword, so a top-level comma separates clauses rather than continuing one ([rule
   * L3-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions));
   * what a clause *means* is
   * [section 8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#8-refinements)
   * of the type system specification.
   */
  public sealed class Refinement : PetNode() {
    override val kind: KClass<out PetNode> = Refinement::class

    /**
     * Admits only the components of the outer domain meeting [requirement]. A conjunction inside
     * one `HAS` must be grouped, since a bare comma would start the next clause instead ([rule
     * L3-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions)).
     */
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

    /**
     * Admits only what every one of [refinements] admits — the comma-separated conjunction of
     * [rule L3-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions).
     * Conjunctions do not nest; build one through [create].
     */
    @ConsistentCopyVisibility
    public data class And internal constructor(val refinements: Set<Refinement>) : Refinement() {
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

      /**
       * Returns the refinement meeting all of [refinements], flattening any nested conjunctions. A
       * single clause is returned as itself rather than as an [And]. At least one is required.
       */
      public fun create(refinements: Collection<Refinement>): Refinement {
        val flattened = refinements.flatMapTo(linkedSetOf()) { it.conjuncts() }
        require(flattened.isNotEmpty())
        return if (flattened.size == 1) flattened.single() else And(flattened)
      }

      internal fun has(requirement: Requirement): Refinement =
          create(Requirement.split(requirement).map(::Has))
    }

    internal fun conjuncts(): Set<Refinement> = if (this is And) refinements else setOf(this)

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
