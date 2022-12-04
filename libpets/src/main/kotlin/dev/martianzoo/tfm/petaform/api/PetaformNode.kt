package dev.martianzoo.tfm.petaform.api

/**
 * An API object that can be represented as Petaform source code.
 */
abstract class PetaformNode {
  abstract val children: Collection<PetaformNode>

  open fun countProds(): Int = children.map { it.countProds() }.sum()
}
