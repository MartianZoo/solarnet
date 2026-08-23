package dev.martianzoo.tfm.data

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.data.TfmClasses.PROJECT_CARD

/** A validated semantic view of one canonical `CARDS[...]` instruction. */
public sealed interface CardOperation {
  /** Reveal cards until the requested matching cards have been found. */
  public data class Search(public val cards: Gain, public val filter: Requirement) : CardOperation

  /** Inspect [offered] cards, retain [retained], and discard the remainder. */
  public data class SelectAndKeep(public val offered: Gain, public val retained: Transmute) :
      CardOperation

  /** Inspect one card and either buy or discard it. */
  public data class SelectAndPurchase(public val offered: Gain) : CardOperation

  /** Inspect cards, retain matching cards for free, and buy or discard the remainder. */
  public data class FilteredPurchase(
      public val offered: Gain,
      public val retained: Transmute,
      public val filter: Requirement,
  ) : CardOperation

  /** Reveal one card, discard it, and gain [outcome] when [filter] matches. */
  public data class RevealAndTest(
      public val revealed: Gain,
      public val filter: Requirement,
      public val outcome: Gain,
  ) : CardOperation

  /** Return played Events to the player's hand. */
  public data class RecoverEvents(public val recovered: Transmute) : CardOperation

  public companion object {
    public const val TRANSFORM_KIND: String = "CARDS"

    /** Validates and interprets [transform] as one canonical card operation. */
    public fun decode(transform: Transform): CardOperation {
      if (transform.transformKind != TRANSFORM_KIND) malformed(transform.instruction)
      return decode(transform.instruction)
    }

    private fun decode(source: InstructionTree): CardOperation =
        when (source) {
          is Gain -> decodeSearch(source)
          is InstructionGroup -> decodeSelection(source)
          is Then ->
              when {
                source.first.gainingAt(SELECTING) -> decodeFilteredPurchase(source)
                source.first.gainingAt(REVEALED) -> decodeReveal(source)
                else -> malformed(source)
              }
          is Transmute -> decodeEventRecovery(source)
          else -> malformed(source)
        }

    private fun decodeSearch(source: Gain): Search {
      if (
          source.gaining.className != PROJECT_CARD ||
              source.gaining.arguments.any() ||
              !source.mandatory
      ) {
        malformed(source)
      }
      val filter = source.gaining.refinement?.requirement ?: malformed(source)
      return Search(source, filter)
    }

    private fun decodeSelection(source: InstructionGroup): CardOperation {
      if (source.instructions.size != 2) malformed(source)
      val offered =
          source.instructions.filterIsInstance<Gain>().singleOrNull {
            it.gaining.isUnfilteredProjectCardAt(SELECTING) && it.mandatory
          } ?: malformed(source)
      val result = source.instructions.singleOrNull { it !== offered } ?: malformed(source)
      return when (result) {
        is Transmute -> decodeRetainedCards(offered, result)
        is Then -> decodePurchase(offered, result)
        else -> malformed(source)
      }
    }

    private fun decodeRetainedCards(offered: Gain, retained: Transmute): SelectAndKeep {
      if (
          retained.gaining != PROJECT_CARD.expression ||
              !retained.removing.isProjectCardAt(SELECTING) ||
              retained.removing.refinement != null ||
              !retained.mandatory
      ) {
        malformed(retained)
      }
      return SelectAndKeep(offered, retained)
    }

    private fun decodePurchase(offered: Gain, purchase: Then): SelectAndPurchase {
      if (
          offered.count != ActualScalar(1) ||
              purchase.instructions.size != 2 ||
              !purchase.first.removingOptionallyAt(SELECTING) ||
              !purchase.instructions.last().isMandatoryGainOf(BUY_SELECTED_CARDS)
      ) {
        malformed(purchase)
      }
      return SelectAndPurchase(offered)
    }

    private fun decodeFilteredPurchase(source: Then): FilteredPurchase {
      if (source.instructions.size != 4) malformed(source)
      val offered = source.first as? Gain ?: malformed(source)
      val retained = source.instructions[1] as? Transmute ?: malformed(source)
      if (
          !offered.gaining.isUnfilteredProjectCardAt(SELECTING) ||
              !offered.mandatory ||
              retained.gaining != PROJECT_CARD.expression ||
              !retained.removing.isProjectCardAt(SELECTING) ||
              retained.intensity != AMAP ||
              !source.instructions[2].removingOptionallyAt(SELECTING) ||
              !source.instructions.last().isMandatoryGainOf(BUY_SELECTED_CARDS)
      ) {
        malformed(source)
      }
      val filter = retained.removing.refinement?.requirement ?: malformed(source)
      if (offered.count !is ActualScalar) malformed(source)
      return FilteredPurchase(offered, retained, filter)
    }

    private fun decodeReveal(source: Then): RevealAndTest {
      if (source.instructions.size != 2) malformed(source)
      val revealed = source.first as? Gain ?: malformed(source)
      if (
          !revealed.gaining.isUnfilteredProjectCardAt(REVEALED) ||
              revealed.count != ActualScalar(1) ||
              !revealed.mandatory
      ) {
        malformed(source)
      }
      val choices = source.instructions.last() as? Or ?: malformed(source)
      if (choices.instructions.count { it == NoOp } != 1) malformed(source)
      val gated = choices.instructions.filterIsInstance<Gated>().singleOrNull() ?: malformed(source)
      val matchingCard =
          gated.gate.descendantsOfType<Expression>().singleOrNull {
            it.isProjectCardAt(REVEALED) && it.refinement != null
          } ?: malformed(source)
      val outcome = gated.inner as? Gain ?: malformed(source)
      if (!outcome.mandatory) malformed(source)
      return RevealAndTest(revealed, matchingCard.refinement!!.requirement, outcome)
    }

    private fun decodeEventRecovery(source: Transmute): RecoverEvents {
      if (
          source.gaining != PROJECT_CARD.expression ||
              !source.removing.isProjectCardAt(EVENT_PILE) ||
              source.intensity != OPTIONAL
      ) {
        malformed(source)
      }
      return RecoverEvents(source)
    }

    private val Instruction.Change.mandatory: Boolean
      get() = intensity == null || intensity == MANDATORY

    private fun InstructionTree.isMandatoryGainOf(className: ClassName): Boolean =
        this is Gain && gaining == className.expression && mandatory

    private fun Instruction.gainingAt(area: ClassName): Boolean =
        this is Gain && gaining.isUnfilteredProjectCardAt(area)

    private fun InstructionTree.removingOptionallyAt(area: ClassName): Boolean =
        this is Remove && removing.isProjectCardAt(area) && intensity == OPTIONAL

    private fun Expression.isProjectCardAt(area: ClassName): Boolean =
        className == PROJECT_CARD && arguments.singleOrNull()?.className == area

    private fun Expression.isUnfilteredProjectCardAt(area: ClassName): Boolean =
        isProjectCardAt(area) && refinement == null

    private fun malformed(source: InstructionTree): Nothing =
        throw PetSyntaxException("Unsupported $TRANSFORM_KIND card operation: $source")

    private val BUY_SELECTED_CARDS = cn("BuySelectedCards")
    private val EVENT_PILE = cn("EventPile")
    private val SELECTING = cn("Selecting")
    private val REVEALED = cn("Revealed")
  }
}
