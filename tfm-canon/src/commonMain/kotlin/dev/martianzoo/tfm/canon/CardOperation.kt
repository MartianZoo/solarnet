package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
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
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.tfm.canon.TfmClasses.PROJECT_CARD

/** A validated semantic view of one canonical `CARDS[...]` instruction. */
public sealed interface CardOperation {
  /** Observe cards in a represented card area. */
  public data class Observe(public val observation: Instruction.Per) : CardOperation

  /** Reveal cards until the requested matching cards have been found. */
  public data class Search(public val cards: Gain, public val filter: Requirement) : CardOperation

  /** Inspect [offered] cards, retain [retained], and discard the remainder. */
  public data class SelectAndKeep(public val offered: Gain, public val retained: Transmute) :
      CardOperation

  /** Inspect one card and either buy or discard it. */
  public data class SelectAndPurchase(public val offered: Gain) : CardOperation

  /** Inspect cards, retain one, discard the remainder, and play the retained card. */
  public data class SelectAndPlay(
      public val offered: Gain,
      public val retained: Transmute,
      public val play: Gain,
  ) : CardOperation

  /** Reveal cards, retain matching cards for free, and buy or discard the remainder. */
  public data class RevealAndPurchase(
      public val revealed: Gain,
      public val retained: Transmute,
      public val filter: Requirement,
  ) : CardOperation

  /** Reveal one card, discard it, and gain [outcome] when [filter] matches. */
  public data class RevealAndTest(
      public val revealed: Gain,
      public val filter: Requirement,
      public val outcome: Gain,
  ) : CardOperation

  /** Reveal cards from hand, restore those exact cards, and scale [outcome] by their count. */
  public data class RevealAndRestore(
      public val revealed: Transmute,
      public val restored: Transmute,
      public val outcome: Gain,
  ) : CardOperation

  /** Return played Events to the player's hand. */
  public data class RecoverEvents(public val recovered: Transmute) : CardOperation

  /** Move represented played Events without changing which card fronts they represent. */
  public data class MoveEvents(public val moved: Transmute) : CardOperation

  public companion object {
    public const val TRANSFORM_KIND: String = "CARDS"

    /** Validates and interprets [transform] as one canonical card operation. */
    public fun decode(transform: Transform): CardOperation {
      if (transform.transformKind != TRANSFORM_KIND) malformed(transform.instruction)
      return decode(transform.instruction)
    }

    /** Interprets [transform] when it is one of the currently modeled card operations. */
    public fun decodeOrNull(transform: Transform): CardOperation? =
        try {
          decode(transform)
        } catch (_: PetSyntaxException) {
          null
        }

    private fun decode(source: InstructionTree): CardOperation =
        when (source) {
          is Gain -> decodeSearch(source)
          is Instruction.Per -> Observe(source)
          is InstructionGroup -> decodeSelection(source)
          is Then ->
              when {
                source.instructions.last().isMandatoryGainOf(BUY_SELECTED_CARDS) ->
                    decodeRevealAndPurchase(source)
                source.first.gainingCardAt(SELECTING) -> decodeSelectedPlay(source)
                source.first.gainingAt(REVEALED) -> decodeRevealedOperation(source)
                source.first.movingCard(HAND, REVEALED) -> decodeRevealAndRestore(source)
                else -> malformed(source)
              }
          is Transmute -> decodeEventMovement(source)
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
            it.gaining.cardFamilyAt(SELECTING) != null && it.mandatory
          } ?: malformed(source)
      val result = source.instructions.singleOrNull { it !== offered } ?: malformed(source)
      return when (result) {
        is Transmute -> decodeRetainedCards(offered, result)
        is Then -> decodePurchase(offered, result)
        else -> malformed(source)
      }
    }

