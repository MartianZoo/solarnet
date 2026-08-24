package dev.martianzoo.tfm.data

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.Full
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
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.tfm.data.TfmClasses.PROJECT_CARD

/** Compiles identity-bearing card source into follow mode's delegated, count-based operations. */
internal object FollowModeNeutralizer : PetTransformer() {
  private val BUY_CARD = cn("BuyCard")
  private val BUY_SELECTED_CARDS = cn("BuySelectedCards")
  private val CARD_BACK = cn("CardBack")
  private val CLASS = cn("Class")
  private val CORPORATION_CARD = cn("CorporationCard")
  private val EVENT_PILE = cn("EventPile")
  private val HAND = cn("Hand")
  private val PLAY_CARD = cn("PlayCard")
  private val PLAYED_EVENT = cn("PlayedEvent")
  private val PRELUDE_CARD = cn("PreludeCard")

  internal fun neutralize(source: ClassDeclaration): ClassDeclaration =
      source.copy(effects = source.effects.map(::transformEffect))

  override fun transformNode(node: PetNode): PetNode =
      when {
        node is Transform && node.transformKind == CARDS -> neutralizeCards(node.instruction)
        node is Metric.Transform && node.transformKind == CARDS -> neutralizeCardMetric(node.inner)
        node is Requirement.Transform && node.transformKind == CARDS ->
            neutralizeCardRequirement(node.requirement)
        else -> transformChildren(node)
      }

  private fun neutralizeCards(source: InstructionTree): InstructionTree =
      when (source) {
        is Gain -> neutralizeSearch(source)
        is Instruction.Per -> neutralizeObservation(source)
        is InstructionGroup -> neutralizeSelection(source)
        is Then ->
            when {
              source.instructions.last().isMandatoryGainOf(BUY_SELECTED_CARDS) ->
                  neutralizeSelectingPurchase(source)
              source.first.gainingCardAt(SELECTING) -> neutralizeSelectedPlay(source)
              source.first.gainingAt(REVEALED) -> neutralizeReveal(source)
              source.first.movingCard(HAND, REVEALED) -> neutralizeHandReveal(source)
              else -> malformed(source)
            }
        is Transmute -> neutralizeEventMovement(source)
        else -> malformed(source)
      }

  private fun neutralizeSearch(source: Gain): InstructionTree {
    if (source.gaining.className != PROJECT_CARD || source.gaining.arguments.any())
        malformed(source)
    return Gain(scaledEx(PROJECT_CARD.expression, source.count), source.intensity)
  }

  private fun neutralizeSelection(source: InstructionGroup): InstructionTree {
    if (source.instructions.size != 2) malformed(source)
    val offered =
        source.instructions.filterIsInstance<Gain>().singleOrNull {
          it.gaining.cardFamilyAt(SELECTING) != null
        } ?: malformed(source)
    val result = source.instructions.singleOrNull { it !== offered } ?: malformed(source)

    return when (result) {
      is Transmute -> neutralizeRetainedCards(offered, result)
      is Then -> neutralizePurchase(offered, result)
      else -> malformed(source)
    }
  }

  private fun neutralizeRetainedCards(offered: Gain, source: Transmute): InstructionTree {
    val family = offered.gaining.cardFamilyAt(SELECTING) ?: malformed(source)
    if (
        source.gaining.cardFamilyAt(HAND) != family ||
            source.removing.cardFamilyAt(SELECTING) != family ||
            source.removing.refinement != null
    ) {
      malformed(source)
    }
    return Gain(scaledEx(family.expression, source.count), source.intensity)
  }

  private fun neutralizeSelectedPlay(source: Then): InstructionTree {
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
    return Then.create(
        listOf(
            Gain(scaledEx(family.expression, offeredCount), offered.intensity),
            Remove.remove(family.expression, offeredCount - retainedCount, intensity = null),
            play,
        )
    )
  }

  private fun neutralizePurchase(offered: Gain, source: Then): InstructionTree {
    val offeredCount = (offered.count as? ActualScalar)?.value ?: malformed(offered)
    val removal = source.first as? Remove ?: malformed(source)
    if (
        source.instructions.size != 2 ||
            !removal.removingOptionallyAt(SELECTING) ||
            removal.count != offered.count ||
            !source.instructions.last().isMandatoryGainOf(BUY_SELECTED_CARDS)
    ) {
      malformed(source)
    }
    return Gain(scaledEx(BUY_CARD.expression, offeredCount), OPTIONAL)
  }

