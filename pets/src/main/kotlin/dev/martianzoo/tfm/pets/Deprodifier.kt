package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.ProductionBox
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.types.PetClassLoader

class Deprodifier(val producible: Set<String>) : NodeVisitor() {
  companion object {
    fun <P : PetsNode> deprodify(node: P, producibleClassNames: Set<String>): P {
      return Deprodifier(producibleClassNames).s(node)
    }

    fun <P : PetsNode> deprodify(node: P, loader: PetClassLoader): P {
      val resourceNames = loader["StandardResource"].allSubclasses.map { it.name }.toSet()
      return deprodify(node, resourceNames)
    }
  }

  var inProd : Boolean = false

  override fun <P : PetsNode?> s(node: P): P =
    when {
      node is ProductionBox<*> -> {
        require(!inProd)
        inProd = true
        s(node.extract()).also { inProd = false }
      }
      inProd && node is TypeExpression && node.className in producible ->
        TypeExpression("Production", listOf(node))

      else -> super.s(node)
    } as P
}
