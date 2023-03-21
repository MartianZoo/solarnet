package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.PetElement

data class Raw<P : PetElement>(val element: P, val features: Set<PetFeature>) {
  fun handle(handledFeatures: Set<PetFeature>) = copy(element, features - handledFeatures)
}
