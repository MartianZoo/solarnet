package dev.martianzoo.tfm.pets

/**
 * An API object that can be represented as PETS source code.
 */
sealed class PetsNode {
  fun toStringWhenInside(container: PetsNode) = if (parenthesizeThisWhenInside(container)) "($this)" else "$this"

  open fun parenthesizeThisWhenInside(container: PetsNode) = precedence() <= container.precedence()

  open fun precedence(): Int = Int.MAX_VALUE

  abstract val children: Collection<PetsNode>

  open fun countProds(): Int = children.map { it.countProds() }.sum()

  fun descendants(): List<PetsNode> {
    return children.flatMap { listOf(it) + it.descendants() }
  }
}
