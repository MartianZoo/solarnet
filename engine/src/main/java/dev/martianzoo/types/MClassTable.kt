package dev.martianzoo.types

import dev.martianzoo.api.Type
import dev.martianzoo.data.Authority
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression

public abstract class MClassTable {

  internal abstract val authority: Authority

  /** The `Component` class, which is the root of the class hierarchy. */
  internal abstract val componentClass: MClass

  /** The `Class` class, the other class that is required to exist. */
  internal abstract val classClass: MClass

  /** All classes loaded by this class loader; can only be accessed after the loader is frozen. */
  abstract fun allClasses(): Set<MClass>

  internal abstract val allClassNamesAndIds: Set<ClassName>

  /**
   * Returns the [MClass] whose [MClass.className] or [MClass.shortName] is [name], or throws an
   * exception.
   */
  abstract fun getClass(name: ClassName): MClass

  /** Returns the [MType] represented by [expression]. */
  abstract fun resolve(expression: Expression): MType

  /** Returns the corresponding [MType] to [type] (possibly [type] itself). */
  abstract fun resolve(type: Type): MType
}
