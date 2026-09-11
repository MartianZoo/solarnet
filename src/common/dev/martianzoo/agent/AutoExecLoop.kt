package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.ActorEngine
import dev.martianzoo.engine.TaskQueue
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.Task.TaskId

/** Shared legacy queue drain above the policy-free engine. */
internal class AutoExecLoop(private val world: World) {
  private val allTasks: TaskQueue
    get() = world.tasks

  internal fun run(actor: Actor, policy: AutoExecPolicy) {
    while (actOnce(actor, policy)) {}
  }

  private fun actOnce(actor: Actor, policy: AutoExecPolicy): Boolean {
    if (allTasks.isEmpty()) return false

    // Preserve the transitional rule: Player NONE still drains Admin work, while Admin NONE stops.
    val eligible =
        if (policy == NONE) {
          if (actor !is Player) return false
          allTasks.ids().filter { taskId -> allTasks.getTaskData(taskId).assignee == ADMIN }
        } else {
          allTasks.ids()
        }
    if (eligible.isEmpty()) return false

    val selected = allTasks.selectedTask()
    if (selected != null && selected !in eligible) return false
    val effectivePolicy = if (policy == NONE) EAGER else policy
    val options = selected?.let(::listOf) ?: eligible.filter(::canSelectTask)

    when (options.size) {
      0 -> {
        val taskId = eligible.first()
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
      else -> if (effectivePolicy == CONCRETE) return false
    }

    var recoverable = false
    for (taskId in options) {
      try {
        world.timeline.atomic { engineFor(taskId).doTask(taskId) }
        return true
      } catch (_: AbstractException) {
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

  private fun engineFor(taskId: TaskId): ActorEngine =
      world.actorEngine(allTasks.getTaskData(taskId).assignee)
}
