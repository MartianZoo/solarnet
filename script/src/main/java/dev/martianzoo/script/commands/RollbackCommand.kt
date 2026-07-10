package dev.martianzoo.script.commands

import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession

internal class RollbackCommand(private val repl: ScriptSession) : ScriptCommand("rollback") {
  override val usage = "rollback <logid>"
  override val help =
      """
        Undoes the event with the id given and every event after it. If you undo too far,
        you can't go forward again (you can only try to reconstruct the game from your
        ~/.rego_history). If you want to undo your command `exec 5 Plant`, look for the number in
        the command prompt on that line; that's the number to use here. Or check `log`. Be careful
        though, as you it will let you undo to a position when the engine was in the middle of
        doing stuff, which would put you in an invalid game state.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.checkpointIds()

  override fun withArgs(args: String): List<String> {
    repl.game.timeline.rollBack(Checkpoint(args.toInt()))
    return listOf("Rollback done")
  }
}
