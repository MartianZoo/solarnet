package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.data.CardOperation
import dev.martianzoo.tfm.data.CardOperation.FilteredPurchase
import dev.martianzoo.tfm.data.CardOperation.RecoverEvents
import dev.martianzoo.tfm.data.CardOperation.RevealAndTest
import dev.martianzoo.tfm.data.CardOperation.Search
import dev.martianzoo.tfm.data.CardOperation.SelectAndKeep
import dev.martianzoo.tfm.data.CardOperation.SelectAndPurchase

internal fun renderCardOperation(transform: Transform, describers: Describers): List<Clause>? {
  if (transform.transformKind != CardOperation.TRANSFORM_KIND) return null
  return when (val operation = CardOperation.decode(transform)) {
    is Search -> renderSearch(operation, describers)?.let(::listOf)
    is SelectAndKeep -> renderSelection(operation)
    is SelectAndPurchase -> renderPurchaseSelection()
    is FilteredPurchase -> renderFilteredPurchase(operation, describers)
    is RevealAndTest -> renderRevealAndTest(operation, describers)
    is RecoverEvents -> renderEventRecovery(operation)?.let(::listOf)
  }
}

private fun renderSearch(operation: Search, describers: Describers): Clause? {
  val criterion = describers.cardCriterion(operation.filter) ?: return null
  val count = operation.cards.count.quantity()
  return clause("draw", matchingCards(criterion, count, describers))
}

private fun renderSelection(operation: SelectAndKeep): List<Clause> {
  val offered = countedProjectCards(operation.offered.count.quantity())
  val retained = retainedCards(operation.retained.count.quantity())
  return listOf(
      clause("look at", offered),
      clause("draw", retained),
  )
}

private fun renderPurchaseSelection(): List<Clause> =
    listOf(
        clause("look at", "1 project card"),
        Clause.Simple(
            Predicate("may buy", Coordination.one(NounPhrase.text("it"))),
            NounPhrase.text("you"),
        ),
    )

private fun renderFilteredPurchase(
    operation: FilteredPurchase,
    describers: Describers,
): List<Clause>? {
  val criterion = describers.cardCriterion(operation.filter) ?: return null
  val offered = countedProjectCards(operation.offered.count.quantity())
  val matching = anyMatchingCards(criterion, describers)
  return listOf(
      clause("look at", offered),
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

private fun renderEventRecovery(operation: RecoverEvents): Clause? {
  val count = operation.recovered.count.fixedQuantity() ?: return null
  val cards =
      if (count == 1) "one of your played event cards" else "$count of your played event cards"
  return clause("return", "up to $cards", Modifier.Phrase("to your hand"))
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
        Quantity.Fixed(1) ->
            "${describers.indefiniteArticle(matchingCardNoun(criterion, true, describers))} "
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
  return when (quantity) {
    Quantity.Fixed(1) -> "1 project card"
    is Quantity.Fixed -> "${quantity.count} project cards"
    is Quantity.Variable -> "$quantity project cards"
  }
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
