package dev.martianzoo.pets.types

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement

/**
 * A [type](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types):
 * either a ground type or one authored occurrence of a type variable. A ground type may carry
 * refinements, including state-dependent refinements; those are part of the type.
 *
 * Consumers interested only in resolved meaning can use [groundType]. Consumers interpreting
 * authored syntax can inspect [typeVariable] without maintaining a parallel representation. This is
 * the two-form model specified by
 * [rules 5-8 and 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
 */
public interface Type : HasExpression, HasClassName, Specification<Type> {
  /**
   * This type's resolved interpretation after forgetting variable identity, as specified by
   * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public val groundType: GroundType

  /**
   * The authored variable represented by this occurrence, or null for a ground type ([rule
   * 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables)).
   */
  public val typeVariable: TypeVariable?
    get() = null

  /**
   * The nominal root class of this type ([rule
   * 5-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types)).
   */
  public val rootClass: Class
    get() = groundType.rootClass

  /**
   * The canonical name of [rootClass], following
   * [rule 2-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
   */
  override val className: ClassName
    get() = rootClass.className

  /**
   * The master universe to which this type belongs. Type-system operations reject values from
   * different universes as specified by
   * [rule 1-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  public val classTable: ClassTable
    get() = groundType.classTable

  /**
   * The bound for every dependency key of [rootClass], as specified by
   * [rules 3-2 and 3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public val dependencies: DependencySet
    get() = groundType.dependencies

  /**
   * The optional predicate filtering this type's structural domain ([section
   * 8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#8-refinements)).
   */
  public val refinement: Expression.Refinement?
    get() = groundType.refinement

  /**
   * Whether this type cannot directly describe a component, according to
   * [rule 5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  public val abstract: Boolean
    get() = groundType.abstract

  /**
   * The dependencies that target components; the represented-class slot of a class literal is
   * excluded by
   * [rule 4-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#4-class-literals).
   */
  public val typeDependencies: List<Dependency.TypeDependency>
    get() = groundType.typeDependencies

  /**
   * The class named by a class literal, or null for every other type ([rule
   * 4-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#4-class-literals)).
   */
  public val representedClass: Class?
    get() = groundType.representedClass

  /**
   * Only the dependency bounds narrower than [rootClass]'s base type ([rule
   * 3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies)).
   */
  public val narrowedDependencies: DependencySet
    get() = groundType.narrowedDependencies

  /**
   * The canonical prefix expression that resolves to this type ([rule
   * 5-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types)).
   */
  override val expression: Expression
    get() = groundType.expression

  /**
   * The expression containing every dependency bound in key order ([rule
   * 5-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types)).
   */
  override val expressionFull: Expression
    get() = groundType.expressionFull

  /**
   * Implements the structural abstractness rule
   * [5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types);
   * [info] is not consulted.
   */
  override fun isAbstract(info: TypeInfo): Boolean = groundType.isAbstract(info)

  /**
   * Asserts the contextual narrowing relation of
   * [rules 6-1 and 6-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping),
   * using [info] only for a state-dependent refinement.
   *
   * @throws NarrowingException if this type does not narrow [that] in [info].
   * @throws IllegalArgumentException if [that] belongs to another universe (rule 1-2).
   */
  override fun ensureNarrows(that: Type, info: TypeInfo): Unit =
      groundType.ensureNarrows(that.groundType, info)

  /**
   * Tests the contextual narrowing relation of
   * [rules 6-1 and 6-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping),
   * using [info] only for a state-dependent refinement.
   *
   * @throws IllegalArgumentException if [that] belongs to another universe (rule 1-2).
   */
  override fun narrows(that: Type, info: TypeInfo): Boolean =
      groundType.narrows(that.groundType, info)

  /**
   * Tests context-free subtyping. A comparison that needs a world fails rather than guessing, per
   * [rules 6-1 and 8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   *
   * @throws IllegalStateException if a state-dependent refinement requires a world.
   * @throws IllegalArgumentException if [that] belongs to another universe (rule 1-2).
   */
  public fun isSubtypeOf(that: Type): Boolean = narrows(that, NoGameState)

  /**
   * The converse of [isSubtypeOf], as defined by
   * [rule 6-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  public fun isSupertypeOf(that: Type): Boolean = that.isSubtypeOf(this)

  /**
   * The greatest lower bound of this type and [that], or null when it is absent ([rule
   * 7-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#7-bounds)).
   *
   * @throws IllegalArgumentException if [that] belongs to another universe (rule 1-2).
   */
  public infix fun glb(that: Type): GroundType? = groundType glb that.groundType

  /**
   * Enumerates every concrete narrowing in the master universe, following
   * [rules 11-1 and 11-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  public fun allConcreteSubtypes(): Sequence<GroundType> = groundType.allConcreteSubtypes()

  /**
   * Returns the sole concrete narrowing in the master universe when every structural choice is
   * unique and its refinement accepts [info], as specified by
   * [rule 11-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  public fun singleConcreteSubtype(info: TypeInfo): GroundType? =
      groundType.singleConcreteSubtype(info)

  /**
   * Returns the concrete numeric value of [propertyName], under the property-reading contract of
   * [rule 9-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties).
   */
  public fun getNumberPropertyValue(propertyName: String): Int =
      groundType.getNumberPropertyValue(propertyName)

  /**
   * Returns the concrete metric value of [propertyName], under the property-reading contract of
   * [rule 9-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties).
   */
  public fun getMetricPropertyValue(propertyName: String): Metric =
      groundType.getMetricPropertyValue(propertyName)

  /**
   * Returns the concrete requirement value of [propertyName], or null for an absent optional, under
   * [rule 9-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties).
   */
  public fun getRequirementPropertyValue(propertyName: String): Requirement? =
      groundType.getRequirementPropertyValue(propertyName)

  /**
   * Captures the values this type supplies for selected class-header [variables] when specializing
   * [general], following
   * [rule 13-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun variableBindingsFrom(
      general: Type,
      variables: Iterable<TypeVariable>,
  ): Map<TypeVariable, GroundType> = groundType.variableBindingsFrom(general.groundType, variables)
}
