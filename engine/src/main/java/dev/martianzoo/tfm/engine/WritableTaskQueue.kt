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
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.util.toStrings
import java.util.SortedMap
import java.util.TreeMap

public class WritableTaskQueue(val eventLog: WritableEventLog) : TaskQueue {
  internal val taskMap: SortedMap<TaskId, Task> = TreeMap() // TODO dejavafy

  override fun toString() = taskMap.values.joinToString("\n")

  override operator fun contains(id: TaskId) = id in taskMap

  override operator fun get(id: TaskId): Task {
    require(id in this) { id }
    return taskMap[id]!!
  }

  override val size by taskMap::size
  override val ids by taskMap::keys
  override fun isEmpty() = taskMap.isEmpty()

  internal fun addTasks(effect: FiredEffect) =
      addTasks(effect.instruction, effect.player, effect.cause)
  internal fun addTasks(effects: Iterable<FiredEffect>) = effects.forEach(::addTasks)

  internal fun addTasks(
      instruction: Instruction,
      taskOwner: Player,
      cause: Cause?,
      whyPending: String? = null
  ) {
    addTasks(listOf(instruction), taskOwner, cause, whyPending)
  }

  internal fun addTasks(
      instructions: Iterable<Instruction>,
      taskOwner: Player,
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

  internal fun removeTask(id: TaskId) {
    require(id in this) { id }
    val removed = taskMap.remove(id)!!
    eventLog.taskRemoved(removed)
  }

  internal fun replaceTask(newTask: Task) {
    val id = newTask.id
    val oldTask = this[id]
    taskMap[id] = newTask
    eventLog.taskReplaced(oldTask, newTask)
  }

  internal fun undo(entry: TaskEvent) {
    when (entry) {
      is TaskAddedEvent -> taskMap.remove(entry.task.id)
      is TaskRemovedEvent -> taskMap[entry.task.id] = entry.task
      is TaskReplacedEvent -> require(taskMap.put(entry.task.id, entry.oldTask) == entry.task)
    }
  }

  override fun nextAvailableId() = if (taskMap.none()) TaskId("A") else taskMap.lastKey().next()

  override fun toStrings(): List<String> = taskMap.values.toStrings()
}
