package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.ActorEngine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue

/** Shared Agent-policy queue drain above the policy-free engine. */
internal class AutoExecLoop(private val world: World, private val taskLog: TaskLog) {
  private val allTasks: TaskQueue
    get() = world.tasks

  private val policies = mutableMapOf<Actor, () -> AutoExecPolicy>()

  internal fun register(actor: Actor, policy: () -> AutoExecPolicy) {
    check(policies.put(actor, policy) == null) { "Agent already registered for $actor" }
  }

  internal fun run() {
    while (actOnce()) {}
  }

  private fun actOnce(): Boolean {
    if (allTasks.isEmpty()) return false
    val selected = allTasks.selectedTask()
    val candidates =
        selected?.let(::listOf)
            ?: allTasks.ids().let { pending ->
              // A sole task has no competing choice. Selection still validates its execution,
              // and the enclosing Agent transaction rolls back any failure.
              if (pending.size == 1) pending.toList() else pending.filter(::canSelectTask)
            }
    val candidateCounts = candidates.groupingBy { allTasks.getTaskData(it).assignee }.eachCount()
    val policyOptions = candidates.filter { taskId ->
      val actor = allTasks.getTaskData(taskId).assignee
      when (policy(taskId)) {
        NONE -> false
        CONCRETE -> candidateCounts.getValue(actor) == 1
        else -> true
      }
    }
    val (adminOptions, otherOptions) =
        policyOptions.partition { taskId -> allTasks.getTaskData(taskId).assignee == ADMIN }
    val options = adminOptions + otherOptions

    when (options.size) {
      0 -> {
        val taskId =
            allTasks.ids().firstOrNull { taskId ->
              policy(taskId) != NONE
            } ?: return false
        if (selected != null || candidates.isNotEmpty()) return false
        command(taskId) { engineFor(taskId).doTask(taskId) }
        error("that should've completed")
      }
      // A manual Actor's available task still competes with this option. Do not acquire an
      // abstract select-lock merely because the other Actor has autoexecution disabled.
      1 ->
          if (candidates.size == 1) {
            val taskId = options.single()
            val engine = engineFor(taskId)
            command(taskId) { engine.selectTask(taskId) }
            if (taskId !in allTasks) return true
            if (allTasks.getTaskData(taskId).assignee != engine.actor) return true
            try {
              if (command(taskId) { engine.trySelectedTask() }) return true
            } catch (e: DeadEndException) {
              throw e.cause ?: e
            }
          }
    }

    var recoverable = false
    for (taskId in options) {
      try {
        world.timeline.atomic { command(taskId) { engineFor(taskId).doTask(taskId) } }
        return true
      } catch (_: NotFullySpecifiedException) {
        recoverable = true
      } catch (_: NotNowException) {
        if (allTasks.getTaskData(taskId).instruction.isAbstract(world.reader)) {
          recoverable = true
        }
      }
    }
    if (!recoverable) throw DeadEndException("")
    return false
  }

  private fun canSelectTask(taskId: TaskId): Boolean = engineFor(taskId).canSelectTask(taskId)

  private fun <T> command(taskId: TaskId, block: () -> T): T {
    val task = allTasks.getTaskData(taskId)
    if (task.assignee == ADMIN) return block()
    val instruction = "CHOOSE ${task.instruction}"
    return taskLog.capture(task.assignee, instruction, block = block)
  }

  private fun policy(taskId: TaskId): AutoExecPolicy {
    val actor = allTasks.getTaskData(taskId).assignee
    return policies.getValue(actor).invoke()
  }

  private fun engineFor(taskId: TaskId): ActorEngine =
      world.actorEngine(allTasks.getTaskData(taskId).assignee)
}
