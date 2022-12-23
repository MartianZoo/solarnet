package dev.martianzoo.tfm.pets

class TypeReplacer(val from: TypeExpression, val to: TypeExpression) : NodeVisitor() {
  override fun <P : PetsNode?> s(node: P) =
      if (node == from) {
        @Suppress("UNCHECKED_CAST")
        to as P
      } else {
        super.s(node)
      }
}

