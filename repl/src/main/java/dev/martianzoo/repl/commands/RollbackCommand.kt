package dev.martianzoo.repl.commands

import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession

internal class RollbackCommand(val repl: ReplSession) : ReplCommand("rollback") {
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

  override fun withArgs(args: String): List<String> {
    repl.tfm.game.timeline.rollBack(Checkpoint(args.toInt()))
    return listOf("Rollback done")
  }
}
