package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.SIGNAL
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.text.ComponentDescriber.CardLocation as Location
import dev.martianzoo.tfm.text.ComponentDescriber.CardProcedure as Procedure

/** Reads the temporary card locations as one offer or reveal procedure. */
internal fun renderCardOffer(
    instructions: List<Instruction>,
    index: Int,
    describers: Describers,
): Pair<Int, Clause>? {
  val first = instructions.getOrNull(index) ?: return null
  if (first is Gain) {
    val second = instructions.getOrNull(index + 1)
    val third = instructions.getOrNull(index + 2)
    if (second is Transmute && third is Remove) {
      renderKeepOffer(first, second, third, describers)?.let {
        return 3 to it
      }
    }
    if (second is Instruction.Then) {
      val move = second.stages.singleOrNull() as? Transmute
      val discard = second.continuation as? Remove
      if (move != null && discard != null) {
        renderKeepOffer(first, move, discard, describers)?.let {
          return 2 to it
        }
      }
    }
  }
  return null
}

internal fun renderCardOfferSequence(
    sequence: Instruction.Then,
    describers: Describers,
): Clause? =
    renderPlayOffer(sequence, describers)
        ?: renderPurchaseOffer(sequence, describers)
        ?: renderRevealedReward(sequence, describers)
        ?: renderHandReveal(sequence, describers)

private fun renderKeepOffer(
    offer: Gain,
    keep: Transmute,
    discard: Remove,
    describers: Describers,
): Clause? {
  if (!offer.isCardAt(Location.SELECTING, describers)) return null
  if (!keep.moves(offer.gaining, Location.SELECTING, Location.HAND, describers)) return null
  if (!discard.removes(offer.gaining, Location.SELECTING, describers)) return null
  val offered = offer.count.fixedQuantity()
  val kept = keep.count.fixedQuantity()
  val rejected = discard.count.fixedQuantity()
  val linkedX =
      offer.count.variableQuantity()?.takeIf { it.multiple == 1 } != null &&
          kept == 1 &&
          discard.count.variableQuantity() == offer.count.variableQuantity() &&
          discard.quantifier.modality() == Modality.BEST_EFFORT
  if (
      !linkedX &&
          (offered == null || kept == null || rejected == null || kept + rejected != offered)
  )
      return null
  if (!linkedX && discard.quantifier.modality() != Modality.REQUIRED) return null
  val countText = if (linkedX) "that many" else offered.toString()
  val cardNoun = describers.componentNoun(offer.gaining.className, if (offered == 1) 1 else 2)
  return sequence(
      offerClause("look at", NounPhrase.text("$countText $cardNoun")),
      offerClause("keep", NounPhrase.text("${if (kept == 1) "1" else kept} of them")),
      offerClause("discard", NounPhrase.text("the rest")),
  )
}

private fun renderPlayOffer(sequence: Instruction.Then, describers: Describers): Clause? {
  val offer = sequence.stages.getOrNull(0) as? Gain ?: return null
  val discard = sequence.stages.getOrNull(1) as? Remove ?: return null
  val play = sequence.continuation as? Gain ?: return null
  if (sequence.stages.size != 2 || !offer.isCardAt(Location.SELECTING, describers)) return null
  val plays = describers.changeFrame(play.gaining.className) == ComponentDescriber.ChangeFrame.Play
  val playChoice =
      renderPlayOrFizzle(play, describers, NounPhrase.text("the other"))
          ?: if (plays) offerClause("play", NounPhrase.text("the other")) else return null
  if (play.count.fixedQuantity() != 1 || play.quantifier.modality() != Modality.REQUIRED)
      return null
  if (describers.location(play.gaining) != Location.SELECTING) return null
  if (
      !discard.removes(offer.gaining, Location.SELECTING, describers) ||
          discard.quantifier.modality() != Modality.REQUIRED
  )
      return null
  if (
      plays &&
          play.gaining.arguments
              .firstOrNull()
              ?.let(describers::representedClassArgument)
              ?.className != offer.gaining.className
  )
      return null
  val offered = offer.count.fixedQuantity() ?: return null
  if (discard.count.fixedQuantity() != offered - 1 || offered < 2) return null
  val cardNoun = describers.componentNoun(offer.gaining.className, 2)
  return sequence(
      offerClause("look at", NounPhrase.text("$offered $cardNoun")),
      offerClause("discard", NounPhrase.text("${offered - 1} of them")),
      playChoice,
  )
}

/**
 * Reads one card-play choice from the signal's own Pets, with an optional selected-card referent.
 */
