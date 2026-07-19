package dev.martianzoo.tfm.script.commands

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.commands.TaskCommand
import dev.martianzoo.tfm.api.TfmRuleset

internal class TfmPlayCommand(private val repl: ScriptSession) : ScriptCommand("tfm_play") {
  override val usage: String = "tfm_play <CardName>"
  override val help: String = ""

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.playableCardNames()

  override fun withArgs(args: String): List<String> {
    val cardName = ClassName.cn(args)
    val kind = (repl.game.reader.ruleset as TfmRuleset).card(cardName).deck!!.className
    return TaskCommand(repl).withArgs("PlayCard<Class<$kind>, Class<$args>>")
  }
}
