@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.GameReader
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.api.tfmAuthority
import dev.martianzoo.tfm.data.CardDefinition.Deck.PRELUDE
import dev.martianzoo.types.Type

internal val promoCardPackCustomClasses: Set<CustomClass> = setOf(PromoCardPack.CopyPrelude)

/** Namespace for the promotional cards' custom Pets implementations. */
private object PromoCardPack {
  internal object CopyPrelude : CustomClass() {
    override fun translate(reader: GameReader, owner: Type, cardType: Type): InstructionTree {
      val card = reader.tfmAuthority.card(cardType.className)
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
