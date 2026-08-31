package dev.martianzoo.pets.types

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement

/**
 * A structural Type or an authored Type-variable declaration or usage.
 *
 * Code interested only in structural meaning can use [groundType]. Code interpreting authored
 * syntax can inspect [typeVariable] and the runtime subtype without maintaining a parallel model.
 */
public interface Type : HasExpression, HasClassName, Specification<Type> {
  /** This Type's structural interpretation after forgetting variable identity. */
  public val groundType: GroundType

  /** The authored variable represented here, or null for an ordinary Ground Type. */
  public val typeVariable: TypeVariable?
    get() = null

  public val rootClass: Class
    get() = groundType.rootClass

  override val className: ClassName
    get() = rootClass.className

  public val classTable: ClassTable
    get() = groundType.classTable

  public val dependencies: DependencySet
    get() = groundType.dependencies

  public val refinement: Expression.Refinement?
    get() = groundType.refinement

  public val abstract: Boolean
    get() = groundType.abstract

  public val typeDependencies: List<Dependency.TypeDependency>
    get() = groundType.typeDependencies

  public val representedClass: Class?
    get() = groundType.representedClass

  public val narrowedDependencies: DependencySet
    get() = groundType.narrowedDependencies

  override val expression: Expression
    get() = groundType.expression

  override val expressionFull: Expression
    get() = groundType.expressionFull

  override fun isAbstract(info: TypeInfo): Boolean = groundType.isAbstract(info)

  override fun ensureNarrows(that: Type, info: TypeInfo): Unit =
      groundType.ensureNarrows(that.groundType, info)

  override fun narrows(that: Type, info: TypeInfo): Boolean =
      groundType.narrows(that.groundType, info)

  public fun isSubtypeOf(that: Type): Boolean = narrows(that, NoGameState)

  public fun isSupertypeOf(that: Type): Boolean = that.isSubtypeOf(this)

  public infix fun glb(that: Type): GroundType? = groundType glb that.groundType

  public infix fun lub(that: Type): GroundType = groundType lub that.groundType

  public fun allConcreteSubtypes(): Sequence<GroundType> = groundType.allConcreteSubtypes()

  public fun singleConcreteSubtype(info: TypeInfo): GroundType? =
      groundType.singleConcreteSubtype(info)

  public fun getNumberPropertyValue(propertyName: String): Int =
      groundType.getNumberPropertyValue(propertyName)

  public fun getMetricPropertyValue(propertyName: String): Metric =
      groundType.getMetricPropertyValue(propertyName)

  public fun getRequirementPropertyValue(propertyName: String): Requirement? =
      groundType.getRequirementPropertyValue(propertyName)

  /** Ground-Type values captured when this Type specializes [general]. */
  public fun variableBindingsFrom(
      general: Type,
      variables: Iterable<TypeVariable>,
  ): Map<TypeVariable, GroundType> = groundType.variableBindingsFrom(general.groundType, variables)
}
