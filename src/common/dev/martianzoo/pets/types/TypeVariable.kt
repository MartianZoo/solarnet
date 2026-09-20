package dev.martianzoo.pets.types

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration as SyntaxDeclaration

/**
 * One authored type variable: a shared choice with one [declaration] and zero or more [usages],
 * independent of the class name used to spell it. Its identity and resolved [bound] follow
 * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
 */
public class TypeVariable
internal constructor(
    /**
     * The ground-type constraint, including any refinement, through which ordinary type operations
     * interpret this variable ([rule
     * T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
     */
    public val bound: GroundType,
    declarationSite: Site,
    usageSites: List<Site>,
) : Type {
  /**
   * Returns [bound], the variable's resolved interpretation under
   * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  override val groundType: GroundType
    get() = bound

  /**
   * Returns this variable's identity, distinguishing it from a ground type under
   * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  override val typeVariable: TypeVariable
    get() = this

  /**
   * The authored expression at [declaration], which is the variable's canonical spelling in this
   * scope ([rule
   * T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
   */
  override val expression: Expression
    get() = declaration.expression

  /**
   * The occurrence that introduces this variable under
   * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public val declaration: Declaration = Declaration(this, declarationSite)

  /** The explicit source handle written with `^`, or null for an unmarked header variable. */
  public val name: String? = (declaration.expression.typeVariableName as? SyntaxDeclaration)?.name

  /**
   * Every other occurrence interpreted as a use of the same choice, in authored order ([rule
   * T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
   */
  public val usages: List<Usage> = usageSites.map { Usage(this, it) }

  /**
   * [declaration] followed by [usages]. The usages preserve their authored order; an observing use
   * may precede the declaration within the same settlement region under
   * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
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
   * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   *
   * @constructor Creates an occurrence tied to one shared [typeVariable] identity and one authored
   *   site under
   *   [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public sealed class Occurrence
  protected constructor(
      /**
       * The shared variable identity represented by this occurrence ([rule
       * T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
       */
      final override val typeVariable: TypeVariable,
      private val site: Site,
  ) : Type {
    /**
     * This occurrence's resolved interpretation, preserving any occurrence-specific arguments under
     * [rule T13-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
     */
    final override val groundType: GroundType
      get() = site.interpretedGroundType ?: typeVariable.bound

    /**
     * The exact authored expression recorded for this occurrence, as required for scoped binding by
     * [rule T13-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
     */
    final override val expression: Expression
      get() = site.expression

    /**
     * The zero-based choice-region index used by the region rules in
     * [rules T13-6 and T13-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
     */
    public val region: Int
      get() = site.region

    /**
     * The zero-based source-order index that preserves authored occurrence order under
     * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
     */
    public val ordinal: Int
      get() = site.ordinal

    internal fun expressionFor(
        binding: GroundType,
        source: Expression,
        classTable: ClassTable = binding.classTable,
    ): Expression {
      fun retainNestedVariableNames(target: Expression, authored: Expression): Expression {
        val targetClass = classTable.getClass(target.className)
        val authoredClass = classTable.getClass(authored.className)
        val authoredByKey =
            authored.arguments
                .zip(authoredClass.matchDependencyKeys(authored.arguments, classTable))
                .associate { (argument, key) -> key to argument }
        val targetArguments =
            target.arguments
                .zip(targetClass.matchDependencyKeys(target.arguments, classTable))
                .map { (argument, key) ->
                  authoredByKey[key]?.let { authoredArgument ->
                    retainNestedVariableNames(argument, authoredArgument)
                        .copy(
                            typeVariableName =
                                authoredArgument.typeVariableName ?: argument.typeVariableName
                        )
                  } ?: argument
                }
        return target.copy(arguments = targetArguments)
      }

      val expression = retainNestedVariableNames(binding.expression, source)
      val representedKeys =
          binding.rootClass.matchDependencyKeys(expression.arguments, classTable).toSet()
      val sourceClass = classTable.getClass(source.className)
      val sourceArguments =
          if (
              source.typeVariableName is Expression.TypeVariableName.Reference &&
                  !source.argumentsSpecified
          ) {
            emptyList()
          } else {
            source.arguments.zip(sourceClass.matchDependencyKeys(source.arguments, classTable))
          }
      val openKeys = binding.rootClass.argumentDependencies.keys.toSet()
      val retainedArguments = sourceArguments.filter { (_, key) ->
        key in openKeys && key !in representedKeys
      }
      val applied = expression.appendArguments(retainedArguments.map { it.first })
      return if (
          source.argumentsSpecified && source.arguments.isEmpty() && !applied.argumentsSpecified
      ) {
        applied.copy(argumentsSpecified = true)
      } else {
        applied
      }
    }
  }

  /**
   * The occurrence that introduces its [typeVariable], as defined by
   * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public class Declaration internal constructor(variable: TypeVariable, site: Site) :
      Occurrence(variable, site)

  /**
   * An occurrence that reuses an existing [typeVariable], as defined by
   * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public class Usage internal constructor(variable: TypeVariable, site: Site) :
      Occurrence(variable, site)

  /**
   * Returns the declaration spelling; variable identity remains its declaration and scope under
   * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  override fun toString(): String = name ?: "${declaration.expression}"
}
