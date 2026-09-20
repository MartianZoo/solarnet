package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameWorld
import dev.martianzoo.tfm.canon.cardResourceType
import dev.martianzoo.tfm.canon.tfmCatalog

/** Current face-up cards, retaining the order in which they first entered play. */
internal fun playedCards(game: GameWorld, player: Player): List<Type> {
  val current =
      game.reader
          .getComponents("CardFront")
          .elements
          .filter { type ->
            type.typeDependencies.any { it.boundType.className == player.className }
          }
          .toSet()
  return game.events
      .changesSince(Checkpoint(0))
      .asSequence()
      .mapNotNull { it.change.gaining }
      .map { it.type }
      .filter(current::contains)
      .distinct()
      .toList()
}

internal fun cardImageDirectory(card: Type): String? {
  val representedClasses =
      card.typeDependencies.mapNotNull { it.boundType.representedClass?.className?.toString() }
  return when {
    "CorporationCard" in representedClasses -> "corporations"
    "PreludeCard" in representedClasses -> "preludes"
    "ProjectCard" in representedClasses -> "projects"
    else -> null
  }
}

/** This card's configured resource type and live count for [player], when it can hold resources. */
internal fun cardResourceCount(
    reader: GameReader,
    player: Player,
    card: Type,
): Pair<ClassName, Int>? {
  val resourceType = cardResourceType(reader.tfmCatalog.card(card.className)) ?: return null
  return resourceType to GameQueries(reader).count(player, "$resourceType<${card.className}>")
}

/** Whether this action card has its generational used marker at the current recording position. */
internal fun hasActionUsedMarker(reader: GameReader, player: Player, card: Type): Boolean {
  if (!card.isSubtypeOf(reader.resolve(cn("ActionCard").expression))) return false
  return GameQueries(reader).count(player, "ActionUsedMarker<${card.className}>") > 0
}

/** Event cards in this player's played-event pile, retaining their play order. */
internal fun playedEventCards(game: GameWorld, player: Player): List<ClassName> {
  val current = game.reader.getComponents("PlayedEvent").elements.toSet()
  return game.events
      .changesSince(Checkpoint(0))
      .asSequence()
      .filter { it.actor == player }
      .mapNotNull { it.change.gaining }
      .map { it.type }
      .filter(current::contains)
      .mapNotNull { playedEvent ->
        playedEvent.typeDependencies
            .mapNotNull { it.boundType.representedClass?.className }
            .singleOrNull()
      }
      .distinct()
      .toList()
}

private val supportedPlayerColors = listOf("red", "yellow", "green", "blue", "purple")

internal fun assignPlayerColors(playerNames: List<String>): List<String> {
  val unclaimedColors = supportedPlayerColors.toMutableList()
  val assignedColors = MutableList<String?>(playerNames.size) { null }
  playerNames.forEachIndexed { index, playerName ->
    val requestedColor = playerName.lowercase()
    if (unclaimedColors.remove(requestedColor)) assignedColors[index] = requestedColor
  }
  var reusedColorIndex = 0
  assignedColors.indices
      .filter { assignedColors[it] == null }
      .forEach { index ->
        assignedColors[index] =
            if (unclaimedColors.isNotEmpty()) unclaimedColors.removeFirst()
            else supportedPlayerColors[reusedColorIndex++ % supportedPlayerColors.size]
      }
  return assignedColors.filterNotNull()
}
