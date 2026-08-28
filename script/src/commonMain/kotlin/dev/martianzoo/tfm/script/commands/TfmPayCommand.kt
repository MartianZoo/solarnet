package dev.martianzoo.tfm.script.commands

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession

internal class TfmPayCommand(private val repl: ScriptSession) : ScriptCommand("tfm_pay") {
  override val usage: String = "tfm_pay <amount resource>"
  override val help: String = ""

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.paymentWords()

  override fun withArgs(args: String): List<String> {
    val gains: List<Instruction> =
        repl.game.vocabulary
            .canonicalize(Parsing.parse<InstructionTree>(args))
            .let(InstructionGroup::of)
            .instructions

    val payments: List<Pair<String, String>> = gains.map {
      val sex = (it as Gain).scaledEx
      val currency = sex.expression
      val pay = cn("Pay").of(CLASS.of(currency))
      currency.toString() to Transmute(Full(pay, currency), sex.scalar).toString()
    }
    val previousAutoExecMode = repl.gameplay.autoExecMode
    val result =
        repl.game.timeline.atomic {
          repl.gameplay.autoExecMode = NONE
          try {
            val selected = repl.game.tasks.selectedTask()
            val ordered = payments.sortedByDescending { (currency) ->
              paymentTask(currency) == selected
            }
            ordered.forEach { (_, instruction) -> repl.gameplay.doTask(instruction) }
            dismissUnusedAcceptsWhilePaused()
          } finally {
            repl.gameplay.autoExecMode = previousAutoExecMode
          }
        }
    return repl.describeExecutionResults(result)
  }

  private fun dismissUnusedAcceptsWhilePaused() {
    repl.game.tasks
        .matching { it.cause?.context?.className == cn("Accept") }
        .forEach {
          repl.gameplay.selectTask(it)
          if (it in repl.game.tasks) repl.gameplay.narrowTask("Ok")
        }
  }

  private fun paymentTask(currency: String) =
      repl.game.tasks
          .matching {
            it.cause?.context?.className == cn("Accept") &&
                it.instruction.toString().contains("Class<$currency>")
          }
          .single()
}
