package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.AutoExecPolicy.SOLE_TASK
import dev.martianzoo.engine.ActorEngine
import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
import dev.martianzoo.engine.InputRequiredException
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Motivation
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue

/** Optional Player-policy execution; the engine handles mandatory Admin work. */
internal class AutoExecLoop(
    private val world: World,
    private val validateApplication: () -> Unit,
) {
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
    val pending = selected?.let(::listOf) ?: allTasks.ids().filterNot(::waitsForEarlierWork)

    // Selection probes execute in disposable copies. Probe only when automatic work is possible.
    if (pending.none { policy(it) != NONE }) return false
    // Keep tasks awaiting a client choice among the candidates: they compete with automatic work.
    // A sole task has no competing choice. Selection can hand work off;
    // the enclosing Agent call rolls back any failure that propagates.
    val candidates =
        if (selected != null || pending.size == 1) pending else pending.filter(::canSelectTask)
    val candidateCounts = candidates.groupingBy { allTasks.getTaskData(it).assignee }.eachCount()
    val options = candidates.filter { taskId ->
      if (isAdmin(taskId)) return@filter false
      val actor = allTasks.getTaskData(taskId).assignee
      when (policy(taskId)) {
        NONE -> false
        SOLE_TASK -> candidateCounts.getValue(actor) == 1
        else -> true
      }
    }
    when (options.size) {
      0 -> {
        val taskId =
            allTasks.ids().firstOrNull { taskId ->
              !isAdmin(taskId) && policy(taskId) != NONE && !waitsForEarlierWork(taskId)
            } ?: return false
        if (selected != null || candidates.isNotEmpty()) return false
        val engine = engineFor(taskId)
        engine.transaction(Motivation.PLAYER_POLICY) {
          executeConcretePlayerTask(engine, taskId)
        }
        error("that should've completed")
      }
      // A task awaiting a client choice still competes with this option. Do not acquire an
      // open task selection merely because the other Actor has autoexecution disabled.
      1 ->
          if (candidates.size == 1) {
            val taskId = options.single()
            val engine = engineFor(taskId)
            try {
              var progressed = false
              world.timeline.atomic {
                engine.transaction(Motivation.PLAYER_POLICY) {
                  engine.selectTask(taskId)
                  progressed =
                      taskId !in allTasks ||
                          allTasks.getTaskData(taskId).assignee != engine.actor ||
                          (canExecuteWithoutChoice(taskId) &&
                              run {
                                try {
                                  engine.doTask(taskId)
                                  taskId !in allTasks
                                } catch (e: NotNowException) {
                                  throw DeadEndException(e)
                                } catch (_: NotFullySpecifiedException) {
                                  false
                                }
                              })
                  try {
                    validateApplication()
                  } catch (_: InputRequiredException) {
                    progressed = false
                    throw AbortTransactionException()
                  }
                }
              }
              if (progressed) return true
            } catch (e: DeadEndException) {
              // Current explicit rejection stops handling immediately, without retrying other work.
              throw e.cause ?: e
            }
          }
    }

    var recoverable = false
    for (taskId in options) {
      var executed = false
      world.timeline.atomic {
        val engine = engineFor(taskId)
        engine.transaction(Motivation.PLAYER_POLICY) {
          // Only a Player attempt is optional. Admin work runs after this block and its failure
          // must escape the call rather than being treated as another Player option.
          try {
            executeConcretePlayerTask(engine, taskId)
            validateApplication()
            executed = true
          } catch (_: InputRequiredException) {
            recoverable = true
            throw AbortTransactionException()
          } catch (e: IncompleteAnalysisException) {
            throw e
          } catch (_: NotFullySpecifiedException) {
            recoverable = true
            throw AbortTransactionException()
          } catch (_: NotNowException) {
            if (allTasks.getTaskData(taskId).instruction.needsNarrowing(world.reader)) {
              recoverable = true
            }
            throw AbortTransactionException()
          }
        }
      }
      if (executed) return true
    }
    if (!recoverable) throw DeadEndException("")
    return false
  }

  /** Existing Player policies do not authorize transaction-based choice inference. */
  private fun executeConcretePlayerTask(engine: ActorEngine, taskId: TaskId) {
    engine.selectTask(taskId)
    if (allTasks.getTaskData(taskId).assignee != engine.actor) return
    if (!canExecuteWithoutChoice(taskId)) {
      throw NotFullySpecifiedException(
          "Player policy requires a selected instruction needing no further narrowing"
      )
    }
    engine.doTask(taskId)
  }

  private fun canExecuteWithoutChoice(taskId: TaskId): Boolean {
    val instruction = allTasks.getTaskData(taskId).instruction
    return instruction is InstructionGroup || !instruction.needsNarrowing(world.reader)
  }

  private fun isAdmin(taskId: TaskId): Boolean = allTasks.getTaskData(taskId).assignee == ADMIN

  private fun canSelectTask(taskId: TaskId): Boolean = engineFor(taskId).canSelectTask(taskId)

  private fun waitsForEarlierWork(taskId: TaskId): Boolean =
      allTasks.getTaskData(taskId).deferral > allTasks.earliestDeferral

  /** Admin is always fully automatic; a configured or overridden policy applies to Players only. */
  private fun policy(taskId: TaskId): AutoExecPolicy {
    val actor = allTasks.getTaskData(taskId).assignee
    if (actor == ADMIN) return NONE
    return policies.getValue(actor).invoke()
  }

  private fun engineFor(taskId: TaskId): ActorEngine =
      world.actorEngine(allTasks.getTaskData(taskId).assignee)
}
