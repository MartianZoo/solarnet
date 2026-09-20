package dev.martianzoo.tfm.script.commands

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.tfm.script.PetsCompletionRoot
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession

internal class CountCommand(private val repl: ScriptSession) : ScriptCommand("count") {
  override val usage = "count <Metric>"
  override val help =
      """
        Evaluates the metric and tells you the count. Usually just a type, but can include `MAX`,
        `+`, etc.
      """
  override val isReadOnly = true

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.petsWords(PetsCompletionRoot.METRIC)

  override fun withArgs(args: String): List<String> {
    val metric: Metric = repl.agent.parse(args)
    val count = repl.agent.count(args)
    return listOf("$count $metric")
  }
}
