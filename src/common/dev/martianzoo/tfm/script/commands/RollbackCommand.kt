package dev.martianzoo.tfm.script.commands

import dev.martianzoo.state.Checkpoint
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession

internal class RollbackCommand(private val repl: ScriptSession) : ScriptCommand("rollback") {
  override val usage = "rollback <transaction>"
  override val help =
      """
        Returns to a completed game position and discards everything after it. Use
        completion suggestions to find a transaction position. This cannot be undone.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.checkpointIds()

  override fun withArgs(args: String): List<String> {
    val transactionSerial = args.toInt()
    repl.game.timeline.seek(Checkpoint(transactionSerial))
    repl.game.timeline.truncate()
    return listOf("Rollback done")
  }
}
