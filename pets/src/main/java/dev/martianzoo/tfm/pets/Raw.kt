package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.PetElement

data class Raw<P : PetElement>(val unprocessed: P, val features: Set<PetFeature>) {
  fun handle(handledFeatures: Set<PetFeature>) = copy(unprocessed, features - handledFeatures)

  fun <Q : PetElement> map(fn: (P) -> Q): Raw<Q> = Raw(fn(unprocessed), features)

  companion object {
    fun <P : PetElement, Q : PetElement> Iterable<Raw<P>>.mapAll(fn: (P) -> Q): List<Raw<Q>> =
        this.map { it.map(fn) }
  }
}
