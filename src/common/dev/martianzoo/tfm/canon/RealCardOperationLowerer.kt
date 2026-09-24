package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.data.ClassDeclaration

/** Validates marked card procedures and preserves their face-sensitive Pets for execution. */
internal object RealCardOperationLowerer : TransformHandler {
  internal fun lower(source: ClassDeclaration): ClassDeclaration {
    val transformed = source.effects.map(dispatcher::transformEffect)
    return source.copy(
        executableEffects = transformed.takeUnless { it == source.authoredEffectsWithActions },
    )
  }

  override fun transform(inner: PetNode): PetNode {
    val instruction =
        inner as? InstructionTree ?: error("CARDS requires an instruction tree: $inner")
    CardOperation.decode(instruction)
    return instruction
  }

  private val dispatcher = TransformHandler.dispatcher(mapOf(CardOperation.TRANSFORM_KIND to this))
}
