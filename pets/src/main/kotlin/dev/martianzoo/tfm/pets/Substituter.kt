package dev.martianzoo.tfm.pets

class Substituter(val from: TypeExpression, val to: TypeExpression) : NodeVisitor() {
  override fun <P : PetsNode?> s(node: P) =
      if (node == from) to as P else super.s(node)
}

