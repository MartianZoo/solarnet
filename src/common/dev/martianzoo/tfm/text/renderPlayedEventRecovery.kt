package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Transmute

internal fun renderPlayedEventRecovery(
    transmute: Transmute,
    describers: Describers,
): Clause? {
  if (
      transmute.quantifier.modality() != Modality.OPTIONAL ||
          !transmute.gaining.simple ||
          transmute.gaining.className != cn("ProjectCard") ||
          !transmute.removing.simple ||
          transmute.removing.className != cn("PlayedEvent") ||
          describers.changeFrame(transmute.gaining.className) != ComponentDescriber.ChangeFrame.Deck
  ) {
    return null
  }
  val count = transmute.count.fixedQuantity() ?: return null
  val cards =
      if (count == 1) "one of your played event cards" else "$count of your played event cards"
  return Clause.Simple(
      Predicate(
          Verb("may return"),
          Coordination.one(NounPhrase.text("up to $cards")),
          listOf(Modifier.Phrase("to your hand")),
      ),
      NounPhrase.you(),
  )
}
