package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.data.ClassDeclaration

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
      declarations.associateBy(ClassDeclaration::className).also { byName ->
        if (byName.size != declarations.size) {
          val duplicates =
              declarations.groupingBy(ClassDeclaration::className).eachCount().filterValues {
                it > 1
              }
          throw InvalidGameConfigException(
              "premise contains duplicate class declarations: ${duplicates.keys.joinToString { "`$it`" }}"
          )
        }
      }

  init {
    val collisions = this.declarations.keys intersect master.allClassNames
    if (collisions.isNotEmpty()) {
      throw InvalidGameConfigException(
          "premise class names collide with the master table: ${collisions.joinToString { "`$it`" }}"
      )
    }
  }

  /** Tests the nominal relation using this table's one-way imports without compiling Classes. */
  public fun isSubtypeOf(candidate: ClassName, superclass: ClassName): Boolean {
    val masterCandidate = master.findClass(candidate)
    val masterSuperclass = master.findClass(superclass)
    if (masterCandidate != null) {
      return masterSuperclass != null && masterCandidate.isSubtypeOf(masterSuperclass)
    }
    if (candidate !in declarations) {
      throw InvalidPetDefinitionException("unknown premise class name: `$candidate`")
    }
    if (masterSuperclass == null && superclass !in declarations) {
      throw InvalidPetDefinitionException("unknown superclass name: `$superclass`")
    }

    val pending = ArrayDeque<ClassName>()
    val visited = mutableSetOf<ClassName>()
    pending.add(candidate)
    while (pending.isNotEmpty()) {
      val name = pending.removeLast()
      if (name == superclass) return true
      if (!visited.add(name)) continue
      if (
          masterSuperclass != null && master.findClass(name)?.isSubtypeOf(masterSuperclass) == true
      ) {
        return true
      }
      val declaration = declarations[name] ?: continue
      val directSupertypeNames =
          if (declaration.supertypes.isEmpty()) setOf(COMPONENT)
          else declaration.supertypes.mapTo(linkedSetOf()) { it.className }
      pending.addAll(directSupertypeNames)
    }
    return false
  }
}
