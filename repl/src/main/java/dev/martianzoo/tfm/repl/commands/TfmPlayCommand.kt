package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.repl.commands.TaskCommand

internal class TfmPlayCommand(val repl: ReplSession) : ReplCommand("tfm_play") {
  override val usage: String = "tfm_play <CardName>"
  override val help: String = ""
  override fun withArgs(args: String): List<String> {
    val cardName = ClassName.cn(args)
    val kind = repl.setup.authority.card(cardName).deck!!.className
    return TaskCommand(repl).withArgs("PlayCard<Class<$kind>, Class<$args>>")
  }
}
