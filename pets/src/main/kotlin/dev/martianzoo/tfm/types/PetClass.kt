package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.Deprodifier.Companion.deprodify
import dev.martianzoo.tfm.pets.Instruction
import dev.martianzoo.tfm.pets.Instruction.Intensity
import dev.martianzoo.tfm.pets.rootName

/**
 */
data class PetClass(val def: ComponentDef, val loader: PetClassLoader): DependencyTarget {
  val name by def::name
  override val abstract by def::abstract


// HIERARCHY

  override fun isSubtypeOf(that: DependencyTarget) = loader.allSubclasses.hasEdgeConnecting(that as PetClass, this)
  fun isSuperclassOf(that: PetClass) = loader.allSubclasses.hasEdgeConnecting(this, that)

  val directSubclasses: Set<PetClass> get() = loader.directSubclasses.successors(this)
  val directSuperclasses: Set<PetClass> get() = loader.directSubclasses.predecessors(this)

  val allSubclasses: Set<PetClass> get() = loader.allSubclasses.successors(this)
  val allSuperclasses: Set<PetClass> get() = loader.allSubclasses.predecessors(this)

  val directSupertypes: Set<PetType> by lazy {
    def.supertypes.map { loader.resolve(it) }.toSet()
  }

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

// General inheritable stuff

  val explicitDefaultGain: Instruction.Gain? by lazy {
    val gains = def.defaults.filterIsInstance(Instruction.Gain::class.java)
    require (gains.size <= 1)
    gains.firstOrNull()
  }

  val heritableDefaultGainIntensity: Intensity? by lazy {
    val intens: Intensity? = explicitDefaultGain?.intensity
    if (intens != null) { // override
      intens
    } else {
      val inherited = directSuperclasses
          .filter { it.name != rootName }
          .map { it.heritableDefaultGainIntensity }
          .toSet()
      require(inherited.size <= 1)
      inherited.firstOrNull()
    }
  }

  val defaultGainIntensity: Intensity by lazy {
    heritableDefaultGainIntensity ?: loader[rootName].explicitDefaultGain?.intensity!!
  }

// EFFECTS

  val directEffects by lazy {
    val resourceNames = loader["StandardResource"].allSubclasses.map { it.name }.toSet()
    def.effects.map { deprodify(it, resourceNames, "Production") }
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
