package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.types.PetClassLoader

class Deprodifier(val productionable: Set<String>, val prodType: String) : NodeVisitor() {
  companion object {
    fun <P : PetsNode> deprodify(node: P, productionable: Set<String>, prodType: String): P {
      return Deprodifier(productionable, prodType).s(node)
    }

    fun <P : PetsNode> deprodify(node: P, loader: PetClassLoader): P {
      val resourceNames = loader["StandardResource"].allSubclasses.map { it.name }.toSet()
      return deprodify(node, resourceNames, "Production")
    }
  }
  enum class Prodding { NOT_YET, DOIN_IT, DID_THAT }
  var inProd = Prodding.NOT_YET

  val types: List<PetsNode> = productionable.map(::te)

  override fun <P : PetsNode?> s(node: P): P {
    return when {
      node is TypeExpression && node.className == prodType -> error("wtf dude")

      node is ProductionBox<*> -> {
        require(inProd == Prodding.NOT_YET)
        inProd = Prodding.DOIN_IT
        s(node.extract()).also { inProd = Prodding.DID_THAT }
      }

      node is TypeExpression && node.className in productionable && inProd == Prodding.DOIN_IT -> {
        TypeExpression(prodType, listOf(node as TypeExpression))
      }

      else -> super.s(node)

    } as P
  }
}
