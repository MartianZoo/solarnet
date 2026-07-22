package dev.martianzoo.tfm.script.commands

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.commands.TaskCommand

internal class TfmPayCommand(private val repl: ScriptSession) : ScriptCommand("tfm_pay") {
  override val usage: String = "tfm_pay <amount resource>"
  override val help: String = ""

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.paymentWords()

  override fun withArgs(args: String): List<String> {
    val gains: List<Instruction> = Instruction.split(Parsing.parse(args)).instructions

    val ins: List<String> = gains.map {
      val sex = (it as Gain).scaledEx
      val currency = sex.expression
      val pay = ClassName.cn("Pay").of(CLASS.of(currency))
      Transmute(FromExpression(pay, currency), sex.scalar).toString()
    }
    val cmd = TaskCommand(repl)
    return ins.flatMap { cmd.withArgs(it) }
  }
}
