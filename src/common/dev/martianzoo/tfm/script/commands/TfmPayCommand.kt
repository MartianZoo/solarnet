package dev.martianzoo.tfm.script.commands

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession

internal class TfmPayCommand(private val repl: ScriptSession) : ScriptCommand("tfm_pay") {
  override val usage: String = "tfm_pay <amount resource>"
  override val help: String = ""

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.paymentWords()

  override fun withArgs(args: String): List<String> {
    val gains: List<Instruction> =
        Parsing.parse<InstructionTree>(args).let(InstructionGroup::of).instructions

    val payments: List<Pair<ClassName, String>> = gains.map {
      val sex = (it as Gain).scaledEx
      val currency = sex.expression
      val pay = cn("Pay").of(CLASS.of(currency))
      currency.className to Transmute(Full(pay, currency), sex.scalar).toString()
    }
    val previousAutoExecPolicy = repl.agent.autoExecPolicy
    val result =
        repl.game.timeline.atomic {
          repl.agent.autoExecPolicy = NONE
          try {
            val selected = repl.game.tasks.selectedTask()
            val ordered = payments.sortedByDescending { (currency) ->
              paymentTask(currency) == selected
            }
            ordered.forEach { (_, instruction) -> repl.agent.doTask(instruction) }
            dismissUnusedAcceptsWhilePaused()
          } finally {
            repl.agent.autoExecPolicy = previousAutoExecPolicy
          }
        }
    return repl.describeExecutionResults(result)
  }

  private fun dismissUnusedAcceptsWhilePaused() {
    repl.game.tasks
        .matching { it.cause?.context?.className == cn("Accepting") }
        .forEach {
          repl.agent.selectTask(it)
          if (it in repl.game.tasks) repl.agent.narrowTask("Ok")
        }
  }

  private fun paymentTask(currency: ClassName) =
      repl.game.tasks
          .matching {
            it.cause?.context?.className == cn("Accepting") &&
                it.instruction.descendantsOfType<Change>().any { change ->
                  val gaining = change.gaining
                  gaining?.className == cn("Pay") &&
                      gaining.arguments.lastOrNull() == CLASS.of(currency)
                }
          }
          .single()
}
