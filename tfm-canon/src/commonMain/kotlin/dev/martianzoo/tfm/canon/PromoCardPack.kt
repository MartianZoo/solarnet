@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.CardDefinition.Deck.PRELUDE

internal val promoCardPackBundle: StandardFormBundle by lazy {
  StandardFormBundle("PromoCardPack", promoCardPackCustomClasses)
}

private val promoCardPackCustomClasses: Set<CustomClass> = setOf(PromoCardPack.CopyPrelude)

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
