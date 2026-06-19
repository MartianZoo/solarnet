package dev.martianzoo.repl.commands

import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession

internal class StatusCommand(private val repl: ReplSession) : ReplCommand("status") {
  override val usage = "status"
  override val help = "Shows current game state: bundles, phase, player, and timeline checkpoint."
  override val isReadOnly = true

  override fun noArgs() = listOf(repl.promptPlain())
}
