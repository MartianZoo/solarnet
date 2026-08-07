package dev.martianzoo.tfm.script.commands

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Transmute
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
        Instruction.split(repl.game.vocabulary.canonicalize(Parsing.parse(args))).instructions

    val payments: List<Pair<String, String>> = gains.map {
      val sex = (it as Gain).scaledEx
      val currency = sex.expression
      val pay = cn("Pay").of(CLASS.of(currency))
      currency.toString() to Transmute(FromExpression(pay, currency), sex.scalar).toString()
    }
    val previousAutoExecMode = repl.gameplay.autoExecMode
    val result =
        repl.game.timeline.atomic {
          repl.gameplay.autoExecMode = NONE
          try {
            val prepared = repl.game.tasks.preparedTask()
            val ordered = payments.sortedByDescending { (currency) ->
              paymentTask(currency) == prepared
            }
            ordered.forEach { (currency, instruction) ->
              val id = paymentTask(currency)
              repl.gameplay.reviseTask(id, instruction)
              repl.gameplay.tryTask(id)
            }
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
          repl.gameplay.reviseTask(it, "Ok")
          if (it in repl.game.tasks) repl.gameplay.tryTask(it)
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
