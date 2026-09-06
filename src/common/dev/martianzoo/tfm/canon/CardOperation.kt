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
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.canon.TfmClasses.PROJECT_CARD

/** A validated semantic view of one canonical `CARDS[...]` instruction. */
public sealed interface CardOperation {
  /** Reveal cards until the requested matching cards have been found. */
  public data class Search(public val cards: Gain, public val filter: Requirement) : CardOperation

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

  public companion object {
    public const val TRANSFORM_KIND: String = "CARDS"

    /** Interprets a validated canonical card operation after its marker has been removed. */
    public fun decode(source: InstructionTree): CardOperation =
        when (source) {
          is Gain -> decodeSearch(source)
          is InstructionGroup -> decodeRevealAndPurchase(source)
          is Then -> decodeRevealAndTest(source)
          else -> malformed(source)
        }

    private fun decodeSearch(source: Gain): Search {
      if (
          source.gaining.className != SEARCH_FOR_CARD ||
              source.gaining.argumentsSpecified ||
              !source.mandatory
      ) {
        malformed(source)
      }
      val filter = source.gaining.refinement?.requirement ?: malformed(source)
      return Search(source, filter)
    }

    private fun decodeRevealAndPurchase(source: InstructionGroup): RevealAndPurchase {
      if (source.instructions.size != 2) malformed(source)
      val offered =
          source.instructions.filterIsInstance<Gain>().singleOrNull {
            it.gaining.isUnfilteredProjectCardAt(SELECTING) && it.mandatory
          } ?: malformed(source)
      val purchase = source.instructions.single { it !== offered } as? Then ?: malformed(source)
      if (purchase.instructions.size != 3) malformed(source)
      val retained = purchase.first as? Transmute ?: malformed(source)
      val discarded = purchase.instructions[1] as? Remove ?: malformed(source)
      if (
          retained.gaining.className != PROJECT_CARD ||
              retained.gaining.arguments.singleOrNull()?.className != HAND ||
              !retained.removing.isProjectCardAt(SELECTING) ||
              retained.removing.refinement != null ||
              retained.count != offered.count ||
              retained.intensity != AMAP ||
              !discarded.removingOptionallyAt(SELECTING) ||
              discarded.count != offered.count ||
              !purchase.instructions.last().isMandatoryGainOf(BUY_SELECTED_CARDS)
      ) {
        malformed(source)
      }
      val filter = retained.gaining.refinement?.requirement ?: malformed(source)
      if (offered.count !is ActualScalar) malformed(source)
      return RevealAndPurchase(offered, retained, filter)
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

    private val Instruction.Change.mandatory: Boolean
      get() = intensity == null || intensity == MANDATORY

    private fun InstructionTree.isMandatoryGainOf(className: ClassName): Boolean =
        this is Gain && gaining == className.expression && mandatory

    private fun InstructionTree.removingOptionallyAt(area: ClassName): Boolean =
        this is Remove && removing.isProjectCardAt(area) && intensity == OPTIONAL

    private fun Expression.isProjectCardAt(area: ClassName): Boolean =
        className == PROJECT_CARD && arguments.singleOrNull()?.className == area

    private fun Expression.isUnfilteredProjectCardAt(area: ClassName): Boolean =
        isProjectCardAt(area) && refinement == null

    private fun malformed(source: InstructionTree): Nothing =
        throw PetSyntaxException("Unsupported $TRANSFORM_KIND card operation: $source")

    private val BUY_SELECTED_CARDS = cn("BuySelectedCards")
    private val HAND = cn("Hand")
    private val SELECTING = cn("Selecting")
    private val REVEALED = cn("Revealed")
    private val SEARCH_FOR_CARD = cn("SearchForCard")
  }
}
