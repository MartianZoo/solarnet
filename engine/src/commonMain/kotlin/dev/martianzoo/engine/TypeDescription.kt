package dev.martianzoo.engine

import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.Class
import dev.martianzoo.types.Type
import dev.martianzoo.util.toSetStrict

public class TypeDescription public constructor(type: Type) {

  private val rootClass: Class by type::rootClass
  private val transformers = Transformers(type.typeUniverse)

  val classShortName: ClassName by rootClass::shortName

  val docstring: String? by rootClass::docstring

  val superclassNames: Set<ClassName> = rootClass.allSuperclasses().classNames()
  val subclassNames: Set<ClassName> = descendingBySubclassCount(rootClass.allSubclasses())

  val rawClassEffects: List<Effect> = rootClass.declaration.effects
  val classEffects: List<Effect> = transformers.classEffects(rootClass)

  val classInvariants: Set<Requirement> = rootClass.invariants()

  val baseType: Type by rootClass::baseType

  val concreteTypesForThisClassCount = baseType.concreteSubtypesSameClass().take(100).count()

  val supertypes: List<Type> =
      rootClass.allSuperclasses().map { it.withAllDependencies(type.dependencies) }

  val substitutions =
      transformers.findSubstitutions(type.rootClass.defaultType.dependencies, type.dependencies)

  val componentTypesCount: Int = type.allConcreteSubtypes().take(100).count()

  val componentEffects: List<Effect> =
      if (type.abstract) listOf() else type.toComponent().effects(transformers)

  private fun descendingBySubclassCount(classes: Iterable<Class>): Set<ClassName> =
      classes
          .sortedWith(compareBy({ -it.allSubclasses().size }, { it.className }))
          .classNames()
          .toSetStrict()
}
