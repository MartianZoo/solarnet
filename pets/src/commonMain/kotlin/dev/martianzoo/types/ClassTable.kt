package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.SystemClasses.AUTO_LOAD
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.GamePremise
import dev.martianzoo.pets.ClassSynonyms
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency

/** One closed set of mutually compatible active and authority-known phantom [Class]es. */
public abstract class ClassTable {
  public companion object {
    /** Loads and freezes the classes activated by [premise]. */
    public fun forPremise(
        premise: GamePremise,
        classSynonyms: ClassSynonyms = ClassSynonyms.NONE,
    ): ClassTable {
      val ruleset = premise.ruleset

      fun isAutoLoad(declaration: ClassDeclaration): Boolean =
          declaration.className == AUTO_LOAD ||
              declaration.supertypes.any {
                isAutoLoad(ruleset.classDeclaration(it.className))
              }

      val rootClassNames =
          premise.actors.classNames() +
              premise.rootClassNames +
              ruleset.allClassDeclarations.filterValues(::isAutoLoad).keys

      return ClassLoader(ruleset, classSynonyms).apply { rootClassNames.forEach(::load) }.freeze()
    }
  }

  /** The `Component` class, which is the root of the class hierarchy. */
  public abstract val componentClass: Class

  /** The `Class` class, the other class that is required to exist. */
  public abstract val classClass: Class

  /** Every active class in this table; phantom classes are deliberately not enumerated. */
  public abstract fun allClasses(): Set<Class>

  /** Every active class's full names and programmatic ids; phantom names are excluded. */
  public abstract val allClassNamesAndIds: Set<ClassName>

  /** Returns the active or phantom [Class] having this name, id, or configured synonym. */
  public abstract fun findClass(name: ClassName): Class?

  /** Returns the [Class] having this name, id, or configured synonym, or throws. */
  public fun getClass(name: ClassName): Class =
      findClass(name) ?: throw Exceptions.classNotFound(name)

  /** Returns the active [Class] having this name, id, or synonym; null if unknown or phantom. */
  public fun findActiveClass(name: ClassName): Class? = findClass(name)?.takeUnless(Class::phantom)

  /** Whether [name] names a class that is active in this table (not unknown, not phantom). */
  public fun isActive(name: ClassName): Boolean = findActiveClass(name) != null

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
    require(candidate.classTable === this && domain.classTable === this) {
      "constraint types belong to a different class table"
    }
    val key = Key(domain.className, 0)
    val domainDependency = TypeDependency(key, domain)
    val constrained = domainDependency.intersect(constraint) ?: return false
    return TypeDependency(key, candidate).narrows(constrained, info)
  }
}
