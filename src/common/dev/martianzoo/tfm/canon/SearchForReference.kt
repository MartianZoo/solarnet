@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.types.Type

/** Draws the next project card whose declaration references a named Class. */
internal object SearchForReference : CustomClass() {
  override fun translate(game: GameReader, owner: Type, targetClass: Type): Instruction {
    val target = requireNotNull(targetClass.representedClass).className
    return RealCardDraw.dealMatching(
        game,
        game.classTable.getClass(cn("ProjectCard")),
        cn("Hand").expression,
        allowExhaustion = true,
    ) { card ->
      card.declaration.allNodes.any { node ->
        node.descendantsOfType<ClassName>().any { it == target }
      }
    }
  }
}
