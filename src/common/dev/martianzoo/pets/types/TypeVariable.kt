package dev.martianzoo.pets.types

import dev.martianzoo.pets.ast.Expression

/**
 * One authored type variable: a shared choice with one [declaration] and zero or more [usages],
 * independent of the class name used to spell it. Its identity and structural [bound] follow
 * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
 */
public class TypeVariable
internal constructor(
    /**
     * The ground-type constraint through which ordinary type operations interpret this variable
     * ([rule
     * 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
     */
    public val bound: GroundType,
    declarationSite: Site,
    usageSites: List<Site>,
) : Type {
  /**
   * Returns [bound], the variable's structural interpretation under
   * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  override val groundType: GroundType
    get() = bound

  /**
   * Returns this variable's identity, distinguishing it from a ground type under
   * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  override val typeVariable: TypeVariable
    get() = this

  /**
   * The authored expression at [declaration], which is the variable's canonical spelling in this
   * scope ([rule
   * 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
   */
  override val expression: Expression
    get() = declaration.expression

  /**
   * The first authored occurrence, which introduces this variable under
   * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public val declaration: Declaration = Declaration(this, declarationSite)

  /**
   * Every later occurrence interpreted as a use of the same choice, in authored order ([rule
   * 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
   */
  public val usages: List<Usage> = usageSites.map { Usage(this, it) }

  /**
   * [declaration] followed by [usages], preserving the authored order required by
   * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public val occurrences: List<Occurrence> = listOf(declaration) + usages

  internal data class Site(
      val expression: Expression,
      val region: Int,
      val ordinal: Int,
      val interpretedGroundType: GroundType? = null,
  )

  /**
   * One declaration or usage occurrence of a type variable. Each occurrence is a type view of the
   * same variable but retains its own authored expression, region, and position, as specified by
   * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   *
   * @constructor Creates an occurrence tied to one shared [typeVariable] identity and one authored
   *   site under
   *   [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public sealed class Occurrence
  protected constructor(
      /**
       * The shared variable identity represented by this occurrence ([rule
       * 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
       */
      final override val typeVariable: TypeVariable,
      private val site: Site,
  ) : Type {
    /**
     * This occurrence's structural interpretation, preserving any occurrence-specific arguments
     * under
     * [rule 13-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
     */
    final override val groundType: GroundType
      get() = site.interpretedGroundType ?: typeVariable.bound

    /**
     * The exact authored expression recorded for this occurrence, as required for scoped binding by
     * [rule 13-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
     */
    final override val expression: Expression
      get() = site.expression

    /**
     * The zero-based choice-region index used by the region rules in
     * [rules 13-6 and 13-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
     */
    public val region: Int
      get() = site.region

    /**
     * The zero-based source-order index that preserves authored occurrence order under
     * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
     */
    public val ordinal: Int
      get() = site.ordinal

    internal fun expressionFor(binding: GroundType, source: Expression): Expression {
      val expression = binding.expression
      val representedKeys = binding.rootClass.matchDependencyKeys(expression.arguments).toSet()
      val sourceClass = binding.classTable.getClass(source.className)
      val sourceArguments = source.arguments.zip(sourceClass.matchDependencyKeys(source.arguments))
      val retainedArguments = sourceArguments.filterNot { (_, key) -> key in representedKeys }
      return expression.appendArguments(retainedArguments.map { it.first })
    }
  }

  /**
   * The occurrence that introduces its [typeVariable], as defined by
   * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public class Declaration internal constructor(variable: TypeVariable, site: Site) :
      Occurrence(variable, site)

  /**
   * An occurrence that reuses an existing [typeVariable], as defined by
   * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public class Usage internal constructor(variable: TypeVariable, site: Site) :
      Occurrence(variable, site)

  /**
   * Returns the declaration spelling; variable identity remains its declaration and scope under
   * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  override fun toString(): String = "${declaration.expression}"
}
