@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.data.CardDefinition.Deck.PRELUDE

/** The promotional cards currently supported by Canon. */
internal object PromosExpansion :
    CanonicalBundle(name = "PromosExpansion", legacyCode = "X", cards = true) {
  override val customClasses: Set<CustomClass> = setOf(CopyPrelude)

  private object CopyPrelude : CustomClass("CopyPrelude") {
    override fun translate(reader: GameReader, owner: Type, cardType: Type): Instruction {
      val card = (reader.ruleset as TfmRuleset).card(cardType.className)
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
