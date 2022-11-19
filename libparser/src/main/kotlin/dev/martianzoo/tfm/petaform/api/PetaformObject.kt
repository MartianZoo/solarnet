package dev.martianzoo.tfm.petaform.api

/**
 * An API object that can be represented as Petaform source code.
 */
interface PetaformObject {
  /** The Petaform source representation of this API object, in a standard style. */
  val asSource: String
}