    private fun decodeRetainedCards(offered: Gain, retained: Transmute): SelectAndKeep {
      val family = offered.gaining.cardFamilyAt(SELECTING) ?: malformed(offered)
      if (
          retained.gaining.cardFamilyAt(HAND) != family ||
              retained.removing.cardFamilyAt(SELECTING) != family ||
              retained.removing.refinement != null ||
              !retained.mandatory
      ) {
        malformed(retained)
      }
      return SelectAndKeep(offered, retained)
    }

    private fun decodePurchase(offered: Gain, purchase: Then): SelectAndPurchase {
      if (
          offered.count !is ActualScalar ||
              purchase.instructions.size != 2 ||
              !purchase.first.removingOptionallyAt(SELECTING) ||
              (purchase.first as Remove).count != offered.count ||
              !purchase.instructions.last().isMandatoryGainOf(BUY_SELECTED_CARDS)
      ) {
        malformed(purchase)
      }
      return SelectAndPurchase(offered)
    }

    private fun decodeRevealedOperation(source: Then): CardOperation =
        if (source.instructions.last().isMandatoryGainOf(BUY_CARDS)) {
          decodeRevealAndPurchase(source)
        } else {
          decodeRevealAndTest(source)
        }

    private fun decodeSelectedPlay(source: Then): SelectAndPlay {
      if (source.instructions.size != 3) malformed(source)
      val offered = source.first as? Gain ?: malformed(source)
      val retained = source.instructions[1] as? Transmute ?: malformed(source)
      val play = source.instructions.last() as? Gain ?: malformed(source)
      val family = offered.gaining.cardFamilyAt(SELECTING) ?: malformed(source)
      val offeredCount = (offered.count as? ActualScalar)?.value ?: malformed(source)
      val retainedCount = (retained.count as? ActualScalar)?.value ?: malformed(source)
      if (
          family !in setOf(CORPORATION_CARD, PRELUDE_CARD) ||
              retained.gaining.cardFamilyAt(HAND) != family ||
              retained.removing.cardFamilyAt(SELECTING) != family ||
              retainedCount >= offeredCount ||
              play.gaining.className != PLAY_CARD ||
              play.gaining.arguments.singleOrNull()?.let {
                it.className == CLASS && it.arguments.singleOrNull()?.className == family
              } != true
      ) {
        malformed(source)
      }
      return SelectAndPlay(offered, retained, play)
    }

    private fun decodeRevealAndPurchase(source: Then): RevealAndPurchase {
      if (source.instructions.last().isMandatoryGainOf(BUY_CARDS)) {
        return decodeLegacyRevealAndPurchase(source)
      }
      if (source.instructions.size != 4) malformed(source)
      val offered = source.first as? Gain ?: malformed(source)
      val retained = source.instructions[1] as? Transmute ?: malformed(source)
      val discarded = source.instructions[2] as? Remove ?: malformed(source)
      if (
          !offered.gaining.isUnfilteredProjectCardAt(SELECTING) ||
              !offered.mandatory ||
              retained.gaining.className != PROJECT_CARD ||
              retained.gaining.arguments.singleOrNull()?.className != HAND ||
              !retained.removing.isProjectCardAt(SELECTING) ||
              retained.removing.refinement != null ||
              retained.count != offered.count ||
              retained.intensity != AMAP ||
              !discarded.removingOptionallyAt(SELECTING) ||
              discarded.count != offered.count
      ) {
        malformed(source)
      }
      val filter = retained.gaining.refinement?.requirement ?: malformed(source)
      if (offered.count !is ActualScalar) malformed(source)
      return RevealAndPurchase(offered, retained, filter)
    }

    private fun decodeLegacyRevealAndPurchase(source: Then): RevealAndPurchase {
      if (source.instructions.size != 3) malformed(source)
      val revealed = source.first as? Gain ?: malformed(source)
      val retained = source.instructions[1] as? Transmute ?: malformed(source)
      if (
          !revealed.gaining.isUnfilteredProjectCardAt(REVEALED) ||
              !revealed.mandatory ||
              retained.gaining.className != PROJECT_CARD ||
              retained.gaining.arguments.any() ||
              !retained.removing.isProjectCardAt(REVEALED) ||
              retained.removing.refinement != null ||
              retained.intensity != AMAP
      ) {
        malformed(source)
      }
      val filter = retained.gaining.refinement?.requirement ?: malformed(source)
      if (revealed.count !is ActualScalar) malformed(source)
      return RevealAndPurchase(revealed, retained, filter)
    }

