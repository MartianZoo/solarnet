package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agent
import dev.martianzoo.state.TaskResult

/** Releases a selected NOW task to [recipient], who immediately submits [choice]. */
internal fun Agent.OperationScope.giveTask(
    recipient: Agent,
    task: String,
    choice: String = task,
): TaskResult {
  doTask("NOW ${recipient.actor} { $task }")
  return recipient.doTask(choice)
}

/** Releases a selected NOW task from this Agent, then submits [choice] for [recipient]. */
internal fun Agent.giveTask(recipient: Agent, task: String, choice: String = task): TaskResult {
  doTask("NOW ${recipient.actor} { $task }")
  return recipient.doTask(choice)
}
