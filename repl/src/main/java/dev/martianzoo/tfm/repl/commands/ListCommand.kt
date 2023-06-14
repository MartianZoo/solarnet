package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.api.SystemClasses
import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.repl.ReplCommand
import dev.martianzoo.tfm.repl.ReplSession
import dev.martianzoo.util.Multiset

internal class ListCommand(val repl: ReplSession) : ReplCommand("list") {
  override val usage = "list <Expression>"
  override val help = """
        This command is super broken right now.
      """
  override val isReadOnly = true
  override fun noArgs() = withArgs(SystemClasses.COMPONENT.toString())

  override fun withArgs(args: String): List<String> {
    val expr: Expression = repl.tfm.parse(args)
    val counts: Multiset<Expression> = repl.tfm.list(args)
    return listOf("${counts.size} $expr") +
        counts.entries.sortedByDescending { (_, ct) -> ct }.map { (e, ct) -> "  $ct $e" }
  }
}
