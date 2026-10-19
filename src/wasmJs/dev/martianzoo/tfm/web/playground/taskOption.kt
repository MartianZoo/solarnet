package dev.martianzoo.tfm.web.playground

import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.Task

/** Evaluates a live task for the Play chooser, rolling back even a concrete task's execution. */
internal fun taskOption(
    world: World,
    task: Task,
    availableArms: ((Task) -> List<String>)? = null,
): Prompt.Option {
  fun text(task: Task) = "${task.instruction}${task.then?.let { " THEN $it" } ?: ""}"
  var instruction = text(task)
  var arms = emptyList<String>()
  var selectable = false
  try {
    world.timeline.atomic {
      val before = world.timeline.checkpoint()
      // Engine selection runs the game's effects, but does not deal cards, settle Agent policy,
      // or notify transaction-completion callbacks. The UI refreshes only after this probe returns.
      world.actorEngine(task.assignee).selectTask(task.id)
      val events = world.events.entriesSince(before)
      val selected =
          events.filterIsInstance<TaskEditedEvent>().firstOrNull { it.task.id == task.id }
      instruction =
          selected?.task?.let(::text)
              ?: events
                  .takeWhile { it !is TaskRemovedEvent || it.task.id != task.id }
                  .filterIsInstance<TaskAddedEvent>()
                  .joinToString(", ") { text(it.task) }
                  .ifEmpty { "Ok" }
      arms =
          world.tasks
              .selectedTask()
              ?.let(world.tasks::getTaskData)
              ?.let { active ->
                if (orArms(active.instruction).isEmpty()) emptyList()
                else availableArms?.invoke(active) ?: orArms(active.instruction)
              }
              .orEmpty()
      selectable = true
      throw AbortTransactionException()
    }
  } catch (_: GameplayException) {
    // The atomic call has already restored the original task and world.
  }
  return Prompt.Option(
      label = "${task.id.ordinal}  $instruction",
      who = task.assignee.toString(),
      enabled = selectable,
      taskId = task.id,
      arms = arms,
  )
}

private fun orArms(tree: InstructionTree): List<String> =
    when (tree) {
      is Or -> tree.instructions.map(InstructionTree::toString)
      is Then -> orArms(tree.first)
      is By -> orArms(tree.inner)
      is Gated -> orArms(tree.inner)
      is Per -> orArms(tree.inner)
      else -> emptyList()
    }
