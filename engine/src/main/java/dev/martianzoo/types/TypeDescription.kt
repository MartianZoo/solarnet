package dev.martianzoo.types

import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.util.toSetStrict

public class TypeDescription public constructor(val mtype: MType) {

  private val mclass: MClass by mtype::root

  val classShortName: ClassName by mclass::shortName

  val superclassNames: Set<ClassName> = mclass.allSuperclasses.classNames()
  val subclassNames: Set<ClassName> = descendingBySubclassCount(mclass.allSubclasses)

  val rawClassEffects: Set<Effect> = mclass.rawEffects()
  val classEffects: Set<Effect> by mclass::classEffects

  val classInvariants: Set<Requirement> by mclass::invariants

  val baseType: Type by mclass::baseType

  val concreteTypesForThisClassCount: Int by lazy {
    (baseType as MType).concreteSubtypesSameClass().take(100).count()
  }

  val type: Type by ::mtype

  val supertypes: List<Type> =
      mclass.allSuperclasses.map { it.withAllDependencies(mtype.dependencies) }

  val componentTypesCount: Int by lazy { mtype.allConcreteSubtypes().take(100).count() }

  val componentEffects: List<Effect> = if (type.abstract) listOf() else mtype.toComponent().effects

  private fun descendingBySubclassCount(classes: Iterable<MClass>): Set<ClassName> =
      classes
          .sortedWith(compareBy({ -it.allSubclasses.size }, { it.className }))
          .classNames()
          .toSetStrict()
}
