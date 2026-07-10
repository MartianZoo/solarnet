package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptSession

internal class StatusCommand(private val repl: ScriptSession) : ScriptCommand("status") {
  override val usage = "status"
  override val help = "Shows current game state: bundles, phase, player, and timeline checkpoint."
  override val isReadOnly = true

  override fun noArgs() = listOf(repl.promptPlain())
}
