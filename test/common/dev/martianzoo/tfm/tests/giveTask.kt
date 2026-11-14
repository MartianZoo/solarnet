package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agent
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Deferral.NOW
import dev.martianzoo.pets.ast.Instruction.Dispatch
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.state.TaskResult

/** Releases the unique pending NOW task to [recipient], who immediately submits [choice]. */
internal fun Agent.OperationScope.giveTask(recipient: Agent, choice: String): TaskResult {
  doTask(uniqueTaskToGive(tasks, recipient))
  return recipient.doTask(choice)
}

/** Identifies the NOW task explicitly when several can be given to [recipient]. */
internal fun Agent.OperationScope.giveTask(
    recipient: Agent,
    task: String,
    choice: String,
): TaskResult {
  doTask("NOW ${recipient.actor} { $task }")
  return recipient.doTask(choice)
}

/** Releases the unique pending NOW task to [recipient], who immediately submits [choice]. */
internal fun Agent.giveTask(recipient: Agent, choice: String): TaskResult {
  doTask(uniqueTaskToGive(tasks, recipient))
  return recipient.doTask(choice)
}

/** Identifies the NOW task explicitly when several can be given to [recipient]. */
internal fun Agent.giveTask(recipient: Agent, task: String, choice: String): TaskResult {
  doTask("NOW ${recipient.actor} { $task }")
  return recipient.doTask(choice)
}

private fun uniqueTaskToGive(tasks: TaskQueue, recipient: Agent): TaskId =
    tasks
        .matching {
          val instruction = it.instruction
          !it.deferred &&
              instruction is Dispatch &&
              instruction.deferral == NOW &&
              instruction.actor == recipient.actor.expression
        }
        .singleOrNull()
        ?: throw TaskException(
            "Expected exactly one pending NOW task for ${recipient.actor}; available tasks:\n$tasks"
        )
