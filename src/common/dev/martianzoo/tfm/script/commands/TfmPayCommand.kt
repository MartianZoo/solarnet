package dev.martianzoo.tfm.script.commands

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
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
    val result =
        repl.game.timeline.atomic {
          repl.agents
              .tfm(repl.agent.actor)
              .pay(
                  mc = payments["MC"] ?: 0,
                  steel = payments["Steel"] ?: 0,
                  titanium = payments["Titanium"] ?: 0,
                  plants = payments["Plant"] ?: 0,
                  energy = payments["Energy"] ?: 0,
                  heat = payments["Heat"] ?: 0,
              )
          repl.agent.autoExecNow()
        }
    return repl.describeExecutionResults(result)
  }
}
