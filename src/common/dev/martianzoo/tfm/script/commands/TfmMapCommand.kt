package dev.martianzoo.tfm.script.commands

import dev.martianzoo.pets.data.Player
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.tfm.script.TfmMapRenderer

internal class TfmMapCommand(repl: ScriptSession) : AbstractTfmCommand(repl, "tfm_map") {
  override val usage = "map"
  override val help =
      """
        I mean it shows a map.
      """
  override val isReadOnly = true

  override fun noArgs() =
      TfmMapRenderer(
              repl.game.reader,
              repl.game.actors.filterIsInstance<Player>(),
              repl.useAnsiColors,
          )
          .render()
}
