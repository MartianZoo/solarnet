package dev.martianzoo.tfm.script.commands

import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.engine.AutoExecMode.NONE
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
    val pauseForWrittenCost = payment.isNotEmpty() && action.cost != null
    val previousAutoExecMode = repl.gameplay.autoExecMode
    var writtenCostPaused = false
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
            if (pauseForWrittenCost) {
              repl.gameplay.autoExecMode = NONE
              writtenCostPaused = true
            }
            val taskIdsBeforeAction = repl.game.tasks.ids()
            TaskCommand(repl).withArgs("UseAction<$cardName, $whichAction>")
            if (payment.isNotEmpty()) {
              if (pauseForWrittenCost) payWrittenActionCost(payment, taskIdsBeforeAction)
              else TfmPayCommand(repl).withArgs(payment)
            }
            if (pauseForWrittenCost) {
              repl.gameplay.autoExecMode = previousAutoExecMode
              writtenCostPaused = false
            }
          }
        } finally {
          if (writtenCostPaused) repl.gameplay.autoExecMode = previousAutoExecMode
        }
    return repl.describeExecutionResults(result)
  }

  private fun payWrittenActionCost(payment: String, taskIdsBeforeAction: Set<TaskId>) {
    val costTasks = repl.game.tasks.extract { it }.filter { it.id !in taskIdsBeforeAction }
    val directCosts = costTasks.filter { it.instruction.descendantsOfType<Remove>().any() }
    if (directCosts.isEmpty()) {
      openInvoice(costTasks, payment)
      TfmPayCommand(repl).withArgs(payment)
      return
    }

    val removals = paymentRemovals(payment)
    check(removals.size == directCosts.size) {
      "Action requires ${directCosts.size} direct payment(s), but ${removals.size} were supplied"
    }
    directCosts.zip(removals).forEach { (task, removal) ->
      val revision = specializeVariableCost(task.instruction, removal)
      repl.gameplay.reviseTask(task.id, revision.toString())
    }
  }

  private fun openInvoice(costTasks: List<Task>, payment: String) {
    val invoice = costTasks.single { task ->
      task.instruction.descendantsOfType<Change>().any { change ->
        change.gaining?.className == cn("Owed")
      }
    }
    val owed =
        invoice.instruction.descendantsOfType<Change>().single { change ->
          change.gaining?.className == cn("Owed")
        }
    val revision =
        if (owed.count.abstract) {
          val supplied =
              paymentGains(payment).single { gain ->
                gain.scaledEx.expression.className in owed.gaining!!.descendantsOfType<ClassName>()
              }
          val suppliedAmount = supplied.scaledEx.scalar.toString().toInt()
          val authored = owed.count.toString()
          val authoredMultiple = authored.removeSuffix("X").ifEmpty { "1" }.toInt()
          check(suppliedAmount % authoredMultiple == 0) {
            "$suppliedAmount isn't a multiple of $authoredMultiple"
          }
          bindXTo(suppliedAmount / authoredMultiple).transformInstruction(invoice.instruction)
        } else {
          invoice.instruction
        }
    val taskIdsBeforeInvoice = repl.game.tasks.ids()
    TaskCommand(repl).withArgs(revision.toString())
    val paymentTask =
        repl.game.tasks
            .extract { it }
            .singleOrNull { task ->
              task.id !in taskIdsBeforeInvoice &&
                  task.instruction.descendantsOfType<Change>().any { change ->
                    change.gaining?.className == cn("Payment")
                  }
            }
    paymentTask?.let { TaskCommand(repl).withArgs(it.instruction.toString()) }
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
    return bindXTo(x).transformInstruction(task)
  }

  private fun paymentRemovals(payment: String): List<Instruction> {
    return paymentGains(payment).map { gain -> remove(gain.scaledEx) }
  }

  private fun paymentGains(payment: String): List<Gain> =
      repl.game.vocabulary
          .canonicalize(Parsing.parse<InstructionTree>(payment))
          .let(InstructionGroup::of)
          .instructions
          .map {
            it as? Gain ?: throw UsageException("payment must contain positive resources")
          }
}
