package dev.martianzoo.tfm.petaform.api

/**
 * An API object that can be represented as Petaform source code.
 */
abstract class PetaformObject {
  abstract val hasProd: Boolean
  abstract val children: Collection<PetaformObject>
}
