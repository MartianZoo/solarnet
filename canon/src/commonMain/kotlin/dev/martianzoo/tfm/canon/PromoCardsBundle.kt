@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.CustomMetric
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.api.tfmRuleset
import dev.martianzoo.tfm.data.CardDefinition.Deck.PRELUDE

internal val promoCardsCustomClasses: Set<CustomClass> =
    setOf(PromoCardsBundle.CopyPrelude, PromoCardsBundle.DistinctResourceType)

/** Namespace for the promotional cards' custom Pets implementations. */
internal object PromoCardsBundle {
  internal object DistinctResourceType : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int =
        distinctClasses(game, type, cn("Resource"))
  }

  internal object CopyPrelude : CustomClass() {
    override fun translate(reader: GameReader, owner: Type, cardType: Type): Instruction {
      val card = reader.tfmRuleset.card(cardType.className)
      if (card.deck != PRELUDE) {
        throw NarrowingException("Card ${card.className} is not a prelude card")
      }
      if (card.className == cn("DoubleDown")) {
        throw NarrowingException("Cute, but Double Down can't copy itself")
      }
      return card.immediate ?: NoOp
    }
  }
}
