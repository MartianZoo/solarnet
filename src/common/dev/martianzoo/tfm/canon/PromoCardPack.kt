@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.Type

private val promoCardPackCustomClasses: Set<CustomClass> = setOf(PromoCardPack.CopyPrelude)

internal val promoCardPackBundle: StandardFormBundle =
    StandardFormBundle(
        "PromoCardPack",
        promoCardPackCustomClasses,
        moduleClassExclusions =
            mapOf(
                cn("PromoCardPack") to
                    setOf(cn("DeimosDown"), cn("GreatDam"), cn("MagneticFieldGenerators"))
            ),
    )

/** Namespace for the promotional cards' custom Pets implementations. */
private object PromoCardPack {
  internal object CopyPrelude : CustomClass() {
    override fun translate(reader: GameReader, owner: Type, cardType: Type): InstructionTree {
      val card = reader.tfmCatalog.card(cardType.className)
      if (cardBack(card)?.className != TfmClasses.PRELUDE_CARD) {
        throw NarrowingException("Card ${card.className} is not a prelude card")
      }
      if (card.className == cn("DoubleDown")) {
        throw NarrowingException("Cute, but Double Down can't copy itself")
      }
      return cardImmediate(card) ?: NoOp
    }
  }
}
