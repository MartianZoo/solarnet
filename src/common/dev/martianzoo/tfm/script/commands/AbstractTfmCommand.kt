package dev.martianzoo.tfm.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

internal abstract class AbstractTfmCommand(internal val repl: ScriptSession, name: String) :
    ScriptCommand(name) {
  internal fun tfm() = repl.agents.tfm(repl.agent.actor)
}
