package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression

internal abstract class MClassTable {

  abstract val authority: Authority
  /** The `Component` class, which is the root of the class hierarchy. */
  abstract val componentClass: MClass

  /** The `Class` class, the other class that is required to exist. */
  abstract val classClass: MClass

  /** All classes loaded by this class loader; can only be accessed after the loader is frozen. */
  abstract val allClasses: Set<MClass>

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

  internal abstract fun defaults(className: ClassName): Defaults
}