internal fun renderPlayOrFizzle(
    signal: Gain,
    describers: Describers,
    selected: NounPhrase? = null,
): Clause? {
  if (
      describers.fact(signal.gaining.className, ComponentDescriber::cardProcedure) !=
          Procedure.PLAY_OR_FIZZLE ||
          signal.count.fixedQuantity() != 1 ||
          signal.quantifier.modality() != Modality.REQUIRED
  )
      return null
  val choice =
      describers
          .declaration(signal.gaining.className)
          .authoredEffectsWithActions
          .singleOrNull()
          ?.instruction as? Instruction.Or ?: return null
  if (choice.instructions.size != 2) return null
  val play = choice.instructions[0] as? Gain ?: return null
  val fallback = choice.instructions[1] as? Instruction.Then ?: return null
  val discard = fallback.stages.firstOrNull() as? Remove ?: return null
  val marker = fallback.stages.getOrNull(1) as? Gain ?: return null
  val compensation = fallback.continuation as? Gain ?: return null
  if (
      fallback.stages.size != 2 ||
          describers.changeFrame(play.gaining.className) != ComponentDescriber.ChangeFrame.Play ||
          play.count.fixedQuantity() != 1 ||
          play.quantifier.modality() != Modality.REQUIRED ||
          discard.count.fixedQuantity() != 1 ||
          discard.quantifier.modality() != Modality.REQUIRED ||
          discard.removing.refinement != null ||
          play.gaining.arguments
              .firstOrNull()
              ?.let(describers::representedClassArgument)
              ?.className != discard.removing.className ||
          play.gaining.arguments.lastOrNull() != discard.removing.arguments.singleOrNull() ||
          marker.count.fixedQuantity() != 1 ||
          marker.quantifier.modality() != Modality.REQUIRED ||
          !marker.gaining.simple ||
          !describers.expressions.isSubtypeOf(marker.gaining.className, SIGNAL) ||
          describers.changeFrame(marker.gaining.className) != null ||
          describers
              .declaration(marker.gaining.className)
              .authoredEffectsWithActions
              .isNotEmpty() ||
          compensation.quantifier.modality() != Modality.REQUIRED ||
          !compensation.gaining.simple ||
          !describers.isStandardResource(compensation.gaining.className)
  )
      return null
  val amount = compensation.count.fixedQuantity() ?: return null
  val card =
      selected
          ?: NounPhrase.text(
              "the selected ${describers.componentNoun(discard.removing.className, 1)}"
          )
  val reward =
      NounPhrase.text("$amount ${describers.componentNoun(compensation.gaining.className, amount)}")
  val playClause = offerClause("play", card)
  val discardClause =
      offerClause("discard", NounPhrase.text("it"))
          .withModifier(Modifier.Relation("for", reward))
          .withModifier(Modifier.Phrase("if it cannot be played"))
  return Clause.Either(Coordination(listOf(playClause, discardClause), Conjunction.OR))
}

private fun renderPurchaseOffer(sequence: Instruction.Then, describers: Describers): Clause? {
  val offer = sequence.stages.firstOrNull() as? Gain ?: return null
  if (!offer.isCardAt(Location.SELECTING, describers)) return null
  val offered = offer.count.fixedQuantity() ?: return null
  val buy = sequence.continuation as? Gain ?: return null
  if (
      describers.fact(buy.gaining.className, ComponentDescriber::cardProcedure) !=
          Procedure.BUY_SELECTED ||
          buy.count.fixedQuantity() != 1 ||
          buy.quantifier.modality() != Modality.REQUIRED
  )
      return null
  val middle = sequence.stages.drop(1)
  val discard = middle.lastOrNull() as? Remove ?: return null
  if (
      !discard.removes(offer.gaining, Location.SELECTING, describers) ||
          discard.quantifier.modality() != Modality.OPTIONAL ||
          discard.count.fixedQuantity() != offered
  )
      return null
  val take = middle.dropLast(1).singleOrNull() as? Gain
  if (middle.size != if (take == null) 1 else 2) return null
  val takeCriterion = take?.let {
    if (
        describers.fact(it.gaining.className, ComponentDescriber::cardProcedure) !=
            Procedure.TAKE_MATCHING ||
            it.quantifier.modality() != Modality.OPTIONAL ||
            it.count.fixedQuantity() != offered
    )
        return null
    it.gaining.arguments.singleOrNull()?.let(describers::cardFilterCriterion) ?: return null
  }
  val cardNoun = describers.componentNoun(offer.gaining.className, offered)
  val offerClause = offerClause("look at", NounPhrase.text("$offered $cardNoun"))
  if (takeCriterion == null) {
    return sequence(
        offerClause,
        offerClause("buy or discard", NounPhrase.text(if (offered == 1) "it" else "each card")),
    )
  }
  val matching = matchingCardNoun(takeCriterion, false, describers)
  return sequence(
      offerClause,
      offerClause("take", NounPhrase.text("any $matching into your hand for free")),
      offerClause("buy or discard", NounPhrase.text("each remaining card")),
  )
}

