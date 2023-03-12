package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.LogEntry.ChangeEvent.Cause
import dev.martianzoo.tfm.data.LogEntry.TaskAddedEvent
import dev.martianzoo.tfm.data.LogEntry.TaskRemovedEvent
import dev.martianzoo.tfm.data.LogEntry.TaskReplacedEvent
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.util.toStrings
import java.util.SortedMap
import java.util.TreeMap

class TaskQueue(val game: Game) {
  val taskMap: SortedMap<TaskId, Task> = TreeMap()

  override fun toString() = taskMap.values.joinToString("\n")

  operator fun get(id: TaskId) = taskMap[id] ?: error("no task with id: $id; queue has: $taskMap")

  val size by taskMap::size
  fun isEmpty() = taskMap.isEmpty()

  fun addTasks(instruction: Instruction, taskOwner: Actor, cause: Cause?) =
      addTasks(listOf(instruction), taskOwner, cause)

  fun addTasks(
      instructions: Iterable<Instruction>,
      taskOwner: Actor,
      cause: Cause?,
  ): List<TaskAddedEvent> { // TODO just ids
    var nextId = nextAvailableId()
    return split(instructions).map {
      val task = Task(nextId, it, taskOwner, cause)
      taskMap[nextId] = task
      val event = TaskAddedEvent(task)
      game.gameLog.addEntry(event)
      nextId = nextId.next()
      event
    }
  }

  fun removeTask(id: TaskId): TaskRemovedEvent {
    val removed = taskMap.remove(id) ?: error("no task with id $id")
    val event = TaskRemovedEvent(removed)
    game.gameLog.addEntry(event)
    return event
  }

  fun replaceTask(newTask: Task): TaskReplacedEvent {
    val id = newTask.id
    val oldTask = taskMap[id] ?: error("")
    taskMap[id] = newTask
    return TaskReplacedEvent(id, oldTask, newTask)
  }

  private fun nextAvailableId() = if (taskMap.none()) TaskId("A") else taskMap.lastKey().next()

  fun toStrings(): List<String> = taskMap.values.toStrings()
}
