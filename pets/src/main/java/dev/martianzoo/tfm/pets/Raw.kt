package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.PetElement

data class Raw<P : PetElement>(val unprocessed: P, val unhandled: Set<PetFeature>) {
  fun handle(handledFeatures: Set<PetFeature>) = copy(unprocessed, unhandled - handledFeatures)

  fun <Q : PetElement> map(fn: (P) -> Q?): Raw<Q>? = fn(unprocessed)?.let { Raw(it, unhandled) }

  fun finished() = unprocessed.also { require(unhandled.none()) }

  companion object {
    fun <P : PetElement, Q : PetElement> Iterable<Raw<P>>.mapAll(fn: (P) -> Q?): List<Raw<Q>> =
        mapNotNull { it.map(fn) }
  }
}
