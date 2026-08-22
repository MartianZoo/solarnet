package dev.martianzoo.tfm.script.commands

import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException
import dev.martianzoo.script.commands.TaskCommand
import dev.martianzoo.tfm.api.tfmAuthority

internal class TfmActionCommand(private val repl: ScriptSession) : ScriptCommand("tfm_action") {
  override val usage: String = "tfm_action <CardName> <1|2|3>[, <payment>...]"
  override val help: String =
      """
        Uses one of a Terraforming Mars card's actions. The card must already be owned, and the
        action number must be 1, 2, or 3. The Use Card Action standard action is selected first
        when needed. Payment text after the first comma either drives the action's `tfm_pay`
        workflow or selects its direct resource-removal cost.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      if (',' in context.args) context.paymentWords()
      else if (context.argIndex == 0) context.actionCardNames()
      else context.completions("1", "2", "3", group = "card actions")

  override fun withArgs(args: String): List<String> {
    val actionArgs = args.substringBefore(',').trim()
    val payment = args.substringAfter(',', missingDelimiterValue = "").trim()
    val match = Regex("""^(.+?)\s+([123])$""").matchEntire(actionArgs) ?: throw UsageException()
    val cardName = repl.game.vocabulary.canonicalName(cn(match.groupValues[1]))
    val actionNumber = match.groupValues[2]
    val whichAction = listOf("First", "Second", "Third")[actionNumber.toInt() - 1]
    val action =
        repl.game.reader.tfmAuthority.card(cardName).actions.getOrNull(actionNumber.toInt() - 1)
            ?: throw UsageException("$cardName has no action $actionNumber")
    val pauseForDirectCost = payment.isNotEmpty() && action.cost != null
    val previousAutoExecMode = repl.gameplay.autoExecMode
    var directCostPaused = false
    val result =
        try {
          repl.game.timeline.atomic {
            val choosingStandardAction =
                repl.game.tasks
                    .matching { it.instruction.toString().contains("StandardAction") }
                    .any()
            if (choosingStandardAction) {
              TaskCommand(repl).withArgs("UseAction<UseCardActionSA, First>")
            }
            TaskCommand(repl).withArgs("ActionUsedMarker<$cardName>")
            if (pauseForDirectCost) {
              repl.gameplay.autoExecMode = NONE
              directCostPaused = true
            }
            val taskIdsBeforeAction = repl.game.tasks.ids()
            TaskCommand(repl).withArgs("UseAction<$cardName, $whichAction>")
            if (payment.isNotEmpty()) {
              if (pauseForDirectCost) payDirectActionCost(payment, taskIdsBeforeAction)
              else TfmPayCommand(repl).withArgs(payment)
            }
            if (pauseForDirectCost) {
              repl.gameplay.autoExecMode = previousAutoExecMode
              directCostPaused = false
            }
          }
        } finally {
          if (directCostPaused) repl.gameplay.autoExecMode = previousAutoExecMode
        }
    return repl.describeExecutionResults(result)
  }

  private fun payDirectActionCost(payment: String, taskIdsBeforeAction: Set<TaskId>) {
    val directCosts =
        repl.game.tasks
            .extract { it }
            .filter { it.id !in taskIdsBeforeAction }
            .filter { it.instruction.descendantsOfType<Remove>().any() }
    check(directCosts.isNotEmpty()) { "Action produced no direct cost to pay" }

    val removals = paymentRemovals(payment)
    check(removals.size == directCosts.size) {
      "Action requires ${directCosts.size} direct payment(s), but ${removals.size} were supplied"
    }
    directCosts.zip(removals).forEach { (task, removal) ->
      val revision = specializeVariableCost(task.instruction, removal)
      repl.gameplay.reviseTask(task.id, revision.toString())
    }
  }

  private fun specializeVariableCost(task: Instruction, removal: Instruction): Instruction {
    val directRemoval = removal as Remove
    val matchingVariableCosts =
        task.descendantsOfType<Remove>().filter {
          it.scaledEx.scalar.abstract &&
              directRemoval.scaledEx.expression.className == it.scaledEx.expression.className
        }
    val variableCost = matchingVariableCosts.singleOrNull() ?: return removal
    val supplied = directRemoval.scaledEx.scalar.toString().toInt()
    val authored = variableCost.scaledEx.scalar.toString()
    val authoredMultiple = authored.removeSuffix("X").ifEmpty { "1" }.toInt()
    check(supplied % authoredMultiple == 0) {
      "$supplied isn't a multiple of $authoredMultiple"
    }
    val x = supplied / authoredMultiple
    val specialized =
        X_SCALAR.replace(task.toString()) { match ->
          val multiple = match.groupValues[1].ifEmpty { "1" }.toInt()
          (multiple * x).toString()
        }
    return repl.game.vocabulary.canonicalize(Parsing.parse<Instruction>(specialized))
  }

  private fun paymentRemovals(payment: String): List<Instruction> {
    val gains =
        repl.game.vocabulary
            .canonicalize(Parsing.parse<InstructionTree>(payment))
            .let(InstructionGroup::of)
            .instructions
    return gains.map {
      val gain = it as? Gain ?: throw UsageException("payment must contain positive resources")
      remove(gain.scaledEx)
    }
  }

  private companion object {
    val X_SCALAR = Regex("""\b(\d*)X\b""")
  }
}
