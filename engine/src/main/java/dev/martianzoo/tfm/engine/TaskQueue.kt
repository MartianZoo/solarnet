package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.util.toStrings
import java.util.SortedMap
import java.util.TreeMap

class TaskQueue(val eventLog: EventLog) {
  val taskMap: SortedMap<TaskId, Task> = TreeMap() // TODO oops, dejavafy

  override fun toString() = taskMap.values.joinToString("\n")

  operator fun contains(id: TaskId) = id in taskMap
  operator fun get(id: TaskId) = taskMap[id] ?: error("no task with id: $id")

  val size by taskMap::size
  fun isEmpty() = taskMap.isEmpty()

  fun addTasks(effect: FiredEffect) = addTasks(effect.instruction, effect.actor, effect.cause)
  fun addTasks(effects: Iterable<FiredEffect>) = effects.forEach(::addTasks)

  fun addTasks(
      instruction: Instruction,
      taskOwner: Actor,
      cause: Cause?,
      whyPending: String? = null
  ) {
    addTasks(listOf(instruction), taskOwner, cause, whyPending)
  }

  fun addTasks(
      instructions: Iterable<Instruction>,
      taskOwner: Actor,
      cause: Cause?,
      whyPending: String? = null,
  ) {
    var nextId = nextAvailableId()
    split(instructions).forEach {
      val task = Task(nextId, it, taskOwner, cause, whyPending)
      taskMap[nextId] = task
      nextId = nextId.next()
      eventLog.taskAdded(task)
    }
  }

  fun removeTask(id: TaskId) {
    val removed = taskMap.remove(id) ?: error("no task with id: $id")
    eventLog.taskRemoved(removed)
  }

  fun replaceTask(newTask: Task) {
    val id = newTask.id
    val oldTask = taskMap[id] ?: error("no task with id: $id")
    taskMap[id] = newTask
    eventLog.taskReplaced(oldTask, newTask)
  }

  private fun nextAvailableId() = if (taskMap.none()) TaskId("A") else taskMap.lastKey().next()

  fun toStrings(): List<String> = taskMap.values.toStrings()
}
