package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression

public abstract class MClassTable {
  abstract val authority: Authority

  /** The `Component` class, which is the root of the class hierarchy. */
  abstract val componentClass: MClass

  /** The `Class` class, the other class that is required to exist. */
  abstract val classClass: MClass

  /** All classes loaded by this class loader; can only be accessed after the loader is frozen. */
  abstract val allClasses: Set<MClass>
  internal abstract val allClassNamesAndIds: Set<ClassName>

  abstract val transformers: Transformers

  /**
   * Returns the [MClass] whose [MClass.className] or [MClass.shortName] is [name], or throws an
   * exception.
   */
  abstract fun getClass(name: ClassName): MClass

  /** Returns the [MType] represented by [expression]. */
  abstract fun resolve(expression: Expression): MType

  /** Returns the corresponding [MType] to [type] (possibly [type] itself). */
  abstract fun resolve(type: Type): MType

  /**
   * For an example expression like `Foo<Bar, Qux>`, pass in `[Bar, Qux]` and Foo's base dependency
   * set. This method decides which dependencies in the dependency set each of these args should be
   * matched with. The returned dependency set will have [Dependency]s in the corresponding order to
   * the input expressions.
   *
   * DON'T call this for the <Foo> in Class<Foo>, it won't work.
   */
  internal abstract fun matchPartial(
      expressionArgs: List<Expression>,
      deps: DependencySet,
  ): DependencySet

  internal abstract fun defaults(className: ClassName): Defaults
}
