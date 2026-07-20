package dev.martianzoo.script.commands

import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.script.PetsCompletionRoot
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession

internal class HasCommand(private val repl: ScriptSession) : ScriptCommand("has") {
  override val usage = "has <Requirement>"
  override val help =
      """
        Evaluates the requirement and tells you true or false. Go see syntax.md on the github page
        for syntax.
      """
  override val isReadOnly = true

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.petsWords(PetsCompletionRoot.REQUIREMENT)

  override fun withArgs(args: String): List<String> {
    val result = repl.gameplay.has(args)
    return listOf("$result: ${repl.gameplay.parse<Requirement>(args)}")
  }
}
