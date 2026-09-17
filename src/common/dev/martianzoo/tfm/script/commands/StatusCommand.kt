package dev.martianzoo.tfm.script.commands

import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptSession

internal class StatusCommand(private val repl: ScriptSession) : ScriptCommand("status") {
  override val usage = "status"
  override val help = "Shows the current world: bundles, phase, player, and timeline checkpoint."
  override val isReadOnly = true

  override fun noArgs() = listOf(repl.promptPlain())
}
