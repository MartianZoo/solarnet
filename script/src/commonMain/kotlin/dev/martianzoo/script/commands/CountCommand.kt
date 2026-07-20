package dev.martianzoo.script.commands

import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.script.PetsCompletionRoot
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession

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
    val metric: Metric = repl.gameplay.parse(args)
    val count = repl.gameplay.count(args)
    return listOf("$count $metric")
  }
}
