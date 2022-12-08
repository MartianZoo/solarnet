package dev.martianzoo.tfm.petaform

/**
 * An API object that can be represented as Petaform source code.
 */
abstract class PetaformNode {
  fun toStringWithin(container: PetaformNode) = if (groupWithin(container)) "(${this})" else "$this"
  open fun groupWithin(container: PetaformNode) = precedence() <= container.precedence()
  open fun precedence(): Int = Int.MAX_VALUE

  abstract val children: Collection<PetaformNode>

  open fun countProds(): Int = children.map { it.countProds() }.sum()
}
