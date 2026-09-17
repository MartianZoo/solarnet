package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.data.Player

/** Actor-contextual read conveniences used by the passive recording UI. */
internal class GameQueries(private val reader: GameReader) {
  private val elaborator = PetElaborator(reader.classTable)

  internal fun count(player: Player, metric: String): Int =
      reader.count(
          elaborator.elaborateMetricInput(
              Parsing.parse<Metric>(metric),
              player.expression,
              player,
          )
      )

  internal fun production(player: Player, kind: ClassName): Int =
      count(player, "PROD[$kind]") - count(player, "ProdOffset<Class<$kind>>")
}
