package dev.martianzoo.repl.commands

import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplCompletion
import dev.martianzoo.repl.ReplCompletionContext
import dev.martianzoo.repl.ReplSession

internal class CountCommand(private val repl: ReplSession) : ReplCommand("count") {
  override val usage = "count <Metric>"
  override val help =
      """
        Evaluates the metric and tells you the count. Usually just a type, but can include `MAX`,
        `+`, etc.
      """
  override val isReadOnly = true

  override fun completions(context: ReplCompletionContext): List<ReplCompletion> =
      context.petsWords(PetsCompletionRoot.METRIC)

  override fun withArgs(args: String): List<String> {
    val metric: Metric = repl.gameplay.parse(args)
    val count = repl.gameplay.count(args)
    return listOf("$count $metric")
  }
}
