package dev.martianzoo.tfm.script.commands

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.tfm.canon.cardActions
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession
import dev.martianzoo.tfm.script.ScriptSession.UsageException
import dev.martianzoo.tfm.state.tfmCatalog

internal class TfmActionCommand(private val repl: ScriptSession) : ScriptCommand("tfm_action") {
  override val usage: String = "tfm_action <CardName> <1|2|3>[, <payment>...]"
  override val help: String =
      """
        Uses one of a Terraforming Mars card's actions. The card must already be owned, and the
        action number must be 1, 2, or 3. It selects the card directly from the offered
        turn option or granted card-action task. Payment text after the first comma either drives the action's `tfm_pay`
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
    val cardName = cn(match.groupValues[1])
    val actionNumber = match.groupValues[2]
    val whichAction = listOf("Action1", "Action2", "Action3")[actionNumber.toInt() - 1]
    val action =
        cardActions(repl.game.reader.tfmCatalog.card(cardName)).getOrNull(actionNumber.toInt() - 1)
            ?: throw UsageException("$cardName has no action $actionNumber")
    val pauseForWrittenCost = payment.isNotEmpty() && action.cost != null
    val previousAutoExecPolicy = repl.agent.autoExecPolicy
    var writtenCostPaused = false
    val result =
        try {
          repl.game.timeline.atomic {
            val grantedOperation =
                repl.game.tasks
                    .matching {
                      cn("UseAction") in it.instruction.descendantsOfType<ClassName>()
                    }
                    .any()
            if (pauseForWrittenCost) {
              repl.agent.autoExecPolicy = NONE
              writtenCostPaused = true
            }
            val taskIdsBeforeAction = repl.game.tasks.ids()
            val instruction =
                if (grantedOperation) "UseAction<$cardName, $whichAction>"
                else "UseCardAction<$cardName, $whichAction>"
            TaskCommand(repl).withArgs(instruction)
            if (payment.isNotEmpty()) {
              if (pauseForWrittenCost) payWrittenActionCost(payment, taskIdsBeforeAction)
              else TfmPayCommand(repl).withArgs(payment)
            }
            if (pauseForWrittenCost) {
              repl.agent.autoExecPolicy = previousAutoExecPolicy
              writtenCostPaused = false
            }
          }
        } finally {
          if (writtenCostPaused) repl.agent.autoExecPolicy = previousAutoExecPolicy
        }
    return repl.describeExecutionResults(result)
  }

  private fun payWrittenActionCost(payment: String, taskIdsBeforeAction: Set<TaskId>) {
    val costTasks = repl.game.tasks.extract { it }.filter { it.id !in taskIdsBeforeAction }
    val billing = costTasks.singleOrNull { task ->
      task.instruction.descendantsOfType<Change>().any { it.gaining?.className == cn("Owed") }
    }
    if (billing != null) {
      val owed =
          billing.instruction.descendantsOfType<Change>().single {
            it.gaining?.className == cn("Owed")
          }
      val supplied =
          paymentGains(payment).single { gain ->
            gain.scaledEx.expression.className in owed.gaining!!.descendantsOfType<ClassName>()
          }
      repl.agent.selectTask("${supplied.scaledEx.scalar} ${owed.gaining}")
    }
    if (repl.agent.count("Billing") > 0) {
      TfmPayCommand(repl).withArgs(payment)
      return
    }

    val directCosts = costTasks.filter { it.instruction.descendantsOfType<Remove>().any() }
    val removals = paymentRemovals(payment)
    check(removals.size == directCosts.size) {
      "Action requires ${directCosts.size} direct payment(s), but ${removals.size} were supplied"
    }
    directCosts.zip(removals).forEach { (task, removal) ->
      val narrowing = specializeVariableCost(task.instruction, removal)
      repl.agent.selectTask(task.id)
      repl.agent.narrowTask(narrowing.toString())
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
    check(supplied % authoredMultiple == 0) { "$supplied isn't a multiple of $authoredMultiple" }
    val x = supplied / authoredMultiple
    return bindXTo(x).transformInstruction(task)
  }

  private fun paymentRemovals(payment: String): List<Instruction> {
    return paymentGains(payment).map { gain -> remove(gain.scaledEx) }
  }

  private fun paymentGains(payment: String): List<Gain> =
      Parsing.parse<InstructionTree>(payment).let(InstructionGroup::of).instructions.map {
        it as? Gain ?: throw UsageException("payment must contain positive resources")
      }
}
