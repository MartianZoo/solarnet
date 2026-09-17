package dev.martianzoo.tfm.script.commands

import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptSession

internal abstract class AbstractTfmCommand(internal val repl: ScriptSession, name: String) :
    ScriptCommand(name) {
  internal fun tfm() = repl.agents.tfm(repl.agent.actor)
}
