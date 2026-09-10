package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.HasExpression.Companion.expressions
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * An uppercase-leading identifier used as a class name, matching the grammar of [rule
 * L2-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-names).
 * After the initial ASCII uppercase letter, letters, digits, and underscores are allowed, so
 * `GreeneryTile`, `Tharsis_2_2`, `MC`, and `TOOLONG` are all names. Reserved keywords are rejected
 * ([rule
 * L2-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-names));
 * because the reserved spellings are exact, `Max` and `Has` are perfectly good class names.
 *
 * Beyond that pattern a name is not validated here — there is one namespace and no scoping, and a
 * name means whatever the class table says it means ([rule
 * L2-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-names)).
 * Create one using the compactly-named function [cn].
 */
public class ClassName private constructor(public val asString: String) :
    PetNode(), HasExpression, Comparable<ClassName> {
  public companion object {
    private val reservedNames =
        setOf(
            "ABSTRACT",
            "BY",
            "CLASS",
            "COUNT",
            "DEFAULT",
            "EACH",
            "EVAL",
            "FROM",
            "HAS",
            "IF",
            "MAX",
            "NOT",
            "OR",
            "RANK",
            "THEN",
            "X",
            // Property-value keywords; the tokenizer takes these too.
            "Metric",
            "Number",
            "Requirement",
        )

    /** Returns the [ClassName] for the given string. */
    public fun cn(name: String): ClassName = ClassName(name)

    private const val CLASS_NAME_PATTERN = "[A-Z][A-Za-z0-9_]*"
    private val classNameRegex = Regex(CLASS_NAME_PATTERN)

    internal fun parser(): com.github.h0tk3y.betterParse.parser.Parser<ClassName> =
        Parsing.className
  }

  init {
    require(asString.matches(classNameRegex)) { "Bad class name: $asString" }
    require(asString !in reservedNames) {
      "Pets keyword cannot be a class name: $asString"
    }
  }

  /**
   * Returns the expression having this class name as its [Expression.className], extracting
   * [HasExpression.expression] from each argument (not [HasExpression.expressionFull]).
   */
  public fun of(arguments: List<HasExpression>): Expression =
      expression.appendArguments(arguments.expressions())

  /** Vararg form of [of]. */
  public fun of(vararg arguments: HasExpression): Expression = of(arguments.toList())

  /** Returns the expression consisting of this class name alone, with no argument list. */
  public fun of(): Expression = expression

  /**
   * Returns the expression having this class name as its [Expression.className], no arguments, and
   * [refinement] as its [Expression.refinement] (or no refinement if [refinement] is `null`). For
   * example, if `bt` is the requirement `2 BuildingTag`, then `cn("CardFront").has(bt)` is the
   * expression `CardFront(HAS 2 BuildingTag)`.
   */
  public fun has(refinement: Requirement?): Expression = expression.has(refinement)

  /**
   * For the class name `Foo`, returns the class literal `Class<Foo>`. A class literal is written
   * with one bare class name ([rule
   * L3-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions),
   * [rule T4-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#4-class-literals)).
   */
  public fun classExpression(): Expression = CLASS.of(this)

  override val kind: kotlin.reflect.KClass<out PetNode> = ClassName::class

  override fun visitChildren(visitor: Visitor): Unit = Unit

  override val expression: Expression = Expression(this)
  override val expressionFull: Expression
    get() = expression

  override fun equals(other: Any?): Boolean = other is ClassName && other.asString == asString

  override fun hashCode(): Int = asString.hashCode() xor 1994079235

  override fun toString(): String = asString

  override fun compareTo(other: ClassName): Int = asString.compareTo(other.asString)

  internal object Parsing : PetTokenizer() {
    private val mixedCaseName = _mixedCaseClassNameRE map { cn(it.text) }
    private val allCapsName = _allCapsWordRE map { cn(it.text) }
    val className = mixedCaseName or allCapsName
  }
}
