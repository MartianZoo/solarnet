package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.canon.TfmClasses.MARS_MAP
import dev.martianzoo.tfm.canon.TfmClasses.MC
import dev.martianzoo.tfm.canon.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.engine.TfmApiUtils.standardResourceNames

/** Simple TfM-specific client helper functions, mostly for use by custom instructions. */
public object ApiUtils {
  /**
   * Returns a map with six entries, giving [player]'s current production levels, adjusting mc
   * production to account for our GrossHack.
   */
  public fun lookUpProductionLevels(game: GameReader, player: Expression): Map<ClassName, Int> =
      standardResourceNames(game).associateWith {
        val type = game.resolve(PRODUCTION.of(player, it.classExpression()))
        game.count(type) - if (it == MC) 5 else 0
      }

  /**
   * Returns a map with six entries, giving [player]'s current production levels, adjusting mc
   * production to account for our GrossHack.
   */
  public fun lookUpProductionLevels(game: GameReader, player: Player): Map<ClassName, Int> =
      lookUpProductionLevels(game, player.expression)

  /** Returns the mars map definition being used in this game (there must be exactly one). */
  public fun mapDefinition(game: GameReader): MarsMapDefinition {
    val map = game.resolve(MARS_MAP.expression)
    val mapName = game.getComponents(map).single().className
    return game.tfmCatalog.marsMap(mapName)
  }
}
