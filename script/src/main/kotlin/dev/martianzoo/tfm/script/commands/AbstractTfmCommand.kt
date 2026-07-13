package dev.martianzoo.tfm.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.tfm.engine.TfmGameplay

internal abstract class AbstractTfmCommand(internal val repl: ScriptSession, name: String) :
    ScriptCommand(name) {
  internal fun tfm() = TfmGameplay(repl.game, repl.gameplay.player, repl.gameplay)
}
