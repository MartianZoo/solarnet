package dev.martianzoo.tfm.web.playground

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Player

/** Current face-up cards in their first-play order. */
internal fun playedCards(game: GameWorld, player: Player): List<Type> {
  val current =
      game.reader
          .getComponents(cn("CardFront").expression)
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

/** Whether this action card has its generational used marker at the current recording position. */
internal fun hasActionUsedMarker(reader: GameReader, player: Player, card: Type): Boolean {
  if (!card.isSubtypeOf(reader.resolve(cn("ActionCard").expression))) return false
  return GameQueries(reader).count(player, "ActionUsedMarker<${card.className}>") > 0
}
