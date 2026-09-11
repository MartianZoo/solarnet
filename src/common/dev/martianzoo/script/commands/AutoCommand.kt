package dev.martianzoo.script.commands

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException

internal class AutoCommand(private val repl: ScriptSession) : ScriptCommand("auto") {
  override val usage = "auto [none|concrete|eager]"
  override val help =
      """
        Controls how eagerly the engine executes pending tasks. `none` leaves every task for you.
        `concrete` executes a task only when no other task could currently succeed, preserving every
        choice allowed by the game. `eager` also makes arbitrary, reproducible choices when several
        tasks could succeed; this is convenient, but it can make suboptimal moves. With no
        argument, `auto` reports the current policy.
      """

  override fun noArgs() = listOf("Autoexec policy is: ${repl.agent.autoExecPolicy}")

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.completions("none", "concrete", "eager", group = "auto policies")

  override fun withArgs(args: String): List<String> {
    val policy =
        when (args) {
          "none" -> NONE
          "concrete" -> CONCRETE
          "eager" -> EAGER
          else -> throw UsageException()
        }
    repl.agent.autoExecPolicy = policy
    return noArgs()
  }
}
