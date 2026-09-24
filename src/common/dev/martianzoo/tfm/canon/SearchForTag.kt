@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.types.Type

/** Draws the next project card with a particular printed tag. */
internal object SearchForTag : CustomClass() {
  override fun translate(game: GameReader, owner: Type, tagClass: Type): Instruction {
    val tag = requireNotNull(tagClass.representedClass).className
    return RealCardDraw.dealMatching(
        game,
        game.classTable.getClass(cn("ProjectCard")),
        cn("Hand").expression,
        allowExhaustion = true,
    ) { card ->
      cardTags(card).count(tag) > 0
    }
  }
}
