package dev.martianzoo.tfm.pets.ast

/**
 * An API object that can be represented as PETS source code.
 */
sealed class PetsNode {
  abstract val kind: String
  abstract val children: Collection<PetsNode>

  fun descendants(): List<PetsNode> =
      children.flatMap { listOf(it) + it.descendants() }

  fun groupPartIfNeeded(part: PetsNode) =
      if (part.shouldGroupInside(this)) "($part)" else "$part"

  open fun shouldGroupInside(container: PetsNode) =
      precedence() <= container.precedence()

  open fun precedence(): Int = Int.MAX_VALUE

  interface ProductionBox<P : PetsNode> { fun extract(): P }
}
