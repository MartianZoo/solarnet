@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.api.tfmRuleset
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.TfmClasses.PROD

internal val corporateEraCustomClasses: Set<CustomClass> =
    setOf(CorporateEraExpansion.CopyProductionBox)

/** Namespace for Corporate Era's custom Pets implementations. */
internal object CorporateEraExpansion {
  internal object CopyProductionBox : CustomClass() {
    override fun translate(reader: GameReader, owner: Type, cardType: Type): Instruction {
      val card: CardDefinition = reader.tfmRuleset.card(cardType.className)
      val immediate =
          card.immediate
              ?: throw NarrowingException("card ${card.className} has no immediate instruction")
      val matches = immediate.descendantsOfType<Transform>().filter { it.transformKind == PROD }

      when (matches.size) {
        0 -> throw NarrowingException("must choose a card that has an immediate PROD box")
        1 -> return matches.first()
        else -> error("Card ${card.className} is malformed, has ${matches.size} PROD blocks")
      }
    }
  }
}
