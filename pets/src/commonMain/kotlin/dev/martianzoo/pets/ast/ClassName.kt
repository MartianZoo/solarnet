package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.HasExpression.Companion.expressions
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * A camel-case word used as a class name. Not validated except for its general pattern. Create one
 * using the compactly-named function [cn].
 */
public class ClassName private constructor(private val asString: String) :
    PetNode(), HasExpression, Comparable<ClassName> {
  public companion object {
    /** Returns the [ClassName] for the given string. */
    public fun cn(name: String): ClassName = ClassName(name)

    private const val CLASS_NAME_PATTERN = "\\b[A-Z]([a-z_][A-Za-z0-9_]*|[A-Z0-9]{0,4})\\b"
    private val classNameRegex = Regex(CLASS_NAME_PATTERN)

    public fun parser(): com.github.h0tk3y.betterParse.parser.Parser<ClassName> = Parsing.className
  }

  init {
    require(asString.matches(classNameRegex)) { "Bad class name: $asString" }
  }

  /**
   * Returns the expression having this class name as its [Expression.className], extracting
   * [HasExpression.expression] from each argument (not [HasExpression.expressionFull]).
   */
  public fun of(arguments: List<HasExpression>): Expression =
      expression.appendArguments(arguments.expressions())

  /** Vararg form of [of]. */
  public fun of(vararg arguments: HasExpression): Expression = of(arguments.toList())

  public fun of(): Expression = expression

  /**
   * Returns the expression having this class name as its [Expression.className], no arguments, and
   * [refinement] as its [Expression.refinement] (or no refinement if [refinement] is `null`). For
   * example, if `bt` is the requirement `2 BuildingTag`, then `cn("CardFront").has(bt)` is the
   * expression `CardFront(HAS 2 BuildingTag)`.
   */
  public fun has(refinement: Requirement?, forgiving: Boolean = false): Expression =
      expression.has(refinement, forgiving)

  /** For the class name `Foo`, returns the expression `Class<Foo>`. */
  public fun classExpression(): Expression = CLASS.of(this)

  override val kind: kotlin.reflect.KClass<out PetNode> = ClassName::class

  override fun visitChildren(visitor: Visitor): Unit = Unit

  override val expression: Expression = Expression(this)
  override val expressionFull: Expression by ::expression

  override fun equals(other: Any?): Boolean = other is ClassName && other.asString == asString

  override fun hashCode(): Int = asString.hashCode() xor 1994079235

  override fun toString(): String = asString

  override fun compareTo(other: ClassName): Int = asString.compareTo(other.asString)

  internal object Parsing : PetTokenizer() {
    val classShortName = _allCapsWordRE map { cn(it.text) }
    val classFullName = _upperCamelRE map { cn(it.text) }
    val className = classFullName or classShortName
  }
}
