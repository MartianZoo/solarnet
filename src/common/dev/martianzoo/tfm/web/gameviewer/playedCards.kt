package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.cardResourceType
import dev.martianzoo.tfm.canon.tfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

/** Current face-up cards, retaining the order in which they first entered play. */
internal fun playedCards(game: World, player: Player): List<Type> {
  val current =
      game.reader
          .getComponents("CardFront")
          .elements
          .filter { type ->
            type.expressionFull.arguments.any { it.className == player.className }
          }
          .toSet()
  return game.events
      .changesSince(Checkpoint(0))
      .asSequence()
      .mapNotNull { it.change.gaining }
      .map(game.reader::resolve)
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
internal fun cardResourceCount(game: World, player: Player, card: Type): Pair<ClassName, Int>? {
  val resourceType = cardResourceType(game.reader.tfmCatalog.card(card.className)) ?: return null
  return resourceType to game.tfm(player).count("$resourceType<${card.className}>")
}

/** Whether this action card has its generational used marker at the current recording position. */
internal fun hasActionUsedMarker(game: World, player: Player, card: Type): Boolean {
  if (!card.isSubtypeOf(game.reader.resolve(cn("ActionCard").expression))) return false
  return game.tfm(player).count("ActionUsedMarker<${card.className}>") > 0
}

/** Event cards in this player's played-event pile, retaining their play order. */
internal fun playedEventCards(game: World, player: Player): List<ClassName> {
  val current = game.reader.getComponents("PlayedEvent").elements.toSet()
  return game.events
      .changesSince(Checkpoint(0))
      .asSequence()
      .filter { it.actor == player }
      .mapNotNull { it.change.gaining }
      .map(game.reader::resolve)
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
  require(playerNames.size <= supportedPlayerColors.size) {
    "Only ${supportedPlayerColors.size} distinct player colors are available"
  }

  val unclaimedColors = supportedPlayerColors.toMutableList()
  val assignedColors = MutableList<String?>(playerNames.size) { null }
  playerNames.forEachIndexed { index, playerName ->
    val requestedColor = playerName.lowercase()
    if (unclaimedColors.remove(requestedColor)) assignedColors[index] = requestedColor
  }
  assignedColors.indices
      .filter { assignedColors[it] == null }
      .forEach { index ->
        assignedColors[index] = unclaimedColors.removeFirst()
      }
  return assignedColors.filterNotNull()
}
