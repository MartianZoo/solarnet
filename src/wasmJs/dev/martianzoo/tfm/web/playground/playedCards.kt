package dev.martianzoo.tfm.web.playground

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Component
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.cardResourceType

/** Face-up non-event cards, ordered by when each card first entered play. */
internal fun playedCards(
    reader: GameReader,
    events: EventLog,
    player: Player,
): List<Specimen.PlayedCard> {
  val queries = GameQueries(reader)
  val table = reader.classTable
  val eventCard = table.getClass(cn("EventCard"))
  val current =
      reader
          .getComponents(cn("CardFront").expression)
          .elements
          .filter { Component(it).owningPlayer == player && !it.rootClass.isSubtypeOf(eventCard) }
          .toSet()
  return events
      .changesSince(Checkpoint(0))
      .mapNotNull { it.change.gaining?.type }
      .filter { it in current }
      .map { it.rootClass }
      .distinctBy { it.className }
      .map { card ->
        val resourceType = cardResourceType(card)
        val kind =
            when {
              cardBack(card)?.isSubtypeOf(table.getClass(cn("CorporationCard"))) == true ->
                  Specimen.CardKind.Corporation
              cardBack(card)?.isSubtypeOf(table.getClass(cn("PreludeCard"))) == true ->
                  Specimen.CardKind.Prelude
              card.isSubtypeOf(table.getClass(cn("AutomatedCard"))) -> Specimen.CardKind.Automated
              else -> Specimen.CardKind.Active
            }
        Specimen.PlayedCard(
            name = card.className.toString(),
            kind = kind,
            resources =
                resourceType?.let { queries.count(player, "CardResource<${card.className}>") },
            resourceType = resourceType?.toString(),
            actionUsed =
                card.isSubtypeOf(table.getClass(cn("ActionCard"))) &&
                    queries.count(player, "ActionUsedMarker<${card.className}>") > 0,
        )
      }
}
