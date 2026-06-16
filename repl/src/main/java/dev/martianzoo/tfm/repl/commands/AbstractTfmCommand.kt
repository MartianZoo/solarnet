package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.tfm.engine.TfmGameplay

internal abstract class AbstractTfmCommand(internal val repl: ReplSession, name: String) :
    ReplCommand(name) {
  internal fun tfm() = TfmGameplay(repl.game, repl.gameplay.player, repl.gameplay)
}
