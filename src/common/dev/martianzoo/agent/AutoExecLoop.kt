package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.ActorEngine
import dev.martianzoo.engine.AgentInputRequiredException
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.RecordedInput.Kind
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue

/** Shared Agent-policy queue drain above the policy-free engine. */
internal class AutoExecLoop(private val world: World, private val taskLog: TaskLog) {
  private val allTasks: TaskQueue
    get() = world.tasks

  private val policies = mutableMapOf<Actor, () -> AutoExecPolicy>()

  /**
   * Where the current operation, through any of these Agents, last began or made a decision. Its
   * work is over once the World's `FINALLY` work has begun since then.
   */
  internal var lastDecision: Checkpoint? = null

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
            ?: allTasks.ids().filterNot(::isDeferred).let { pending ->
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
              policy(taskId) != NONE && !isDeferred(taskId)
            } ?: return false
        if (selected != null || candidates.isNotEmpty()) return false
        command(taskId, Kind.DO_TASK_ID) { engineFor(taskId).doTask(taskId) }
        error("that should've completed")
      }
      // A manual Actor's available task still competes with this option. Do not acquire an
      // abstract select-lock merely because the other Actor has autoexecution disabled.
      1 ->
          if (candidates.size == 1) {
            val taskId = options.single()
            val engine = engineFor(taskId)
            try {
              var progressed = false
              world.timeline.atomic {
                command(taskId, Kind.CHOOSE) { engine.selectTask(taskId) }
                progressed =
                    taskId !in allTasks ||
                        allTasks.getTaskData(taskId).assignee != engine.actor ||
                        command(taskId, Kind.TRY_SELECTED) { engine.trySelectedTask() }
                world.agentTransactionGuard?.validate()
              }
              if (progressed) return true
            } catch (_: AgentInputRequiredException) {
              return false
            } catch (e: DeadEndException) {
              throw e.cause ?: e
            }
          }
    }

    var recoverable = false
    for (taskId in options) {
      try {
        world.timeline.atomic {
          command(taskId, Kind.DO_TASK_ID) { engineFor(taskId).doTask(taskId) }
          world.agentTransactionGuard?.validate()
        }
        return true
      } catch (_: AgentInputRequiredException) {
        recoverable = true
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

  private fun isDeferred(taskId: TaskId): Boolean = allTasks.getTaskData(taskId).deferred

  private fun <T> command(taskId: TaskId, kind: Kind, block: () -> T): T {
    val task = allTasks.getTaskData(taskId)
    return taskLog.capture(
        task.assignee,
        task.instruction.toString(),
        kind = kind,
        taskId = taskId,
        automatic = true,
        block = block,
    )
  }

  private fun policy(taskId: TaskId): AutoExecPolicy {
    val actor = allTasks.getTaskData(taskId).assignee
    return policies.getValue(actor).invoke()
  }

  private fun engineFor(taskId: TaskId): ActorEngine =
      world.actorEngine(allTasks.getTaskData(taskId).assignee)
}
