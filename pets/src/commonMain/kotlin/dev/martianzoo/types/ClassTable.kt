package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.data.ClassSelection
import dev.martianzoo.data.GamePremise
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency

/** One closed set of mutually compatible active and authority-known phantom [Class]es. */
public abstract class ClassTable {
  public companion object {
    /** Loads and freezes the classes activated by [premise]. */
    public fun forPremise(premise: GamePremise): ClassTable {
      val initialClassNames =
          premise.initialComponentTypes.flatMap { it.descendantsOfType<ClassName>() }.toSet()
      val configurationNames: Set<ClassName> =
          premise.modules +
              premise.classSelections
                  .filter(ClassSelection::included)
                  .map(ClassSelection::className) +
              initialClassNames
      val moduleSelections =
          premise.modules.flatMap { premise.runtimeClassAuthority.modules.getValue(it) }
      val (applicableModuleSelections, inapplicableModuleSelections) =
          moduleSelections.partition { selection -> selection.appliesTo(configurationNames) }
      val moduleIncluded =
          applicableModuleSelections
              .filter(ClassSelection::included)
              .mapTo(linkedSetOf(), ClassSelection::className)
      val conditionallyExcluded =
          inapplicableModuleSelections
              .filter(ClassSelection::included)
              .mapTo(hashSetOf(), ClassSelection::className) - moduleIncluded
      val moduleExcluded =
          applicableModuleSelections
              .filterNot(ClassSelection::included)
              .mapTo(hashSetOf(), ClassSelection::className) + conditionallyExcluded
      val selectedByModules = moduleIncluded - moduleExcluded
      val explicitlyIncluded =
          premise.classSelections
              .filter(ClassSelection::included)
              .mapTo(linkedSetOf(), ClassSelection::className)
      val explicitlyExcluded =
          premise.classSelections
              .filterNot(ClassSelection::included)
              .mapTo(linkedSetOf(), ClassSelection::className)
      val excluded = (moduleExcluded - explicitlyIncluded) + explicitlyExcluded
      val roots =
          premise.modules +
              ((selectedByModules - explicitlyExcluded) + explicitlyIncluded) +
              initialClassNames

      val table =
          ClassLoader(premise.runtimeClassAuthority).apply { roots.forEach(::load) }.freeze()
      val unexpectedModules =
          premise.runtimeClassAuthority.modules.keys.filterTo(linkedSetOf()) {
            table.isActive(it)
          } - premise.modules
      require(unexpectedModules.isEmpty()) {
        "structural activation selected unrequested Modules: $unexpectedModules"
      }
      val reactivated = excluded.filterTo(linkedSetOf(), table::isActive)
      require(reactivated.isEmpty()) {
        "structural activation conflicts with excluded classes: $reactivated"
      }
      return table
    }
  }

  /** The `Component` class, which is the root of the class hierarchy. */
  public abstract val componentClass: Class

  /** The `Class` class, the other class that is required to exist. */
  public abstract val classClass: Class

  /** Every active class in this table; phantom classes are deliberately not enumerated. */
  public abstract fun allClasses(): Set<Class>

  /** Every active class's stable names; phantom names are excluded. */
  public abstract val allClassNames: Set<ClassName>

  /** Returns the active or phantom [Class] having this canonical name. */
  public abstract fun findClass(name: ClassName): Class?

  /** Returns the [Class] having this canonical name, or throws. */
  public fun getClass(name: ClassName): Class =
      findClass(name) ?: throw Exceptions.classNotFound(name)

  /** Returns the active [Class] having this canonical name; null if unknown or phantom. */
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
