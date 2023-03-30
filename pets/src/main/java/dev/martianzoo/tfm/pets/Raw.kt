package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.PetElement

data class Raw<P : PetElement>(val unprocessed: P) {
  fun <Q : PetElement> map(fn: (P) -> Q?): Raw<Q>? = fn(unprocessed)?.let { Raw(it) }

  companion object {
    fun <P : PetElement, Q : PetElement> Iterable<Raw<P>>.mapAll(fn: (P) -> Q?): List<Raw<Q>> =
        mapNotNull { it.map(fn) }
  }
}
