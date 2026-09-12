package dev.martianzoo.pets.types

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression

/**
 * One keyed bound in a type's
 * [dependency set](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
 * The key is the dependency's identity; the bound is covariant and belongs to the same universe as
 * its containing type. Combining dependencies from different universes throws
 * [IllegalArgumentException], implementing the universe-mismatch failure in rule T1-2.
 *
 * @constructor Creates one implementation of the dependency concept in
 *   [section 3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
 */
public sealed class Dependency : Specification<Dependency>, HasExpression, HasClassName {
  /**
   * The declaring-class-and-slot identity specified by
   * [rule T3-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public abstract val key: Key

  /**
   * Whether the dependency bound admits more than one structural possibility, contributing to type
   * abstractness under
   * [rule T5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  public abstract val abstract: Boolean

  /**
   * Returns [abstract]; dependency abstractness is structural and does not consult [info] ([rule
   * T5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types)).
   */
  override fun isAbstract(info: TypeInfo): Boolean = abstract

  /**
   * Tests covariant narrowing of this dependency's bound against [that], following
   * [rules T6-2 and T6-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  public abstract fun isSubtypeOf(that: Dependency): Boolean

  /**
   * Tests the converse of [isSubtypeOf], including equality ([rule
   * T6-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping)).
   */
  public fun isSupertypeOf(that: Dependency): Boolean = that.isSubtypeOf(this)

  /**
   * Returns the greatest lower bound with [that], or null when absent, by componentwise bound
   * intersection ([rule
   * T7-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#7-bounds)).
   */
  public abstract infix fun glb(that: Dependency): Dependency?

  /**
   * The stable identity of a dependency: the class that introduced it and its zero-based slot in
   * that declaration. Subclasses inherit this key unchanged, as specified by
   * [rules T3-1 and T3-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   *
   * @constructor Combines [declaringClass] and [index] into the identity specified by
   *   [rule T3-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public data class Key(
      /**
       * The class that originally declared this dependency, not a subclass that merely narrowed it
       * ([rule
       * T3-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies)).
       */
      public val declaringClass: ClassName,

