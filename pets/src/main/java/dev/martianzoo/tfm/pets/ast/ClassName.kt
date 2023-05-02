package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.HasExpression.Companion.expressions
import dev.martianzoo.tfm.pets.PetTokenizer
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn

/**
 * A camel-case word used as a class name. Not validated except for its general pattern. Create one
 * using the compactly-named function [cn].
 */
public class ClassName private constructor(private val asString: String) :
    PetNode(), HasExpression, Comparable<ClassName> {
  public companion object {
    /** Returns the [ClassName] for the given string. */
    public fun cn(name: String) = ClassName(name)

    private const val CLASS_NAME_PATTERN = "\\b[A-Z]([a-z_][A-Za-z0-9_]*|[A-Z0-9]{0,4})\\b"
    private val classNameRegex = Regex(CLASS_NAME_PATTERN)

    public fun parser() = Parsing.className
  }

  init {
    require(asString.matches(classNameRegex)) { "Bad class name: $asString" }
  }

  /**
   * Returns the expression having this class name as its [Expression.className], and [arguments] as
   * its [Expression.arguments] (in order).
   */
  public fun of(arguments: List<Expression>): Expression = expression.appendArguments(arguments)

  /** Vararg form of [of]. */
  public fun of(vararg arguments: Expression): Expression = of(arguments.toList())

  /**
   * Variant of [of] that extracts [HasExpression.expression] from each argument (note: not
   * [HasExpression.expressionFull]).
   */
  @JvmName("addArgsFromClassNames")
  public fun of(haveArguments: List<HasExpression>): Expression = of(haveArguments.expressions())

  /** Vararg form of [of]. */
  public fun of(vararg haveArguments: HasExpression): Expression = of(haveArguments.toList())

  /**
   * Returns the expression having this class name as its [Expression.className], no arguments, and
   * [refinement] as its [Expression.refinement] (or no refinement if [refinement] is `null`). For
   * example, if `bt` is the requirement `BuildingTag`, then `cn("CardFront").refine(bt)` is the
   * expression `CardFront(HAS BuildingTag)`.
   */
  fun has(refinement: Requirement?) = expression.has(refinement)

  /** For the class name `Foo`, returns the expression `Class<Foo>`. */
  public fun classExpression(): Expression = CLASS.of(this)

  override val kind = ClassName::class
  override fun visitChildren(visitor: Visitor) = Unit

  override val expression: Expression = Expression(this)
  override val expressionFull: Expression by ::expression

  override fun equals(other: Any?) = other is ClassName && other.asString == asString
  override fun hashCode() = asString.hashCode() xor 1994079235
  override fun toString() = asString
  override fun compareTo(other: ClassName) = asString.compareTo(other.asString)

  internal object Parsing : PetTokenizer() {
    val classShortName = _allCapsWordRE map { cn(it.text) }
    val classFullName = _upperCamelRE map { cn(it.text) }
    val className = classFullName or classShortName
  }
}
