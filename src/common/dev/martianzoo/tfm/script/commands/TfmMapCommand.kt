package dev.martianzoo.tfm.script.commands

import dev.martianzoo.state.Player
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptSession
import dev.martianzoo.tfm.script.TfmMapRenderer

internal class TfmMapCommand(private val repl: ScriptSession) : ScriptCommand("tfm_map") {
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
