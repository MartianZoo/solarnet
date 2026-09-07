package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.canon.CardOperation
import dev.martianzoo.tfm.canon.CardOperation.RevealAndPurchase
import dev.martianzoo.tfm.canon.CardOperation.RevealAndTest
import dev.martianzoo.tfm.canon.CardOperation.Search

internal fun renderCardOperation(transform: Transform, describers: Describers): List<Clause>? {
  if (transform.transformKind != CardOperation.TRANSFORM_KIND) return null
  val operation =
      try {
        CardOperation.decode(transform.instruction)
      } catch (_: PetSyntaxException) {
        return null
      }
  return when (operation) {
    is Search -> renderSearch(operation, describers)?.let(::listOf)
    is RevealAndPurchase -> renderRevealAndPurchase(operation, describers)
    is RevealAndTest -> renderRevealAndTest(operation, describers)
  }
}

internal fun renderAdjacentCardInstructions(
    first: Instruction,
    second: Instruction,
    describers: Describers,
): List<Pair<Instruction, Clause>>? {
  val offered = first as? Gain ?: return null
  val family = offered.selectedCardFamily(describers) ?: return null
  val offeredQuantity = offered.count.quantity()
  val offeredCards = countedCards(family, offeredQuantity, describers)

  (second as? Transmute)?.let { retained ->
    if (!retained.movesCards(family, SELECTING, HAND)) return@let
    val retainedQuantity = retained.count.quantity()
    return listOf(
        offered to clause("look at", offeredCards),
        retained to clause("draw", retainedCards(retainedQuantity)),
    )
  }

  (second as? Then)?.let { purchase ->
    val discarded = purchase.stages.singleOrNull() as? Remove ?: return@let
    val buy = purchase.continuation as? Gain ?: return@let
    if (
        discarded.intensity.modality() != Modality.OPTIONAL ||
            discarded.count != offered.count ||
            !discarded.removing.isCardAt(family, SELECTING) ||
            buy.intensity.modality() != Modality.REQUIRED ||
            buy.count.fixedQuantity() != 1 ||
            !buy.gaining.simple ||
            buy.gaining.className != BUY_SELECTED_CARDS
    ) {
      return@let
    }
    val objectPhrase = if (offeredQuantity == Quantity.Fixed(1)) "it" else "any of them"
    return listOf(
        offered to clause("look at", offeredCards),
        purchase to
            Clause.Simple(
                Predicate(Verb("may buy"), Coordination.one(NounPhrase.text(objectPhrase))),
                NounPhrase.you(),
            ),
    )
  }

  (second as? Gain)?.let { play ->
    val offeredCount = offered.count.fixedQuantity() ?: return@let
    if (
        offeredCount <= 1 ||
            play.intensity.modality() != Modality.REQUIRED ||
            play.count.fixedQuantity() != 1 ||
            play.gaining.className != PLAY_CARD ||
            play.gaining.arguments.none { it.className == SELECTING } ||
            describers.representedClass(play.gaining)?.className != family
    ) {
      return@let
    }
    val clauses =
        listOf(
            clause("draw", offeredCards),
            clause(
                "discard",
                countedCards(family, Quantity.Fixed(offeredCount - 1), describers),
            ),
            clause(
                "play",
                "a ${describers.componentNoun(family, 1)}",
            ),
        )
    return listOf(offered to Clause.Coordinated(Coordination(clauses, Conjunction.THEN)))
  }

  return null
}

internal fun renderCardRevealAndRestore(
    sequence: Then,
    describers: Describers,
): Clause? {
  if (sequence.instructions.size != 3) return null
  val revealed = sequence.stages.getOrNull(0) as? Transmute ?: return null
  val restored = sequence.stages.getOrNull(1) as? Transmute ?: return null
  val outcome = sequence.continuation as? Gain ?: return null
  if (
      revealed.count.variableQuantity()?.multiple != 1 ||
          restored.count != revealed.count ||
          outcome.count != revealed.count ||
          !revealed.movesCards(PROJECT_CARD, HAND, REVEALED) ||
          !restored.movesCards(PROJECT_CARD, REVEALED, HAND)
  ) {
    return null
  }
  val normalized =
      Instruction.Per(
          Gain.gain(scaledEx(outcome.gaining, 1), OPTIONAL),
          Metric.Count(PROJECT_CARD.expression),
      )
  return renderInstructions(normalized, describers).clauses.singleOrNull()
}

