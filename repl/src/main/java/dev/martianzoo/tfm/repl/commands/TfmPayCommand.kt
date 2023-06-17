package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.repl.commands.TaskCommand

internal class TfmPayCommand(val repl: ReplSession) : ReplCommand("tfm_pay") {
  override val usage: String = "tfm_pay <amount resource>"
  override val help: String = ""
  override fun withArgs(args: String): List<String> {
    val gains: List<Instruction> = Instruction.split(Parsing.parse(args)).instructions

    val ins =
        Multi.create(
            gains.map {
              val sex = (it as Gain).scaledEx
              val currency = sex.expression
              val pay = ClassName.cn("Pay").of(CLASS.of(currency))
              Transmute(SimpleFrom(pay, currency), sex.scalar)
            })
    return TaskCommand(repl).withArgs(ins.toString())
  }
}
