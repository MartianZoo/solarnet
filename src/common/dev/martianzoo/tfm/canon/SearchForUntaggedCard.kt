package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.types.Type

/** Draws the next project card with no printed tags. */
internal object SearchForUntaggedCard : CustomClass() {
  override fun translate(game: GameReader, type0: Type): Instruction =
      RealCardDraw.dealMatching(
          game,
          game.classTable.getClass(cn("ProjectCard")),
          cn("Hand").expression,
          allowExhaustion = true,
      ) { card ->
        cardTags(card).isEmpty()
      }
}
