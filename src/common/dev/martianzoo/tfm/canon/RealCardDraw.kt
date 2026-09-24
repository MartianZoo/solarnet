@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.Type

/** An ordered catalog deck whose spent cards are recorded in the World. */
internal object RealCardDraw : CustomClass("DrawCard") {
  override fun translate(
      game: GameReader,
      owner: Type,
      backClassType: Type,
      location: Type,
  ): Instruction {
    val back = requireNotNull(backClassType.representedClass)
    return dealMatching(game, back, location.expression) { true }
  }

  internal fun dealMatching(
      game: GameReader,
      back: Class,
      location: Expression,
      allowExhaustion: Boolean = false,
      matches: (Class) -> Boolean,
  ): Instruction {
    val remaining =
        game.tfmCatalog.cards
            .asSequence()
            .filter { card ->
              game.classTable.isInhabited(card.className) &&
                  cardBack(card)?.isSubtypeOf(back) == true
            }
            .sortedBy { it.className }
            .filter { card ->
              val face = card.className.classExpression()
              game.count(game.resolve(DECK_SPENT.of(face))) == 0 &&
                  game.count(game.resolve(CARD.of(face))) == 0
            }
            .toList()
    val matchIndex = remaining.indexOfFirst(matches)
    if (matchIndex < 0 && allowExhaustion) {
      return Then.create(remaining.map { spentMarker(it.className) })
    }
    val face =
        remaining.getOrNull(matchIndex)?.className
            ?: throw NarrowingException("No cards left in the ${back.className} deck")
    return Then.create(
        remaining.take(matchIndex + 1).map { spentMarker(it.className) } +
            gain(back.className.of(face.classExpression(), location))
    )
  }

  private fun spentMarker(face: ClassName): Instruction =
      By(gain(DECK_SPENT.of(face.classExpression())), ADMIN.expression)

  private val DECK_SPENT = cn("DeckSpent")
  private val CARD = cn("Card")
}
