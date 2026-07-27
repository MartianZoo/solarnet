package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency

/** One closed set of mutually compatible loaded [Class]es and resolved [Type]s. */
public abstract class TypeUniverse {
  /** The `Component` class, which is the root of the class hierarchy. */
  public abstract val componentClass: Class

  /** The `Class` class, the other class that is required to exist. */
  public abstract val classClass: Class

  /** Every class in this universe. */
  public abstract fun allClasses(): Set<Class>

  public abstract val allClassNamesAndIds: Set<ClassName>

  /** Returns the [Class] whose [Class.className] or [Class.shortName] is [name], if present. */
  public abstract fun findClass(name: ClassName): Class?

  /** Returns the [Class] whose [Class.className] or [Class.shortName] is [name], or throws. */
  public fun getClass(name: ClassName): Class =
      findClass(name) ?: throw Exceptions.classNotFound(name)

  /** Returns the [Type] represented by [expression]. */
  public abstract fun resolve(expression: Expression): Type

  /** Resolves every type expression in [node], throwing if any is invalid. */
  public fun checkAllTypes(node: PetNode): Unit = node.visitDescendants {
    if (it is Expression) {
      resolve(it.uncomplemented()).expression
      false
    } else {
      true
    }
  }

  /**
   * Tests [candidate] against [constraint] within [domain]. The explicit domain lets a complement
   * expression act as a constraint without pretending that it has a standalone type.
   */
  public fun matchesConstraint(
      candidate: Type,
      constraint: Expression,
      domain: Type,
      info: TypeInfo,
  ): Boolean {
    require(candidate.typeUniverse === this && domain.typeUniverse === this) {
      "constraint types belong to a different type universe"
    }
    val key = Key(domain.className, 0)
    val domainDependency = TypeDependency(key, domain)
    val constrained = domainDependency.intersect(constraint) ?: return false
    return TypeDependency(key, candidate).narrows(constrained, info)
  }

  public fun isUnresolvedClassLiteral(expression: Expression): Boolean {
    if (expression.className != CLASS) return false
    val argument = expression.arguments.singleOrNull()?.takeIf(Expression::simple) ?: return false
    return findClass(argument.className) == null
  }
}
