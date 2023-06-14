package dev.martianzoo.repl.commands

import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession

internal class HasCommand(val repl: ReplSession) : ReplCommand("has") {
  override val usage = "has <Requirement>"
  override val help =
      """
        Evaluates the requirement and tells you true or false. Go see syntax.md on the github page
        for syntax.
      """
  override val isReadOnly = true

  override fun withArgs(args: String): List<String> {
    val result = repl.tfm.has(args)
    return listOf("$result: ${repl.tfm.parse<Requirement>(args)}")
  }
}
