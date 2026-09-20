package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.ActorEngine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue

/** Shared Agent-policy queue drain above the policy-free engine. */
internal class AutoExecLoop(private val world: World) {
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
    val candidates = selected?.let(::listOf) ?: allTasks.ids().filter(::canSelectTask)
    val candidateCounts = candidates.groupingBy { allTasks.getTaskData(it).assignee }.eachCount()
    val options = candidates.filter { taskId ->
      val actor = allTasks.getTaskData(taskId).assignee
      when (policy(taskId)) {
        NONE -> false
        CONCRETE -> candidateCounts.getValue(actor) == 1
        else -> true
      }
    }
    val activeCandidates = candidates.filter { taskId -> policy(taskId) != NONE }

    when (options.size) {
      0 -> {
        val taskId =
            allTasks.ids().firstOrNull { taskId ->
              policy(taskId) != NONE
            } ?: return false
        if (selected != null || activeCandidates.isNotEmpty()) return false
        engineFor(taskId).doTask(taskId)
        error("that should've completed")
      }
      1 -> {
        val taskId = options.single()
        val engine = engineFor(taskId)
        engine.selectTask(taskId)
        if (taskId !in allTasks) return true
        try {
          if (engine.trySelectedTask()) return true
        } catch (e: DeadEndException) {
          throw e.cause ?: e
        }
      }
    }

    var recoverable = false
    for (taskId in options) {
      try {
        world.timeline.atomic { engineFor(taskId).doTask(taskId) }
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

  private fun policy(taskId: TaskId): AutoExecPolicy {
    val actor = allTasks.getTaskData(taskId).assignee
    return policies.getValue(actor).invoke()
  }

  private fun engineFor(taskId: TaskId): ActorEngine =
      world.actorEngine(allTasks.getTaskData(taskId).assignee)
}
