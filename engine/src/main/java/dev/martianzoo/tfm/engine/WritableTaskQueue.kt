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
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.util.toSetStrict

internal class WritableTaskQueue(private val tasks: MutableSet<Task> = mutableSetOf()) :
    TaskQueue, Set<Task> by tasks {
  override operator fun contains(id: TaskId) = tasks.any { it.id == id }

  override operator fun get(id: TaskId) =
      tasks.firstOrNull { it.id == id } ?: error("nonexistent task: $id")

  override fun ids() = tasks.map { it.id }.toSetStrict()

  internal fun addTask(task: Task) {
    require(tasks.none { it.id == task.id })
    tasks += task
  }

  internal fun addTasksFrom(effect: FiredEffect, eventLog: WritableEventLog) =
      addTasksFrom(effect.instruction, effect.player, effect.cause, eventLog)

  internal fun addTasksFrom(
      instruction: Instruction,
      taskOwner: Player,
      cause: Cause?,
      eventLog: WritableEventLog,
  ) = addTasksFrom(listOf(instruction), taskOwner, cause, eventLog)

  internal fun addTasksFrom(
      instructions: Iterable<Instruction>,
      owner: Player,
      cause: Cause?,
      eventLog: WritableEventLog,
  ): List<TaskAddedEvent> {
    var nextId = nextAvailableId()
    val events = mutableListOf<TaskAddedEvent>()
    split(instructions)
        .filterNot { it == NoOp }
        .forEach { instr ->
          val task =
              if (instr is Then) {
                if (instr.descendantsOfType<XScalar>().size > 1) {
                  Task(id = nextId, owner = owner, instruction = instr, cause = cause)
                } else {
                  Task(
                      id = nextId,
                      owner = owner,
                      instruction = instr.instructions.first(),
                      then = Then.create(instr.instructions.drop(1)),
                      cause = cause,
                  )
                }
              } else {
                Task(id = nextId, owner = owner, instruction = instr, cause = cause)
              }

          addTask(task)
          events += eventLog.taskAdded(task)
          nextId = nextId.next()
        }
    return events
  }

  internal fun removeTask(id: TaskId, eventLog: WritableEventLog): TaskRemovedEvent {
    require(id in this) { id }
    val toRemove = this[id]
    tasks.remove(toRemove)
    return eventLog.taskRemoved(toRemove)
  }

  internal fun replaceTask(newTask: Task, eventLog: WritableEventLog): TaskReplacedEvent {
    val id = newTask.id
    val oldTask = this[id]
    tasks.remove(oldTask)
    addTask(newTask)
    return eventLog.taskReplaced(oldTask, newTask)
  }

  override fun preparedTask(): TaskId? = tasks.firstOrNull { it.next }?.id

  internal fun reverse(entry: TaskEvent) {
    when (entry) {
      is TaskAddedEvent -> tasks -= entry.task
      is TaskRemovedEvent -> addTask(entry.task)
      is TaskReplacedEvent -> {
        tasks.removeIf { it.id == entry.task.id }
        addTask(entry.oldTask)
      }
    }
  }

  override fun nextAvailableId() = if (isEmpty()) TaskId("A") else tasks.maxOf { it.id }.next()
}
