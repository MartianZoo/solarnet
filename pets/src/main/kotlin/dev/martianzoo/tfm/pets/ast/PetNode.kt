package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetVisitor

/** An API object that can be represented as PETS source code. */
sealed class PetNode {
  abstract val kind: String

  fun groupPartIfNeeded(part: PetNode) = if (part.shouldGroupInside(this)) "($part)" else "$part"

  open fun shouldGroupInside(container: PetNode) = precedence() <= container.precedence()

  open fun precedence(): Int = Int.MAX_VALUE

  abstract fun visitChildren(v: PetVisitor)

  interface GenericTransform<P : PetNode> {
    val transform: String
    fun extract(): P
  }
}
