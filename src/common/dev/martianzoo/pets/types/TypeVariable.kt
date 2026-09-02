package dev.martianzoo.pets.types

import dev.martianzoo.pets.ast.Expression

/** One authored Type variable, independent of the spelling used to refer to it. */
public class TypeVariable
internal constructor(
    /** The structural constraint on values captured by this variable. */
    public val bound: GroundType,
    declarationSite: Site,
    usageSites: List<Site>,
) : Type {
  override val groundType: GroundType
    get() = bound

  override val typeVariable: TypeVariable
    get() = this

  override val expression: Expression
    get() = declaration.expression

  /** The occurrence that introduces this variable. */
  public val declaration: Declaration = Declaration(this, declarationSite)

  /** Every later occurrence interpreted as a use of this variable. */
  public val usages: List<Usage> = usageSites.map { Usage(this, it) }

  /** All occurrences in authored order, including the declaration. */
  public val occurrences: List<Occurrence> = listOf(declaration) + usages

  internal data class Site(
      val expression: Expression,
      val region: Int,
      val ordinal: Int,
      val complementedUse: Boolean = false,
      val interpretedGroundType: GroundType? = null,
  )

  /** One declaration or usage of a Type variable in authored syntax. */
  public sealed class Occurrence
  protected constructor(
      final override val typeVariable: TypeVariable,
      private val site: Site,
  ) : Type {
    final override val groundType: GroundType
      get() = site.interpretedGroundType ?: typeVariable.bound

    final override val expression: Expression
      get() = site.expression

    /** Zero-based choice-region index within the declaring scope. */
    public val region: Int
      get() = site.region

    /** Zero-based source-order index within the declaring scope. */
    public val ordinal: Int
      get() = site.ordinal

    internal fun expressionFor(binding: GroundType, source: Expression): Expression {
      val expression = binding.expression
      val representedKeys = binding.rootClass.matchDependencyKeys(expression.arguments).toSet()
      val sourceClass = binding.classTable.getClass(source.className)
      val sourceArguments = source.arguments.zip(sourceClass.matchDependencyKeys(source.arguments))
      val retainedArguments = sourceArguments.filterNot { (_, key) -> key in representedKeys }
      val constrained = expression.appendArguments(retainedArguments.map { it.first })
      return if (site.complementedUse) {
        constrained.copy(complement = !constrained.complement)
      } else {
        constrained
      }
    }

    internal val appliesComplementOperator: Boolean
      get() = site.complementedUse
  }

  /** The occurrence that introduces its [typeVariable]. */
  public class Declaration internal constructor(variable: TypeVariable, site: Site) :
      Occurrence(variable, site)

  /** An occurrence that refers to an existing [typeVariable]. */
  public class Usage internal constructor(variable: TypeVariable, site: Site) :
      Occurrence(variable, site)

  override fun toString(): String = "${declaration.expression}"
}
