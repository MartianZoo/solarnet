package dev.martianzoo.tfm.text

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.canon.CardOperation
import dev.martianzoo.tfm.canon.CardOperation.MoveEvents
import dev.martianzoo.tfm.canon.CardOperation.Observe
import dev.martianzoo.tfm.canon.CardOperation.RecoverEvents
import dev.martianzoo.tfm.canon.CardOperation.RevealAndPurchase
import dev.martianzoo.tfm.canon.CardOperation.RevealAndRestore
import dev.martianzoo.tfm.canon.CardOperation.RevealAndTest
import dev.martianzoo.tfm.canon.CardOperation.Search
import dev.martianzoo.tfm.canon.CardOperation.SelectAndKeep
import dev.martianzoo.tfm.canon.CardOperation.SelectAndPlay
import dev.martianzoo.tfm.canon.CardOperation.SelectAndPurchase

internal fun renderCardOperation(transform: Transform, describers: Describers): List<Clause>? {
  if (transform.transformKind != CardOperation.TRANSFORM_KIND) return null
  return when (val operation = CardOperation.decodeOrNull(transform) ?: return null) {
    is Observe -> renderObservation(operation, describers)
    is Search -> renderSearch(operation, describers)?.let(::listOf)
    is SelectAndKeep -> renderSelection(operation)
    is SelectAndPurchase -> renderPurchaseSelection(operation)
    is SelectAndPlay -> renderSelectionAndPlay(operation)
    is RevealAndPurchase -> renderRevealAndPurchase(operation, describers)
    is RevealAndTest -> renderRevealAndTest(operation, describers)
    is RevealAndRestore -> renderRevealAndRestore(operation, describers)
    is RecoverEvents -> renderEventRecovery(operation)?.let(::listOf)
    is MoveEvents -> renderEventMovement(operation, describers)
  }
}

private fun renderObservation(
    operation: Observe,
    describers: Describers,
): List<Clause> =
    renderInstructions(
            cardReferenceNormalizer.transformInstruction(operation.observation),
            describers,
        )
        .clauses

private fun renderSearch(operation: Search, describers: Describers): Clause? {
  val criterion = describers.cardCriterion(operation.filter) ?: return null
  val count = operation.cards.count.quantity()
  return clause("draw", matchingCards(criterion, count, describers))
}

private fun renderSelection(operation: SelectAndKeep): List<Clause> {
  val offered =
      countedCards(operation.offered.gaining.className, operation.offered.count.quantity())
  val retained = retainedCards(operation.retained.count.quantity())
  return listOf(
      clause("look at", offered),
      clause("draw", retained),
  )
}

private fun renderPurchaseSelection(operation: SelectAndPurchase): List<Clause> {
  val count = operation.offered.count.quantity()
  val cards = countedCards(operation.offered.gaining.className, count)
  val objectPhrase = if (count == Quantity.Fixed(1)) "it" else "any of them"
  return listOf(
      clause("look at", cards),
      Clause.Simple(
          Predicate("may buy", Coordination.one(NounPhrase.text(objectPhrase))),
          NounPhrase.text("you"),
      ),
  )
}

private fun renderSelectionAndPlay(operation: SelectAndPlay): List<Clause> {
  val offered = checkNotNull(operation.offered.count.fixedQuantity())
  val retained = checkNotNull(operation.retained.count.fixedQuantity())
  val family = operation.offered.gaining.className
  val clauses =
      listOf(
          clause("draw", countedCards(family, Quantity.Fixed(offered))),
          clause("discard", countedCards(family, Quantity.Fixed(offered - retained))),
          clause(
              "play",
              if (retained == 1) "a ${cardFamilyName(family)}"
              else countedCards(family, Quantity.Fixed(retained)),
          ),
      )
  return listOf(Clause.Coordinated(Coordination(clauses, Conjunction.THEN)))
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
          Predicate("may buy", Coordination.one(NounPhrase.text("each other card"))),
          NounPhrase.text("you"),
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
      Clause.Prefaced("if it ${matchPredicate(criterion, describers)}", outcome),
  )
}

private fun renderRevealAndRestore(
    operation: RevealAndRestore,
    describers: Describers,
): List<Clause> {
  val normalized =
      Instruction.Per(
          Instruction.Gain.gain(scaledEx(operation.outcome.gaining, 1), OPTIONAL),
          Metric.Count(PROJECT_CARD.expression),
      )
  return renderInstructions(normalized, describers).clauses
}

private fun renderEventRecovery(operation: RecoverEvents): Clause? {
  val count = operation.recovered.count.fixedQuantity() ?: return null
  val cards =
      if (count == 1) "one of your played event cards" else "$count of your played event cards"
  return Clause.Simple(
      Predicate(
          "may return",
          Coordination.one(NounPhrase.text("up to $cards")),
          listOf(Modifier.Phrase("to your hand")),
      ),
      NounPhrase.text("you"),
  )
}

private fun renderEventMovement(
    operation: MoveEvents,
    describers: Describers,
): List<Clause>? =
    renderChange(cardReferenceNormalizer.transformInstruction(operation.moved), describers)
        .value
        ?.let(::listOf)

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
        val tag = checkNotNull(describers.tagName(criterion.className)).first
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

private fun matchPredicate(criterion: CardCriterion, describers: Describers): String =
    when (criterion) {
      is CardCriterion.Tag -> {
        val tag = checkNotNull(describers.tagName(criterion.className)).first
        "has ${describers.indefiniteArticle(tag)} $tag tag"
      }
      CardCriterion.NoTags -> "has no tags"
      CardCriterion.HasRequirement -> "has a requirement"
      is CardCriterion.ResourceIcon -> {
        val resource = checkNotNull(describers.cardResourceNoun(criterion.className, 1))
        "has ${describers.indefiniteArticle(resource)} $resource icon"
      }
    }

private fun countedProjectCards(quantity: Quantity): String {
  return countedCards(PROJECT_CARD, quantity)
}

private fun countedCards(family: ClassName, quantity: Quantity): String {
  val singular = cardFamilyName(family)
  val plural = "${singular}s"
  return when (quantity) {
    Quantity.Fixed(1) -> "1 $singular"
    is Quantity.Fixed -> "${quantity.count} $plural"
    is Quantity.Variable -> "$quantity $plural"
  }
}

private fun cardFamilyName(family: ClassName): String =
    when (family) {
      PROJECT_CARD -> "project card"
      CORPORATION_CARD -> "corporation card"
      PRELUDE_CARD -> "prelude card"
      else -> "card"
    }

private fun retainedCards(quantity: Quantity): String =
    when (quantity) {
      Quantity.Fixed(1) -> "one of them"
      is Quantity.Fixed -> "${quantity.count} of them"
      is Quantity.Variable -> "$quantity of them"
    }

private fun clause(verb: String, objectPhrase: String, vararg modifiers: Modifier): Clause.Simple =
    Clause.Simple(
        Predicate(
            verb,
            Coordination.one(NounPhrase.text(objectPhrase)),
            modifiers.toList(),
        )
    )

private val cardReferenceNormalizer =
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

private val CARD_BACK = cn("CardBack")
private val CORPORATION_CARD = cn("CorporationCard")
private val EVENT_PILE = cn("EventPile")
private val HAND = cn("Hand")
private val PLAYED_EVENT = cn("PlayedEvent")
private val PRELUDE_CARD = cn("PreludeCard")
private val PROJECT_CARD = cn("ProjectCard")
