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
import dev.martianzoo.state.RecordedInput.Kind
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue

/**
 * Current shared task handling for Admin and configurable Player policies.
 *
 * TODO(ENGINE_CONTRACT): separate mandatory Admin handling from optional Player policies. The
 *   proposed contract requires ordinary Admin work to finish before a Player command starts or
 *   returns.
 */
internal class AutoExecLoop(private val world: World, private val taskLog: TaskLog) {
  private val allTasks: TaskQueue
    get() = world.tasks

  private val policies = mutableMapOf<Actor, () -> AutoExecPolicy>()

  /**
   * The event ordinal at which the current operation, through any of these Agents, last began or
   * made a decision. Its work is over once the World's `FINALLY` work has begun since then.
   */
  // TODO(ENGINE_CONTRACT): investigate removing this operation-completion convention. Reaching
  // FINALLY does not define either a transaction or the call the Player awaits.
  internal var lastDecision: Int? = null

  internal fun register(actor: Actor, policy: () -> AutoExecPolicy) {
    check(policies.put(actor, policy) == null) { "Agent already registered for $actor" }
  }

  internal fun run() {
    while (actOnce()) {}
  }

  private fun actOnce(): Boolean {
    if (allTasks.isEmpty()) return false
    val selected = allTasks.selectedTask()
    val pending = selected?.let(::listOf) ?: allTasks.ids().filterNot(::isDeferred)
    // TODO(ENGINE_CONTRACT): proposed Admin handling requires exactly one fully concrete narrowing
    // whose transaction succeeds. A selection probe can leave choices unresolved and proves less.

    // Selection probes can execute and roll back tasks. Only probe when automatic work is possible.
    if (pending.none { policy(it) != NONE }) return false
    // Keep manual tasks among the candidates: they still compete with automatic work.
    // A sole task has no competing choice. Selection still validates its execution,
    // and the enclosing Agent call rolls back any failure.
    // TODO(ENGINE_CONTRACT): this filter drops failed Admin probes without preserving their
    // reasons.
    // A surviving Player candidate that cannot finish can then hide the Admin failure: a NONE
    // policy returns below (AiCentralTest), and a non-NONE policy can leave an abstract task
    // selected.
    val candidates =
        if (selected != null || pending.size == 1) pending else pending.filter(::canSelectTask)
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
      // abstract task selection merely because the other Actor has autoexecution disabled.
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
              // Current explicit rejection stops handling immediately, without retrying other work.
              throw e.cause ?: e
            }
          }
    }

    // TODO(ENGINE_CONTRACT): proposed FIFO retries move a blocked Admin task to the end, rather
    // than restarting from the oldest task after every success.
    // Recommended failure classification is also under review: DeadEndException currently escapes
    // this loop immediately, while a concrete NotNowException loses its reason in the catch below.
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
    // TODO(ENGINE_CONTRACT): the proposed unsuccessful-full-pass rule must also reject abstract
    // Admin work left pending here. Returning false currently lets a Player act in that state.
    // Collecting rejection reasons and separating rule failures from implementation errors are
    // additional recommendations awaiting review.
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