private fun renderRevealedReward(sequence: Instruction.Then, describers: Describers): Clause? {
  val reveal = sequence.stages.firstOrNull() as? Gain ?: return null
  if (
      sequence.stages.size != 2 ||
          !reveal.isCardAt(Location.REVEALED, describers) ||
          reveal.count.fixedQuantity() != 1
  )
      return null
  val claim = optionalClaim(sequence.stages[1]) ?: return null
  if (
      describers.fact(claim.gaining.className, ComponentDescriber::cardProcedure) !=
          Procedure.CLAIM_REWARD ||
          claim.count.fixedQuantity() != 1 ||
          claim.gaining.arguments.lastOrNull() != describers.thisExpression
  )
      return null
  val criterion =
      claim.gaining.arguments.firstOrNull()?.let(describers::cardFilterCriterion) ?: return null
  val discard = sequence.continuation as? Remove ?: return null
  if (
      !discard.removes(reveal.gaining, Location.REVEALED, describers) ||
          discard.count.fixedQuantity() != 1 ||
          discard.quantifier.modality() != Modality.REQUIRED
  )
      return null
  val noun = describers.componentNoun(reveal.gaining.className, 1)
  val tag = (criterion as? CardCriterion.Tag)?.className?.let(describers::tagName) ?: return null
  val resource = describers.currentCardResourceNoun() ?: return null
  return sequence(
      offerClause("reveal", NounPhrase.text("a $noun")),
      offerClause("add", NounPhrase.text("1 $resource to this card if it has a $tag tag")),
      offerClause("discard", NounPhrase.text("the revealed card")),
  )
}

private fun optionalClaim(instruction: Instruction): Gain? =
    when (instruction) {
      is Gain -> instruction.takeIf { it.quantifier.modality() == Modality.OPTIONAL }
      is Instruction.Or ->
          instruction.instructions
              .takeIf { alternatives ->
                alternatives.size == 2 && alternatives.count { it is Instruction.NoOp } == 1
              }
              ?.filterIsInstance<Gain>()
              ?.singleOrNull()
              ?.takeIf { it.quantifier.modality() == Modality.REQUIRED }
      else -> null
    }

private fun renderHandReveal(sequence: Instruction.Then, describers: Describers): Clause? {
  val out = sequence.stages.getOrNull(0) as? Transmute ?: return null
  val back = sequence.stages.getOrNull(1) as? Transmute ?: return null
  val gain = sequence.continuation as? Gain ?: return null
  if (
      sequence.stages.size != 2 ||
          !out.moves(out.removing, Location.HAND, Location.REVEALED, describers) ||
          !back.moves(out.removing, Location.REVEALED, Location.HAND, describers) ||
          out.count.variableQuantity() != back.count.variableQuantity() ||
          out.count.variableQuantity() != gain.count.variableQuantity() ||
          out.count.variableQuantity()?.multiple != 1 ||
          gain.quantifier.modality() != Modality.REQUIRED ||
          !gain.gaining.simple ||
          !describers.isStandardResource(gain.gaining.className)
  )
      return null
  val cards = describers.componentNoun(out.gaining.className, 2)
  val resource = describers.componentNoun(gain.gaining.className, 1)
  return sequence(
      offerClause("reveal", NounPhrase.text("1 or more $cards from your hand")),
      offerClause("return", NounPhrase.text("them to your hand")),
      offerClause("gain", NounPhrase.text("1 $resource for each card revealed")),
  )
}

private fun Gain.isCardAt(location: Location, describers: Describers): Boolean =
    quantifier.modality() == Modality.REQUIRED &&
        gaining.refinement == null &&
        describers.changeFrame(gaining.className) == ComponentDescriber.ChangeFrame.Deck &&
        describers.location(gaining) == location

private fun Remove.removes(card: Expression, location: Location, describers: Describers): Boolean =
    removing.className == card.className &&
        removing.refinement == null &&
        describers.location(removing) == location

private fun Transmute.moves(
    card: Expression,
    from: Location,
    to: Location,
    describers: Describers,
): Boolean =
    quantifier.modality() == Modality.REQUIRED &&
        gaining.className == card.className &&
        removing.className == card.className &&
        gaining.refinement == null &&
        removing.refinement == null &&
        describers.location(gaining) == to &&
        describers.location(removing) == from

internal fun Describers.location(card: Expression): Location? =
    fact(card.className, ComponentDescriber::cardLocationDependency)
        ?.let { resolveExpression(card)?.sourceDependency(it) }
        ?.takeIf(Expression::simple)
        ?.className
        ?.let {
          fact(it, ComponentDescriber::cardLocation)
        }

private fun sequence(vararg clauses: Clause): Clause =
    Clause.Coordinated(Coordination(clauses.toList(), Conjunction.THEN))

private fun offerClause(verb: String, noun: NounPhrase): Clause.Simple =
    Clause.Simple(Predicate(Verb(verb), Coordination.one(noun)))
