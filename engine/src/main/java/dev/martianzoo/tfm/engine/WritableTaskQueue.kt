package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskReplacedEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Game.TaskQueue
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.util.toStrings

internal class WritableTaskQueue(
    private val taskMap: MutableMap<TaskId, Task> = mutableMapOf()
) : TaskQueue {
  override fun toString() = taskMap.values.joinToString("\n")

  override operator fun contains(id: TaskId) = id in taskMap

  override operator fun get(id: TaskId): Task {
    require(id in this) { id }
    return taskMap[id]!!
  }

  override val size by taskMap::size
  override val ids = taskMap.keys.sorted().toSet()
  override fun isEmpty() = taskMap.isEmpty()

  internal fun addTasksFrom(effect: FiredEffect, eventLog: WritableEventLog): Unit =
      addTasksFrom(effect.instruction, effect.player, effect.cause, eventLog)

  internal fun addTasksFrom(effects: Iterable<FiredEffect>, eventLog: WritableEventLog) =
      effects.forEach { addTasksFrom(it, eventLog) }

  internal fun addTasksFrom(
      instruction: Instruction,
      taskOwner: Player,
      cause: Cause?,
      eventLog: WritableEventLog,
  ) {
    addTasksFrom(listOf(instruction), taskOwner, cause, eventLog)
  }

  internal fun addTasksFrom(
      instructions: Iterable<Instruction>,
      taskOwner: Player,
      cause: Cause?,
      eventLog: WritableEventLog,
  ) {
    var nextId = nextAvailableId()
    split(instructions).forEach {
      val task = Task(nextId, it, taskOwner, cause)
      taskMap[nextId] = task
      nextId = nextId.next()
      eventLog.taskAdded(task)
    }
  }

  internal fun removeTask(id: TaskId, eventLog: WritableEventLog) {
    require(id in this) { id }
    val removed = taskMap.remove(id)!!
    eventLog.taskRemoved(removed)
  }

  internal fun replaceTask(newTask: Task, eventLog: WritableEventLog) {
    val id = newTask.id
    val oldTask = this[id]
    taskMap[id] = newTask
    eventLog.taskReplaced(oldTask, newTask)
  }

  internal fun reverse(entry: TaskEvent) {
    when (entry) {
      is TaskAddedEvent -> taskMap.remove(entry.task.id)
      is TaskRemovedEvent -> taskMap[entry.task.id] = entry.task
      is TaskReplacedEvent -> require(taskMap.put(entry.task.id, entry.oldTask) == entry.task)
    }
  }

  override fun nextAvailableId() = if (taskMap.none()) TaskId("A") else taskMap.keys.max().next()

  override fun toStrings(): List<String> = taskMap.values.toStrings()

  override fun asMap() = taskMap.keys.sorted().associateWith { taskMap[it]!! }

  fun clone() = WritableTaskQueue(taskMap.toMutableMap())
}