internal fun renderPlayedEventRecovery(
    transmute: Transmute,
    describers: Describers,
): Clause? {
  if (
      transmute.intensity.modality() != Modality.OPTIONAL ||
          !transmute.gaining.simple ||
          transmute.gaining.className != PROJECT_CARD ||
          !transmute.removing.simple ||
          transmute.removing.className != PLAYED_EVENT ||
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

private fun Gain.selectedCardFamily(describers: Describers): ClassName? {
  if (
      intensity.modality() != Modality.REQUIRED ||
          gaining.refinement != null ||
          gaining.complement ||
          gaining.arguments.singleOrNull()?.className != SELECTING ||
          describers.changeFrame(gaining.className) != ComponentDescriber.ChangeFrame.Deck
  ) {
    return null
  }
  return gaining.className
}

private fun Transmute.movesCards(
    family: ClassName,
    from: ClassName,
    to: ClassName,
): Boolean =
    intensity.modality() == Modality.REQUIRED &&
        gaining.isCardAt(family, to) &&
        removing.isCardAt(family, from)

private fun Expression.isCardAt(family: ClassName, area: ClassName): Boolean =
    className == family &&
        refinement == null &&
        !complement &&
        arguments.singleOrNull()?.className == area

private fun countedCards(
    family: ClassName,
    quantity: Quantity,
    describers: Describers,
): String {
  val singular = if (family == PROJECT_CARD) "project card" else describers.componentNoun(family, 1)
  val plural = if (family == PROJECT_CARD) "project cards" else describers.componentNoun(family, 2)
  return when (quantity) {
    Quantity.Fixed(1) -> "1 $singular"
    is Quantity.Fixed -> "${quantity.count} $plural"
    is Quantity.Variable -> "$quantity $plural"
  }
}

private fun retainedCards(quantity: Quantity): String =
    when (quantity) {
      Quantity.Fixed(1) -> "one of them"
      is Quantity.Fixed -> "${quantity.count} of them"
      is Quantity.Variable -> "$quantity of them"
    }

private fun renderSearch(operation: Search, describers: Describers): Clause? {
  val criterion = describers.cardCriterion(operation.filter) ?: return null
  val count = operation.cards.count.quantity()
  return clause("draw", matchingCards(criterion, count, describers))
}

private fun renderRevealAndPurchase(
    operation: RevealAndPurchase,
    describers: Describers,
): List<Clause>? {
  val criterion = describers.cardCriterion(operation.filter) ?: return null
  val revealed = countedProjectCards(operation.revealed.count.quantity())
  val matching = anyMatchingCards(criterion, describers)
  return listOf(
      clause("reveal", revealed),
      clause("draw", matching, Modifier.Phrase("for free")),
      Clause.Simple(
          Predicate(Verb("may buy"), Coordination.one(NounPhrase.text("each other card"))),
          NounPhrase.you(),
      ),
  )
}

private fun renderRevealAndTest(
    operation: RevealAndTest,
    describers: Describers,
): List<Clause>? {
  val criterion = describers.cardCriterion(operation.filter) ?: return null
  val outcome = renderChange(operation.outcome, describers).value ?: return null
  return listOf(
      clause("reveal", "1 project card"),
      Clause.Prefaced(
          Clause.Preface.Conditional(
              Clause.Simple(matchPredicate(criterion, describers), NounPhrase.text("it"))
          ),
          outcome,
      ),
  )
}

private fun matchingCards(
    criterion: CardCriterion,
    quantity: Quantity,
    describers: Describers,
): String {
  val count = (quantity as? Quantity.Fixed)?.count
  val singular = count == 1
  val prefix =
      when (quantity) {
        Quantity.Fixed(1) -> "1 "
        is Quantity.Fixed -> "${quantity.count} "
        is Quantity.Variable -> "$quantity "
      }
  return prefix + matchingCardNoun(criterion, singular, describers)
}

private fun anyMatchingCards(criterion: CardCriterion, describers: Describers): String =
    "any ${matchingCardNoun(criterion, false, describers)}"

private fun matchingCardNoun(
    criterion: CardCriterion,
    singular: Boolean,
    describers: Describers,
): String =
    when (criterion) {
      is CardCriterion.Tag -> {
        val tag = checkNotNull(describers.tagName(criterion.className))
        "$tag ${if (singular) "card" else "cards"}"
      }
      CardCriterion.NoTags -> "${if (singular) "card" else "cards"} with no tags"
      CardCriterion.HasRequirement ->
          "${if (singular) "card" else "cards"} with ${if (singular) "a requirement" else "requirements"}"
      is CardCriterion.ResourceIcon -> {
        val resource = checkNotNull(describers.cardResourceNoun(criterion.className, 1))
        "${if (singular) "card" else "cards"} with $resource ${if (singular) "icon" else "icons"}"
      }
    }

private fun matchPredicate(criterion: CardCriterion, describers: Describers): Predicate =
    when (criterion) {
      is CardCriterion.Tag -> {
        val tag = checkNotNull(describers.tagName(criterion.className))
        Predicate(
            Verb.HAVE,
            Coordination.one(NounPhrase("$tag tag", determiner = Determiner.INDEFINITE)),
        )
      }
      CardCriterion.NoTags ->
          Predicate(
              Verb.HAVE,
              Coordination.one(
                  NounPhrase(
                      "tag",
                      "tags",
                      determiner = Determiner.NO,
                      grammaticalNumber = NounPhrase.GrammaticalNumber.PLURAL,
                  )
              ),
          )
      CardCriterion.HasRequirement ->
          Predicate(
              Verb.HAVE,
              Coordination.one(NounPhrase("requirement", determiner = Determiner.INDEFINITE)),
          )
      is CardCriterion.ResourceIcon -> {
        val resource = checkNotNull(describers.cardResourceNoun(criterion.className, 1))
        Predicate(
            Verb.HAVE,
            Coordination.one(
                NounPhrase(
                    "$resource icon",
                    determiner = Determiner.INDEFINITE,
                )
            ),
        )
      }
    }

private fun countedProjectCards(quantity: Quantity): String {
  return when (quantity) {
    Quantity.Fixed(1) -> "1 project card"
    is Quantity.Fixed -> "${quantity.count} project cards"
    is Quantity.Variable -> "$quantity project cards"
  }
}

private fun clause(verb: String, objectPhrase: String, vararg modifiers: Modifier): Clause.Simple =
    Clause.Simple(
        Predicate(
            Verb(verb),
            Coordination.one(NounPhrase.text(objectPhrase)),
            modifiers.toList(),
        )
    )

private val BUY_SELECTED_CARDS = cn("BuySelectedCards")
private val HAND = cn("Hand")
private val PLAYED_EVENT = cn("PlayedEvent")
private val PLAY_CARD = cn("PlayCard")
private val PROJECT_CARD = cn("ProjectCard")
private val REVEALED = cn("Revealed")
private val SELECTING = cn("Selecting")
