package dev.martianzoo.tfm.web.playground

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player

/** Player-contextual read conveniences for the passive recording display. */
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
