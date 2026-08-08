package dev.martianzoo.script.commands

import dev.martianzoo.data.GameEvent.TaskEvent
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.script.PetsCompletionRoot
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException
import dev.martianzoo.script.splitTrailingQuotedComment

internal class TaskCommand(private val repl: ScriptSession) : ScriptCommand("task") {
  override val usage = "task <id> [<Instruction> | drop] [\"<comment>\"]"
  override val help =
      """
        To carry out a task exactly as it is, just type `task A` where `A` is the id of that task
        in your `tasks` list. But usually a task gets put on that list because its instruction
        was not fully specified. So, after `task A` you can write a revised version of that
        instruction, as long as your revision is a more specific form of the instruction. For
        example, if the queued task is `-3 StandardResource<Anyone>?` you can revise it to
        `-2 Plant<Player1>`. If you leave out the id (like `A`) it will expect your revision to
        match only one existing task. An optional final double-quoted argument adds a comment to
        the task's event-log entry.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      when (context.argIndex) {
        0 -> context.taskIds() + context.petsWords(PetsCompletionRoot.INSTRUCTION)
        1 ->
            context.completions("drop", "prepare", group = "task actions") +
                context.droppingLeadingWords(1).petsWords(PetsCompletionRoot.INSTRUCTION)
        else -> context.droppingLeadingWords(1).petsWords(PetsCompletionRoot.INSTRUCTION)
      }

  override fun withArgs(args: String): List<String> {
    val (taskArgs, comment) = splitTrailingQuotedComment(args)
    val checkpoint = repl.game.timeline.checkpoint()
    val existingTaskIds = repl.game.tasks.ids()
    val output = executeTask(taskArgs)
    if (comment != null) annotateTaskEvent(checkpoint, existingTaskIds, comment)
    return output
  }

  private fun executeTask(args: String): List<String> {
    val split = Regex("\\s+").split(args, 2)
    val first = split.firstOrNull() ?: throw UsageException()
    val id = repl.resolveTaskLabel(first)
    if (id == null) {
      return repl.describeExecutionResults(repl.gameplay.tryTask(args))
    }

    check(id in repl.game.tasks)
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
            return listOf("Task $first deleted")
          }
          "prepare" -> {
            repl.gameplay.prepareTask(id)
            return repl.taskLines()
          }
          null -> repl.gameplay.tryTask(id)
          else ->
              // TODO: Route this through gameplay's outer atomic boundary so revising to Ok can
              // resume an automatic workflow when it removes the final queued task.
              repl.game.timeline.atomic {
                repl.gameplay.reviseTask(id, rest)
                if (id in repl.game.tasks) repl.gameplay.tryTask(id)
              }
        }
    return repl.describeExecutionResults(result)
  }

  private fun annotateTaskEvent(
      checkpoint: Checkpoint,
      existingTaskIds: Set<TaskId>,
      comment: String,
  ) {
    val taskEvents = repl.game.events.entriesSince(checkpoint).filterIsInstance<TaskEvent>()
    val targetId = taskEvents.firstOrNull { it.task.id in existingTaskIds }?.task?.id
    val targetEvent = targetId?.let { id -> taskEvents.lastOrNull { it.task.id == id } }
    checkNotNull(targetEvent) { "task command created no event to comment" }
    repl.game.events.reviseComment(targetEvent.ordinal, comment)
  }
}
