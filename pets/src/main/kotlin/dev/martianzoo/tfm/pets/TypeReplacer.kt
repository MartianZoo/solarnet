package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.TypeExpression

class TypeReplacer(val from: TypeExpression, val to: TypeExpression) : NodeVisitor() {
  override fun <P : PetsNode?> s(node: P) =
      if (node == from) {
        @Suppress("UNCHECKED_CAST")
        to as P
      } else {
        super.s(node)
      }
}