  private fun neutralizeSelectingPurchase(source: Then): InstructionTree {
    if (source.instructions.size != 4) malformed(source)
    val offered = source.first as? Gain ?: malformed(source)
    val retained = source.instructions[1] as? Transmute ?: malformed(source)
    val discarded = source.instructions[2] as? Remove ?: malformed(source)
    val count = (offered.count as? ActualScalar)?.value ?: malformed(offered)
    if (
        !offered.gaining.isUnfilteredProjectCardAt(SELECTING) ||
            retained.gaining.className != PROJECT_CARD ||
            retained.gaining.arguments.singleOrNull()?.className != HAND ||
            retained.gaining.refinement == null ||
            !retained.removing.isProjectCardAt(SELECTING) ||
            retained.removing.refinement != null ||
            retained.count != offered.count ||
            retained.intensity != AMAP ||
            !discarded.removingOptionallyAt(SELECTING) ||
            discarded.count != offered.count
    ) {
      malformed(source)
    }
    return InstructionGroup(List(count) { followModeBuyOrFreeCard() })
  }

  private fun neutralizeReveal(source: Then): InstructionTree {
    if (source.instructions.size != 2) malformed(source)
    val revealed = source.first as? Gain ?: malformed(source)
    if (revealed.gaining.refinement != null) malformed(source)
    val choices = source.instructions.last() as? Or ?: malformed(source)
    if (choices.instructions.count { it == NoOp } != 1) malformed(source)
    val gated = choices.instructions.filterIsInstance<Gated>().singleOrNull() ?: malformed(source)
    if (
        gated.gate.descendantsOfType<Expression>().none {
          it.isProjectCardAt(REVEALED) && it.refinement != null
        }
    ) {
      malformed(source)
    }
    val outcome = gated.inner as? Gain ?: malformed(source)
    if (outcome.intensity != null && outcome.intensity != MANDATORY) malformed(source)
    return Gain(outcome.scaledEx, OPTIONAL)
  }

  private fun neutralizeHandReveal(source: Then): InstructionTree {
    if (source.instructions.size != 3) malformed(source)
    val reveal = source.first as? Transmute ?: malformed(source)
    val restore = source.instructions[1] as? Transmute ?: malformed(source)
    val outcome = source.instructions.last() as? Gain ?: malformed(source)
    if (
        reveal.count !is XScalar ||
            restore.count !is XScalar ||
            outcome.count !is XScalar ||
            restore.count != reveal.count ||
            outcome.count != reveal.count ||
            !reveal.movingCard(HAND, REVEALED) ||
            !restore.movingCard(REVEALED, HAND)
    ) {
      malformed(source)
    }
    return Instruction.Per(
        Gain(scaledEx(outcome.gaining, 1), OPTIONAL),
        Metric.Count(PROJECT_CARD.expression),
    )
  }

  private fun neutralizeEventMovement(source: Transmute): InstructionTree {
    if (
        source.gaining.cardFamilyAt(HAND) != PROJECT_CARD ||
            !source.removing.isProjectCardAt(EVENT_PILE)
    ) {
      return cardReferenceNeutralizer.transformInstruction(source).also {
        if (it == source) malformed(source)
      }
    }
    return source.copy(fromEx = Full(PROJECT_CARD.expression, PLAYED_EVENT.expression))
  }

  private fun followModeBuyOrFreeCard(): Instruction {
    val free = Gain(scaledEx(PROJECT_CARD.expression, 1), null)
    val buy = Gain(scaledEx(BUY_CARD.expression, 1), OPTIONAL)
    return Or.create(listOf(free, buy))
  }

  private fun neutralizeObservation(source: Instruction.Per): InstructionTree =
      source.copy(metric = neutralizeCardMetric(source.metric))

  private fun neutralizeCardMetric(source: Metric): Metric =
      cardReferenceNeutralizer.transformMetric(source).also {
        if (it == source) malformed(source)
      }

  private fun neutralizeCardRequirement(source: Requirement): Requirement =
      cardReferenceNeutralizer.transformRequirement(source).also {
        if (it == source) malformed(source)
      }

  private fun InstructionTree.isMandatoryGainOf(className: ClassName): Boolean =
      this is Gain &&
          gaining == className.expression &&
          (intensity == null || intensity == MANDATORY)

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

  private fun malformed(source: PetNode): Nothing =
      throw PetSyntaxException("Unsupported $CARDS card operation: $source")

  private val cardReferenceNeutralizer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode =
            when {
              node is Expression && node.className == PROJECT_CARD && node.hasArea(HAND) ->
                  node.withoutArea(PROJECT_CARD, HAND)
              node is Expression && node.className == CARD_BACK && node.hasArea(EVENT_PILE) ->
                  node.withoutArea(PLAYED_EVENT, EVENT_PILE)
              else -> transformChildren(node)
            }

        private fun Expression.hasArea(area: ClassName): Boolean =
            arguments.count { it.className == area } == 1

        private fun Expression.withoutArea(result: ClassName, area: ClassName): Expression =
            copy(
                className = result,
                arguments = arguments.filterNot { it.className == area },
                argumentsSpecified = arguments.size > 1,
            )
      }

  private const val CARDS = "CARDS"
  private val SELECTING = cn("Selecting")
  private val REVEALED = cn("Revealed")
}
