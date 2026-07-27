package dev.martianzoo.engine

import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.Class
import dev.martianzoo.types.Type
import dev.martianzoo.util.toSetStrict

public class TypeDescription public constructor(type: Type) {

  private val rootClass: Class by type::rootClass
  private val transformers = Transformers(type.typeUniverse)

  public val classShortName: ClassName by rootClass::shortName

  public val docstring: String? by rootClass::docstring

  public val superclassNames: Set<ClassName> = rootClass.allSuperclasses().classNames()
  public val subclassNames: Set<ClassName> = descendingBySubclassCount(rootClass.allSubclasses())

  public val rawClassEffects: List<Effect> = rootClass.declaration.effects
  public val classEffects: List<Effect> = transformers.classEffects(rootClass)

  public val classInvariants: Set<Requirement> = rootClass.invariants()

  public val baseType: Type by rootClass::baseType

  public val concreteTypesForThisClassCount: Int =
      baseType.concreteSubtypesSameClass().take(100).count()

  public val supertypes: List<Type> =
      rootClass.allSuperclasses().map { it.withAllDependencies(type.dependencies) }

  public val substitutions: Map<ClassName, Expression> =
      transformers.findSubstitutions(type.rootClass.defaultType.dependencies, type.dependencies)

  public val componentTypesCount: Int = type.allConcreteSubtypes().take(100).count()

  public val componentEffects: List<Effect> =
      if (type.abstract) listOf() else type.toComponent().effects(transformers)

  private fun descendingBySubclassCount(classes: Iterable<Class>): Set<ClassName> =
      classes
          .sortedWith(compareBy({ -it.allSubclasses().size }, { it.className }))
          .classNames()
          .toSetStrict()
}
