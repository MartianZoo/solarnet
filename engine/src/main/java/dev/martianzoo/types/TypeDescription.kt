package dev.martianzoo.types

import dev.martianzoo.api.Type
import dev.martianzoo.engine.Transformers
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.util.toSetStrict

public class TypeDescription public constructor(mtype: MType) {

  private val mclass: MClass by mtype::root

  val classShortName: ClassName by mclass::shortName

  val docstring: String? by mclass::docstring

  val superclassNames: Set<ClassName> = mclass.allSuperclasses().classNames()
  val subclassNames: Set<ClassName> = descendingBySubclassCount(mclass.allSubclasses())

  val rawClassEffects: Set<Effect> by mclass::declaredEffects
  val classEffects: Set<Effect> by mclass::classEffects

  val classInvariants: Set<Requirement> = mclass.invariants()

  val baseType: Type by mclass::baseType

  val concreteTypesForThisClassCount =
      (baseType as MType).concreteSubtypesSameClass().take(100).count()

  val supertypes: List<Type> =
      mclass.allSuperclasses().map { it.withAllDependencies(mtype.dependencies) }

  val substitutions =
      Transformers(mtype.loader)
          .findSubstitutions(mtype.root.defaultType.dependencies, mtype.dependencies)

  val componentTypesCount: Int = mtype.allConcreteSubtypes().take(100).count()

  val componentEffects: List<Effect> = if (mtype.abstract) listOf() else mtype.toComponent().effects

  private fun descendingBySubclassCount(classes: Iterable<MClass>): Set<ClassName> =
      classes
          .sortedWith(compareBy({ -it.allSubclasses().size }, { it.className }))
          .classNames()
          .toSetStrict()
}
