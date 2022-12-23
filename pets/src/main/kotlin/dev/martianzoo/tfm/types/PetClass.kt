package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.ComponentDef.Defaults
import dev.martianzoo.tfm.pets.Deprodifier.Companion.deprodify
import dev.martianzoo.tfm.pets.Instruction.Gain
import dev.martianzoo.tfm.pets.Instruction.Remove
import dev.martianzoo.tfm.pets.NodeVisitor
import dev.martianzoo.tfm.pets.PetsNode
import dev.martianzoo.tfm.pets.QuantifiedExpression
import dev.martianzoo.tfm.pets.TypeExpression
import dev.martianzoo.tfm.pets.te

/**
 */
data class PetClass(val def: ComponentDef, val loader: PetClassLoader): DependencyTarget {
  val name by def::name
  override val abstract by def::abstract


// HIERARCHY

  val directSupertypes: Set<PetType> by lazy {
    def.supertypes.map { loader.resolve/*WithDefaults*/(it) }.toSet()
  }

  override fun isSubtypeOf(that: DependencyTarget) = loader.allSubclasses.hasEdgeConnecting(that as PetClass, this)

  fun isSuperclassOf(that: PetClass) = loader.allSubclasses.hasEdgeConnecting(this, that)
  val directSubclasses: Set<PetClass> get() = loader.directSubclasses.successors(this)

  val directSuperclasses: Set<PetClass> get() = loader.directSubclasses.predecessors(this)
  val allSubclasses: Set<PetClass> get() = loader.allSubclasses.successors(this)

  val allSuperclasses: Set<PetClass> get() = loader.allSubclasses.predecessors(this)

// DEPENDENCIES

  val directDependencyKeys: Set<DependencyKey> by lazy {
    def.dependencies.withIndex().map {
      (i, dep) -> DependencyKey(this, i, dep.classDep)
    }.toSet()
  }

  val allDependencyKeys: Set<DependencyKey> by lazy {
    allSuperclasses.flatMap { it.directDependencyKeys }.toSet()
  }

  /** Common supertype of all types with petClass==this */
  val baseType: PetType by lazy {
    val deps = DependencyMap.merge(directSupertypes.map { it.dependencies })

    val newDeps = directDependencyKeys.map {
      val typeExpression = def.dependencies[it.index].type
      val depType = if (it.classDep) {
        loader.get(typeExpression.className)
      } else {
        loader.resolve(typeExpression)
      }
      it to depType
    }.toMap()
    val allDeps = deps.merge(DependencyMap(newDeps))
    require(allDeps.keyToType.keys == allDependencyKeys)
    PetType(this, allDeps)
  }

// DEFAULTS

  val defaults: Defaults by lazy {
    val fromSupes = Defaults().merge(directSuperclasses.map { it.defaults })
    Defaults(
        listOfNotNull(def.defaults.typeExpression, fromSupes.typeExpression).firstOrNull(),
        listOfNotNull(def.defaults.gainType, fromSupes.gainType).firstOrNull(),
        listOfNotNull(def.defaults.gainIntensity, fromSupes.gainIntensity).firstOrNull(),
        listOfNotNull(def.defaults.removeType, fromSupes.removeType).firstOrNull(),
        listOfNotNull(def.defaults.removeIntensity, fromSupes.removeIntensity).firstOrNull()
    )
  }


// EFFECTS

  val directEffects by lazy {
    def.effects
        .map { deprodify(it, loader) }
        .map { Defaulter(loader).s(it) }
  }

  class Defaulter(val loader: PetClassLoader): NodeVisitor() {
    override fun <P : PetsNode?> s(node: P): P {
      return when {
        node is TypeExpression && node.isClassOnly() -> {
          val defs = loader[node.className].defaults
          defs.typeExpression?.copy(className = node.className) ?: node
        }

        node is QuantifiedExpression -> node.copy(
            s(node.typeExpression ?: te("Megacredit")),
            node.scalar ?: 1
        )

        node is Gain -> {
          val te = node.qe.explicit().typeExpression!!
          val defs = loader[te.className].defaults

          val gain = if (te.isClassOnly()) {
            node.copy(
                qe = node.qe.copy(
                    typeExpression = s(defs.typeExpression?.copy(className = te.className) ?: te)
                )
            )
          } else {
            node
          }
          gain.copy(intensity = listOfNotNull(gain.intensity, defs.gainIntensity).firstOrNull())
        }

        node is Remove -> {
          val te = node.qe.explicit().typeExpression!!
          val defs = loader[te.className].defaults

          val remove = if (te.isClassOnly()) {
            node.copy(
                qe = node.qe.copy(
                    typeExpression = s(defs.typeExpression?.copy(className = te.className) ?: te)
                )
            )
          } else {
            node
          }
          remove.copy(
              intensity = listOfNotNull(
                  remove.intensity,
                  defs.removeIntensity
              ).firstOrNull()
          )
        }

        else -> node
      } as P
    }
  }


// OTHER

  /** Returns the one of `this` or `that` that is a subclass of the other. */
  override fun glb(that: DependencyTarget) = when {
    that !is PetClass -> error("")
    this.isSubtypeOf(that) -> this
    that.isSubtypeOf(this) -> that
    else -> error("we ain't got no intersection types")
  }

  fun lub(that: PetClass) = when {
    this.isSubtypeOf(that) -> that
    that.isSubtypeOf(this) -> this
    else -> allSuperclasses.intersect(that.allSuperclasses).maxBy { it.allSuperclasses.size }
  }

  override fun toString() = name
}
