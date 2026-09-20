package dev.martianzoo.generated

import dev.martianzoo.pets.data.GameConfig

/**
 * Creates a [GameConfig] from generated rich Class literals. [extra] accepts the same
 * comma-or-newline-separated positive and negative Class names as [GameConfig].
 */
public fun gameConfig(
    modules: List<Class<Module>> = emptyList(),
    milestones: List<Class<Milestone<*>>> = emptyList(),
    awards: List<Class<Award>> = emptyList(),
    colonyTiles: List<Class<ColonyTile>> = emptyList(),
    cardFronts: List<Class<CardFront<*, *>>> = emptyList(),
    extra: String = "",
    playerNames: List<String> = emptyList(),
): GameConfig {
  val generatedNames =
      sequenceOf(modules, milestones, awards, colonyTiles, cardFronts)
          .flatMap { classes -> classes.asSequence() }
          .joinToString(separator = ",") { it.name.toString() }
  return GameConfig(
      listOf(generatedNames, extra).filter(String::isNotBlank).joinToString("\n"),
      *playerNames.toTypedArray(),
  )
}
