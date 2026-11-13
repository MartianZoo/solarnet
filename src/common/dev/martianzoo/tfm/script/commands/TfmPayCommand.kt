package dev.martianzoo.tfm.script.commands

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession
import dev.martianzoo.tfm.script.ScriptSession.UsageException

internal class TfmPayCommand(private val repl: ScriptSession) : ScriptCommand("tfm_pay") {
  override val usage: String = "tfm_pay <amount resource>[, <amount resource>...]"
  override val help: String =
      "Pays the specified standard resources, with alternatives before the exact cash remainder."

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.paymentWords()

  override fun withArgs(args: String): List<String> {
    val payments =
        Parsing.parse<InstructionTree>(args)
            .let(InstructionGroup::of)
            .instructions
            .map {
              val gain =
                  it as? Gain ?: throw UsageException("payment must contain positive resources")
              val resource = gain.scaledEx.expression.toString()
              if (resource !in setOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat")) {
                throw UsageException("payment must name a standard resource")
              }
              val amount =
                  gain.scaledEx.scalar.toString().toIntOrNull()
                      ?: throw UsageException("payment must specify a whole number of resources")
              resource to amount
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }
    val previousPolicy = repl.agent.autoExecPolicy
    val result =
        repl.game.timeline.atomic {
          try {
            repl.agent.autoExecPolicy = NONE
            repl.agent.continueOperation {
              for (resource in listOf("Plant", "Energy", "Heat", "Titanium", "Steel", "MC")) {
                val amount = payments[resource] ?: continue
                if (amount == 0) continue
                if (repl.agent.count("Owed<Class<$resource>>") > 0) {
                  declineUnusedPaymentChoices()
                }
                doTask("$amount Pay<Class<$resource>> FROM $resource")
              }
              if (repl.agent.count("Owed") == 0) declineUnusedPaymentChoices()
              repl.agent.autoExecPolicy = previousPolicy
              autoExecNow()
            }
          } finally {
            repl.agent.autoExecPolicy = previousPolicy
          }
        }
    return repl.describeExecutionResults(result)
  }

  private fun declineUnusedPaymentChoices() {
    val paid = repl.agent.count("Owed") == 0
    repl.agent.tasks
        .extract { it }
        .filter { task ->
          (paid || NoOp.narrows(task.instruction, repl.agent.reader)) &&
              task.instruction.descendantsOfType<Change>().any {
                it.gaining?.className in setOf(cn("Pay"), cn("PayFromCard"))
              }
        }
        .forEach { repl.agent.doTask("Ok", it.id) }
  }
}
