package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.PetNode

internal class Substituter(private val subs: Map<ClassName, Expression>) : PetTransformer() {
  override fun <P : PetNode> transform(node: P): P {
    if (node is Expression) {
      val replacement: Expression? = subs[node.className]
      if (replacement != null) {
        val expr: Expression = replacement.addArgs(node.arguments)
        @Suppress("UNCHECKED_CAST") return expr as P
      }
    }
    return transformChildren(node)
  }
}
