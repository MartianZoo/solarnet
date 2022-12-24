package dev.martianzoo.tfm.pets.ast

/**
 * An API object that can be represented as PETS source code.
 */
sealed class PetsNode {

  fun groupIfNeeded(part: PetsNode) =
      if (part.parenthesizeThisWhenInside(this)) "($part)" else "$part"

  open fun parenthesizeThisWhenInside(container: PetsNode) =
      precedence() <= container.precedence()

  open fun precedence(): Int = Int.MAX_VALUE

  abstract val children: Collection<PetsNode>

  fun descendants(): List<PetsNode> {
    return children.flatMap { listOf(it) + it.descendants() }
  }
}
