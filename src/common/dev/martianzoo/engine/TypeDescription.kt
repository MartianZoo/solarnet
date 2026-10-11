package dev.martianzoo.engine

import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.GroundType
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.types.TypeVariable
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.state.toComponent

/**
 * Read-only description of [type] in [classTable] for inspection clients. It combines the root
 * class's authored facts with effects elaborated for the selected premise and, for a concrete
 * inhabited type, effects compiled for a component of that exact type. These values describe the
 * supplied class table; they are not live counts of components in a world.
 *
 * @param classTable Class universe and premise view used for subtype and effect elaboration.
 * @param type Type to inspect; it must belong to [classTable].
 */
public class TypeDescription
public constructor(
    private val classTable: ClassTable,
    private val type: Type,
) {

  private val rootClass: Class
    get() = type.rootClass

  private val elaborator = PetElaborator(classTable)
  private val inhabited = classTable.isInhabited(type)

  /** Root class's authored documentation, if supplied. */
  public val docstring: String?
    get() = rootClass.docstring

  /** Names in the root class's transitive superclass set, including the root class itself. */
  public val superclassNames: Set<ClassName> = rootClass.allSuperclasses().classNames()
  /**
   * Included subclasses of the root class, including itself when included, ordered by descending
   * subclass count then class name.
   */
  public val subclassNames: Set<ClassName> =
      descendingBySubclassCount(classTable.allSubclasses(rootClass))

  /** Effects on the root class declaration before premise elaboration. */
  public val rawClassEffects: List<Effect> = rootClass.declaration.effects
  /** Root class effects elaborated for this premise, or empty when [type] is uninhabited. */
  public val classEffects: List<Effect> =
      if (inhabited) elaborator.classEffects(rootClass) else emptyList()

  /** Requirements inherited by the root class. */
  public val classInvariants: Set<Requirement> = rootClass.invariants

  /** Default base type of the root class. */
  public val baseType: Type
    get() = rootClass.baseType

  /** Number of concrete specializations of [baseType], capped at 100. */
  public val concreteTypesForThisClassCount: Int =
      classTable.concreteSubtypesSameClass(baseType).take(100).count()

  /** Superclass types, including the root class, with [type]'s dependency arguments applied. */
  public val supertypes: List<Type> =
      rootClass.allSuperclasses().map { it.withAllDependencies(type.dependencies) }

  /** Ground bindings for the root class's type variables in [type]. */
  public val variableBindings: Map<TypeVariable, GroundType> =
      type.variableBindingsFrom(type.rootClass.defaultType, type.rootClass.typeVariables)

  /** Number of concrete subtypes of [type], capped at 100. */
  public val componentTypesCount: Int = classTable.allConcreteSubtypes(type).take(100).count()

  /** Effects of a component of this exact type, or empty for an abstract or uninhabited [type]. */
  public val componentEffects: List<Effect> =
      if (type.abstract || !inhabited) emptyList()
      else LiveEffect.compile(type.toComponent(), elaborator).map(LiveEffect::effect)

  private fun descendingBySubclassCount(classes: Iterable<Class>): Set<ClassName> =
      classes
          .sortedWith(compareBy({ -classTable.allSubclasses(it).size }, { it.className }))
          .classNames()
          .toSetStrict()
}
