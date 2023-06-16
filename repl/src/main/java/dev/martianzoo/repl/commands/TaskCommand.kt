package dev.martianzoo.repl.commands

import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.data.TaskResult
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.repl.ReplSession.UsageException

internal class TaskCommand(val repl: ReplSession) : ReplCommand("task") {
  override val usage = "task <id> [<Instruction> | drop]"
  override val help =
      """
        To carry out a task exactly as it is, just type `task A` where `A` is the id of that task
        in your `tasks` list. But usually a task gets put on that list because its instruction
        was not fully specified. So, after `task A` you can write a revised version of that
        instruction, as long as your revision is a more specific form of the instruction. For
        example, if the queued task is `-3 StandardResource<Anyone>?` you can revise it to
        `-2 Plant<Player1>`. If you leave out the id (like `A`) it will expect your revision to
        match only one existing task.
      """

  override fun withArgs(args: String): List<String> {
    val split = Regex("\\s+").split(args, 2)
    val first = split.firstOrNull() ?: throw UsageException()
    if (!first.matches(Regex("[A-Z]{1,2}"))) {
      return repl.describeExecutionResults(repl.tfm.tryTask(args))
    }

    val id = TaskId(first.uppercase())
    if (id !in repl.game.tasks) throw UsageException("valid ids are ${repl.game.tasks.ids()}")
    val rest: String? =
        if (split.size > 1 && split[1].isNotEmpty()) {
          split[1]
        } else {
          null
        }

    val result: TaskResult =
        when (rest) {
          "drop" -> {
            repl.access().dropTask(id)
            return listOf("Task $id deleted")
          }
          "prepare" -> {
            repl.tfm.prepareTask(id)
            return repl.game.tasks.extract { "$it" }
          }
          null -> repl.tfm.tryTask(id)
          else ->
            repl.game.timeline.atomic {
              repl.tfm.reviseTask(id, rest)
                if (id in repl.game.tasks) repl.tfm.tryTask(id)
              }
        }
    return repl.describeExecutionResults(result)
  }
}