    private fun decodeRevealAndTest(source: Then): RevealAndTest {
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

    private fun decodeRevealAndRestore(source: Then): RevealAndRestore {
      if (source.instructions.size != 3) malformed(source)
      val revealed = source.first as? Transmute ?: malformed(source)
      val restored = source.instructions[1] as? Transmute ?: malformed(source)
      val outcome = source.instructions.last() as? Gain ?: malformed(source)
      if (
          revealed.count !is XScalar ||
              restored.count !is XScalar ||
              outcome.count !is XScalar ||
              restored.count != revealed.count ||
              outcome.count != revealed.count ||
              !revealed.movingCard(HAND, REVEALED) ||
              !restored.movingCard(REVEALED, HAND)
      ) {
        malformed(source)
      }
      return RevealAndRestore(revealed, restored, outcome)
    }

    private fun decodeEventMovement(source: Transmute): CardOperation {
      if (
          source.gaining.cardFamilyAt(HAND) == PROJECT_CARD &&
              source.removing.isProjectCardAt(EVENT_PILE) &&
              source.intensity == OPTIONAL
      ) {
        return RecoverEvents(source)
      }
      if (
          source.descendantsOfType<Expression>().none {
            it.className == CARD_BACK &&
                it.arguments.any { argument -> argument.className == EVENT_PILE }
          }
      ) {
        malformed(source)
      }
      return MoveEvents(source)
    }

    private val Instruction.Change.mandatory: Boolean
      get() = intensity == null || intensity == MANDATORY

    private fun InstructionTree.isMandatoryGainOf(className: ClassName): Boolean =
        this is Gain && gaining == className.expression && mandatory

    private fun Instruction.gainingAt(area: ClassName): Boolean =
        this is Gain && gaining.isUnfilteredProjectCardAt(area)

    private fun Instruction.gainingCardAt(area: ClassName): Boolean =
        this is Gain && gaining.cardFamilyAt(area) != null

    private fun Instruction.movingCard(from: ClassName, to: ClassName): Boolean =
        this is Transmute &&
            gaining.cardFamilyAt(to) == PROJECT_CARD &&
            removing.cardFamilyAt(from) == PROJECT_CARD

    private fun InstructionTree.removingOptionallyAt(area: ClassName): Boolean =
        this is Remove && removing.isProjectCardAt(area) && intensity == OPTIONAL

    private fun Expression.isProjectCardAt(area: ClassName): Boolean =
        className == PROJECT_CARD && arguments.singleOrNull()?.className == area

    private fun Expression.isUnfilteredProjectCardAt(area: ClassName): Boolean =
        isProjectCardAt(area) && refinement == null

    private fun Expression.cardFamilyAt(area: ClassName): ClassName? = className.takeIf {
      it in setOf(PROJECT_CARD, CORPORATION_CARD, PRELUDE_CARD) &&
          arguments.singleOrNull()?.className == area &&
          refinement == null
    }

    private fun malformed(source: InstructionTree): Nothing =
        throw PetSyntaxException("Unsupported $TRANSFORM_KIND card operation: $source")

    private val BUY_SELECTED_CARDS = cn("BuySelectedCards")
    private val BUY_CARDS = cn("BuyCards")
    private val CARD_BACK = cn("CardBack")
    private val CLASS = cn("Class")
    private val CORPORATION_CARD = cn("CorporationCard")
    private val EVENT_PILE = cn("EventPile")
    private val HAND = cn("Hand")
    private val PLAY_CARD = cn("PlayCard")
    private val PRELUDE_CARD = cn("PreludeCard")
    private val SELECTING = cn("Selecting")
    private val REVEALED = cn("Revealed")
  }
}
