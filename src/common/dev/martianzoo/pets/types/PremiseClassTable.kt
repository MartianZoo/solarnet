package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.util.associateByStrict

/**
 * The small declaration table owned by one game premise. It imports exactly one immutable master
 * table; master declarations cannot depend on this table, and premise names cannot replace master
 * names.
 */
public class PremiseClassTable(
    /** The reusable declaration-derived class model imported by this premise. */
    public val master: ClassTable,

    /** Generated Players, the generated Premise, and any ad-hoc declarations for this game. */
    declarations: Set<ClassDeclaration>,
) {
  /** Every declaration introduced only by this premise, keyed by its canonical Class Name. */
  public val declarations: Map<ClassName, ClassDeclaration> =
      declarations.associateByStrict(ClassDeclaration::className)

  init {
    val collisions = this.declarations.keys intersect master.allClassNames
    require(collisions.isEmpty()) {
      "premise Class Names collide with the master table: $collisions"
    }
  }

  /** Tests the nominal relation using this table's one-way imports without compiling Classes. */
  public fun isSubtypeOf(candidate: ClassName, superclass: ClassName): Boolean {
    val masterCandidate = master.findClass(candidate)
    val masterSuperclass = master.findClass(superclass)
    if (masterCandidate != null) {
      return masterSuperclass != null && masterCandidate.isSubtypeOf(masterSuperclass)
    }
    require(candidate in declarations) { "unknown premise Class Name: $candidate" }
    require(masterSuperclass != null || superclass in declarations) {
      "unknown superclass name: $superclass"
    }

    fun reaches(name: ClassName, visited: Set<ClassName>): Boolean {
      if (name == superclass) return true
      if (name in visited) return false
      val declaration = declarations[name] ?: return false
      val directSupertypeNames =
          if (declaration.supertypes.isEmpty()) setOf(COMPONENT)
          else declaration.supertypes.mapTo(linkedSetOf()) { it.className }
      return directSupertypeNames.any { supertypeName ->
        supertypeName == superclass ||
            (masterSuperclass != null &&
                master.findClass(supertypeName)?.isSubtypeOf(masterSuperclass) == true) ||
            reaches(supertypeName, visited + name)
      }
    }
    return reaches(candidate, emptySet())
  }
}
