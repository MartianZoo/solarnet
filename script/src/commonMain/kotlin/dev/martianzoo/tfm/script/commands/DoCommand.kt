package dev.martianzoo.tfm.script.commands

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.RoutineContext
import dev.martianzoo.engine.RoutineException
import dev.martianzoo.engine.RoutineProvider
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.ScriptMode.PURPLE
import dev.martianzoo.script.ScriptSession.UsageException
import dev.martianzoo.tfm.script.RoutineCall

/** Temporary REPL entry point for catalog-contributed Routine calls. */
internal class DoCommand(private val repl: ScriptSession) : ScriptCommand("do") {
  override val usage: String = "do <RoutineCall>"
  override val help: String =
      """
        Executes one catalog-contributed Routine call in purple mode.
        `DO` disables player-task autoexecution for the current Actor; Engine-owned workflow
        remains active.
      """

  override fun withArgs(args: String): List<String> {
    if (repl.mode != PURPLE) throw UsageException("DO requires purple mode")
    val call = RoutineCall.parse(args)
    repl.setAutoExecMode(NONE)

    val provider =
        repl.game.reader.catalog as? RoutineProvider
            ?: throw UsageException("This Catalog contributes no Routines")
    val routine =
        provider.routines[call.name] ?: throw UsageException("Unknown Routine: ${call.name}")
    val result =
        try {
          routine.execute(RoutineContext(repl.game, repl.gameplay), call.arguments)
        } catch (e: RoutineException) {
          throw UsageException(e.message)
        }
    return repl.describeExecutionResults(result)
  }
}
