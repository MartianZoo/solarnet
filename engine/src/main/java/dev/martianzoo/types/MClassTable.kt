package dev.martianzoo.types

import dev.martianzoo.api.Type
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression

public abstract class MClassTable {

  internal abstract val authority: TfmAuthority
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
}
