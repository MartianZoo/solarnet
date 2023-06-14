package dev.martianzoo.types

import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.util.toSetStrict

public class TypeDescription public constructor(mtype: MType) {

  private val mclass: MClass by mtype::root

  val classShortName: ClassName by mclass::shortName

  val superclassNames: Set<ClassName> = mclass.getAllSuperclasses().classNames()
  val subclassNames: Set<ClassName> = descendingBySubclassCount(mclass.getAllSubclasses())

  val rawClassEffects: Set<Effect> by mclass::rawEffects
  val classEffects: Set<Effect> by mclass::classEffects

  val classInvariants: Set<Requirement> = mclass.invariants()

  val baseType: Type by mclass::baseType

  val concreteTypesForThisClassCount =
      (baseType as MType).concreteSubtypesSameClass().take(100).count()

  val supertypes: List<Type> =
      mclass.getAllSuperclasses().map { it.withAllDependencies(mtype.dependencies) }

  val componentTypesCount: Int = mtype.allConcreteSubtypes().take(100).count()

  val componentEffects: List<Effect> = if (mtype.abstract) listOf() else mtype.toComponent().effects

  private fun descendingBySubclassCount(classes: Iterable<MClass>): Set<ClassName> =
      classes
          .sortedWith(compareBy({ -it.getAllSubclasses().size }, { it.className }))
          .classNames()
          .toSetStrict()
}
