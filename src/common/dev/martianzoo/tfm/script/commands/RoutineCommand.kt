package dev.martianzoo.tfm.script.commands

import dev.martianzoo.tfm.engine.TfmRoutines
import dev.martianzoo.tfm.script.PetsCompletionRoot
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession
import dev.martianzoo.tfm.script.ScriptSession.UsageException

internal class RoutineCommand(
    private val repl: ScriptSession,
    name: String,
    private val routine: TfmRoutines.Definition,
) : ScriptCommand(name) {
  override val usage: String = if (routine.argumentCount == 0..0) "$name()" else "$name(arguments)"
  override val help: String = "Runs the shared Terraforming Mars $name routine."

  override fun noArgs(): List<String> = withArgs("")

  override fun withArgs(args: String): List<String> {
    val arguments = routine.arguments(args)
    if (arguments.size !in routine.argumentCount) throw UsageException()
    val result = repl.agent.doRoutine(name, *arguments.toTypedArray())
    return if (name == "assert") listOf("Assertion passed.")
    else repl.describeExecutionResults(result)
  }

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      when (name) {
        "playCard" -> context.playableCardNames()
        "useAction1",
        "useAction2" -> context.actionCardNames()
        "pay" -> context.paymentWords()
        "action" -> context.classNames()
        "selectTask",
        "exMachina" -> context.petsWords(PetsCompletionRoot.INSTRUCTION)
        else -> emptyList()
      }
}
