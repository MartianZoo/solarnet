package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.TransformNode
import dev.martianzoo.tfm.data.TfmClasses
import dev.martianzoo.types.MClassTable

object Prod {
  public fun deprodify(table: MClassTable): PetTransformer {
    if (TfmClasses.STANDARD_RESOURCE !in table.allClassNamesAndIds ||
      TfmClasses.PRODUCTION !in table.allClassNamesAndIds) {
      return PetTransformer.noOp()
    }
    val classNames =
        table.getClass(TfmClasses.STANDARD_RESOURCE).getAllSubclasses().flatMap {
          setOf(it.className, it.shortName)
        }

    var inProd = false

    return object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        val rewritten: PetNode =
            when {
              node is Multi -> {
                val badIndex =
                    node.instructions.indexOfFirst {
                      it is Transform && it.transformKind == TfmClasses.PROD && it.instruction is Multi
                    }
                val xed = transformChildren(node)
                if (badIndex == -1) {
                  xed
                } else {
                  Multi.create(
                      xed.instructions.subList(0, badIndex) +
                          (xed.instructions[badIndex] as Multi).instructions +
                          xed.instructions.subList(badIndex + 1, xed.instructions.size),
                  )
                }
              }
              node is TransformNode<*> && node.transformKind == TfmClasses.PROD -> {
                require(!inProd)
                inProd = true
                val inner = transform(node.extract())
                inProd = false
                if (inner == node.extract()) {
                  throw PetSyntaxException("No standard resources found in PROD box: $inner")
                }
                inner
              }
              inProd && node is Expression && node.className in classNames ->
                TfmClasses.PRODUCTION.of(node.arguments + node.className.classExpression())
              else -> transformChildren(node)
            }
        @Suppress("UNCHECKED_CAST") return rewritten as P
      }
    }
  }
}