      /**
       * The zero-based slot in [declaringClass]'s declared dependency list ([rule
       * T3-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies)).
       */
      public val index: Int,
  ) {
    init {
      require(index >= 0)
    }

    /**
     * Renders this key in the canonical `DeclaringClass_index` form specified by
     * [rule T3-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
     */
    override fun toString(): String = "${declaringClass}_$index"
  }

  internal abstract val boundClass: Class

  /**
   * Tests contextual narrowing of this dependency against [that], propagating [info] to a refined
   * bound under
   * [rules T6-2 and T8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  public abstract override fun narrows(that: Dependency, info: TypeInfo): Boolean

  internal abstract fun intersect(expression: Expression): Dependency?

  /**
   * An ordinary component-targeting dependency whose [boundType] identifies the possible target
   * types. This is the dependency edge defined by
   * [section 3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies),
   * excluding the represented-class slot of a class literal (rule T4-7).
   *
   * @constructor Associates [key] with its component-targeting [boundType] under
   *   [section 3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public data class TypeDependency(
      /**
       * The stable identity inherited under
       * [rule T3-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
       */
      override val key: Key,

      /**
       * The covariant target bound described by
       * [rules T3-2 through T3-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
       */
      val boundType: GroundType,
  ) : Dependency(), HasExpression by boundType {

    override val boundClass: Class
      get() = boundType.rootClass

    /**
     * The canonical name of [boundType]'s root class, following
     * [rule T2-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
     */
    override val className: ClassName
      get() = boundClass.className

    internal fun allConcreteSpecializations(): Sequence<TypeDependency> =
        boundType.allConcreteSubtypes().map { TypeDependency(key, it) }

    /**
     * The canonical `key=full-bound` rendering of this dependency ([rules T3-1 and
     * T5-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies)).
     */
    override fun toString(): String = "$key=$expressionFull"

    // Hierarchy

    /**
     * Whether [boundType] is abstract, contributing to the containing type's abstractness under
     * [rule T5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
     */
    override val abstract: Boolean
      get() = boundType.abstract

    /**
     * Tests context-free covariance of [boundType] against [that] ([rule
     * T6-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping)).
     */
    override fun isSubtypeOf(that: Dependency): Boolean = boundType.isSubtypeOf(boundOf(that))

    /**
     * Intersects [boundType] with [that]'s bound, following
     * [rule T7-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#7-bounds).
     */
    override fun glb(that: Dependency): Dependency? {
      if (that !is TypeDependency) return null
      return (boundType glb boundOf(that))?.let { copy(boundType = it) }
    }

    internal inline fun map(function: (GroundType) -> GroundType) =
        copy(boundType = function(boundType))

    override fun intersect(expression: Expression): Dependency? {
      return glb(copy(boundType = boundType.classTable.resolve(expression)))
    }

    /**
     * Asserts contextual covariance against [that], forwarding [info] to refinements under
     * [rules T6-2 and T8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
     */
    override fun ensureNarrows(that: Dependency, info: TypeInfo): Unit =
        boundType.ensureNarrows(boundOf(that), info)

    /**
     * Tests contextual covariance against [that], forwarding [info] to refinements under
     * [rules T6-2 and T8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
     */
    override fun narrows(that: Dependency, info: TypeInfo): Boolean =
        that is TypeDependency && boundType.narrows(boundOf(that), info)

    private fun boundOf(that: Dependency): GroundType =
        (that as TypeDependency).boundType.also { require(key == that.key) }
  }

  /**
   * A dependency used *only* by types of the class `Class`; for example `Class<Foo>` (in which
   * example `boundClass.name` is `"Foo"`). No other class can use this; for example, one cannot
   * declare that the dependency in `Production<Plant>` is a "class dependency" on `Plant`, so
   * instead we use `Production<Class<Plant>>`.
   */
  private data class FakeDependency(override val boundClass: Class) : Dependency() {
    override val key: Key = Key(CLASS, 0)

    override val className: ClassName
      get() = boundClass.className

    override val expression: Expression
      get() = className.expression

    override val expressionFull: Expression
      get() = expression

    override fun toString() = "$key=$expressionFull"

    // Hierarchy

    override val abstract: Boolean
      get() = boundClass.abstract

    override fun isSubtypeOf(that: Dependency) = boundClass.isSubtypeOf(boundOf(that))

    override fun glb(that: Dependency): FakeDependency? {
      if (that !is FakeDependency) return null
      return (boundClass glb boundOf(that))?.let(::copy)
    }

    override fun ensureNarrows(that: Dependency, info: TypeInfo) =
        boundClass.ensureNarrows(boundOf(that), info)

    override fun narrows(that: Dependency, info: TypeInfo) = boundClass.isSubtypeOf(boundOf(that))

    private fun boundOf(that: Dependency): Class =
        (that as FakeDependency).boundClass.also { require(key == that.key) }

    override fun intersect(expression: Expression): FakeDependency? {
      if (!expression.simple) return null
      val klass = boundClass.classTable.getClass(expression.className)
      return glb(FakeDependency(klass))
    }
  }

  internal companion object {
    // Note these don't really belong here; they're just here so that FakeDependency can be private

    internal fun validate(deps: List<Dependency>) {
      deps.indices.forEach { index ->
        for (previous in 0 until index) {
          require(deps[index].key != deps[previous].key) { "duplicate dependency keys: $deps" }
        }
      }
      require(deps.none { it is FakeDependency } || deps.single() is FakeDependency)
      val classTable = deps.firstOrNull()?.boundClass?.classTable
      require(deps.all { it.boundClass.classTable === classTable }) {
        "dependencies belong to different class tables"
      }
    }

    internal fun isForClassType(deps: List<Dependency>) = deps.singleOrNull() is FakeDependency

    internal fun getClassForClassType(deps: List<Dependency>): Class =
        (deps.single() as FakeDependency).boundClass

    internal fun depsForClassType(klass: Class) = DependencySet.of(listOf(FakeDependency(klass)))
  }
}
