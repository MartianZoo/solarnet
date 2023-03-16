package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskReplacedEvent
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

  operator fun contains(id: TaskId) = id in taskMap
  operator fun get(id: TaskId) = taskMap[id] ?: error("no task with id: $id")

  val size by taskMap::size
  fun isEmpty() = taskMap.isEmpty()

  fun addTasks(
      instruction: Instruction,
      taskOwner: Actor,
      cause: Cause?,
      whyPending: String? = null
  ) = addTasks(listOf(instruction), taskOwner, cause, whyPending)

  fun addTasks(
      instructions: Iterable<Instruction>,
      taskOwner: Actor,
      cause: Cause?,
      whyPending: String? = null,
  ): List<TaskAddedEvent> { // TODO just id
    var nextId = nextAvailableId()
    return split(instructions).map {
      val task = Task(nextId, it, taskOwner, cause, whyPending)
      taskMap[nextId] = task
      val event = game.eventLog.taskAdded(task)
      nextId = nextId.next()
      event
    }
  }

  fun removeTask(id: TaskId): TaskRemovedEvent { // TODO just id
    val removed = taskMap.remove(id) ?: error("no task with id: $id")
    return game.eventLog.taskRemoved(removed)
  }

  fun replaceTask(newTask: Task): TaskReplacedEvent { // TODO just id
    val id = newTask.id
    val oldTask = taskMap[id] ?: error("no task with id: $id")
    taskMap[id] = newTask
    return game.eventLog.taskReplaced(oldTask, newTask)
  }

  private fun nextAvailableId() = if (taskMap.none()) TaskId("A") else taskMap.lastKey().next()

  fun toStrings(): List<String> = taskMap.values.toStrings()
}