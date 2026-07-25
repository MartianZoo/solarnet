package dev.martianzoo.types

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.Type
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression

public abstract class MClassTable {
  /** The `Component` class, which is the root of the class hierarchy. */
  public abstract val componentClass: MClass

  /** The `Class` class, the other class that is required to exist. */
  public abstract val classClass: MClass

  /** All classes loaded by this class loader; can only be accessed after the loader is frozen. */
  abstract fun allClasses(): Set<MClass>

  public abstract val allClassNamesAndIds: Set<ClassName>

  /**
   * Returns the [MClass] whose [MClass.className] or [MClass.shortName] is [name], or throws an
   * exception.
   */
  abstract fun getClass(name: ClassName): MClass

  /** Returns the [MType] represented by [expression]. */
  abstract fun resolve(expression: Expression): MType

  /** Returns the corresponding [MType] to [type] (possibly [type] itself). */
  abstract fun resolve(type: Type): MType

  public fun isUnresolvedClassLiteral(expression: Expression): Boolean {
    if (expression.className != CLASS) return false
    val argument = expression.arguments.singleOrNull()?.takeIf(Expression::simple) ?: return false
    return try {
      getClass(argument.className)
      false
    } catch (_: ExpressionException) {
      true
    }
  }
}
