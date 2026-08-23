package dev.martianzoo.engine

import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.Class
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Type
import dev.martianzoo.util.toSetStrict

public class TypeDescription
public constructor(
    private val classTable: ClassTable,
    type: Type,
) {

  private val rootClass: Class by type::rootClass
  private val transformers = Transformers(classTable)
  private val active = classTable.isActive(type)

  public val docstring: String? by rootClass::docstring

  public val superclassNames: Set<ClassName> = rootClass.allSuperclasses().classNames()
  public val subclassNames: Set<ClassName> =
      descendingBySubclassCount(classTable.allSubclasses(rootClass))

  public val rawClassEffects: List<Effect> = rootClass.declaration.effects
  public val classEffects: List<Effect> =
      if (active) transformers.classEffects(rootClass) else emptyList()

  public val classInvariants: Set<Requirement> = rootClass.invariants()

  public val baseType: Type by rootClass::baseType

  public val concreteTypesForThisClassCount: Int =
      classTable.concreteSubtypesSameClass(baseType).take(100).count()

  public val supertypes: List<Type> =
      rootClass.allSuperclasses().map { it.withAllDependencies(type.dependencies) }

  public val substitutions: Map<ClassName, Expression> =
      transformers.findSubstitutions(type.rootClass.defaultType.dependencies, type.dependencies)

  public val componentTypesCount: Int = classTable.allConcreteSubtypes(type).take(100).count()

  public val componentEffects: List<Effect> =
      if (type.abstract || !active) emptyList()
      else LiveEffect.compile(type.toComponent(), transformers).map(LiveEffect::effect)

  private fun descendingBySubclassCount(classes: Iterable<Class>): Set<ClassName> =
      classes
          .sortedWith(compareBy({ -classTable.allSubclasses(it).size }, { it.className }))
          .classNames()
          .toSetStrict()
}
