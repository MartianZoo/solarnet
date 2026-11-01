package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.ActorEngine
import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Motivation
import dev.martianzoo.state.RecordedInput.Kind
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue

/** Mandatory Admin handling followed by optional Player-policy execution. */
internal class AutoExecLoop(
    private val world: World,
    private val taskLog: TaskLog,
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
    do {
      runAdmin()
    } while (actOnce())
  }

  private fun actOnce(): Boolean {
    if (allTasks.isEmpty()) return false
    val selected = allTasks.selectedTask()
    val pending = selected?.let(::listOf) ?: allTasks.ids().filterNot(::isDeferred)

    // Selection probes execute in disposable copies. Probe only when automatic work is possible.
    if (pending.none { policy(it) != NONE }) return false
    // Keep manual tasks among the candidates: they still compete with automatic work.
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
        CONCRETE -> candidateCounts.getValue(actor) == 1
        else -> true
      }
    }
    when (options.size) {
      0 -> {
        val taskId =
            allTasks.ids().firstOrNull { taskId ->
              !isAdmin(taskId) && policy(taskId) != NONE && !isDeferred(taskId)
            } ?: return false
        if (selected != null || candidates.isNotEmpty()) return false
        val engine = engineFor(taskId)
        engine.transaction(motivationFor(engine.actor)) {
          command(taskId, Kind.DO_TASK_ID) { executeConcretePlayerTask(engine, taskId) }
        }
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
                engine.transaction(motivationFor(engine.actor)) {
                  command(taskId, Kind.CHOOSE) { engine.selectTask(taskId) }
                  progressed =
                      allTasks.getTaskData(taskId).assignee != engine.actor ||
                          (canExecuteWithoutChoice(taskId) &&
                              command(taskId, Kind.TRY_SELECTED) {
                                try {
                                  engine.doTask(taskId)
                                  taskId !in allTasks
                                } catch (e: NotNowException) {
                                  throw DeadEndException(e)
                                } catch (_: NotFullySpecifiedException) {
                                  false
                                }
                              })
                }
                validateApplication()
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

    var recoverable = false
    for (taskId in options) {
      try {
        world.timeline.atomic {
          val engine = engineFor(taskId)
          engine.transaction(motivationFor(engine.actor)) {
            command(taskId, Kind.DO_TASK_ID) { executeConcretePlayerTask(engine, taskId) }
          }
          validateApplication()
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

  /** Existing Player policies do not authorize transaction-based choice inference. */
  private fun executeConcretePlayerTask(engine: ActorEngine, taskId: TaskId) {
    engine.selectTask(taskId)
    if (allTasks.getTaskData(taskId).assignee != engine.actor) return
    if (!canExecuteWithoutChoice(taskId)) {
      throw NotFullySpecifiedException("Player policy requires a concrete selected instruction")
    }
    engine.doTask(taskId)
  }

  private fun canExecuteWithoutChoice(taskId: TaskId): Boolean {
    val instruction = allTasks.getTaskData(taskId).instruction
    return instruction is Each ||
        instruction is InstructionGroup ||
        !instruction.isAbstract(world.reader)
  }

  /** Automatic handling is mandatory for Admin and configured policy for every other Actor. */
  private fun motivationFor(actor: Actor): Motivation =
      if (actor == ADMIN) Motivation.ADMIN_EAGER_POLICY else Motivation.PLAYER_POLICY

  private fun isAdmin(taskId: TaskId): Boolean = allTasks.getTaskData(taskId).assignee == ADMIN

  /**
   * Attempts Admin work in FIFO order, retrying waiting tasks after each success. A selected Player
   * task retains priority. Every nonempty pass must succeed at a task; otherwise its retained
   * failure is thrown, including unresolved choices and work waiting on a Player. An unfinished
   * proof is retried after progress and takes precedence over gameplay rejection when the pass
   * stalls. Missing application input is also retried after progress and reported before returning
   * to Player work.
   *
   * Complete enumeration of legal narrowings remains outside this implementation.
   */
  internal fun runAdmin() {
    val waiting = mutableListOf<TaskId>()
    while (true) {
      val selected = allTasks.selectedTask()
      // A failing selected Admin task cannot wait for competing Player work: selection blocks it.
      if (selected != null && !isAdmin(selected)) return
      val pending =
          (selected?.let(::listOf) ?: allTasks.ids().toList()).filter {
            isAdmin(it) && !isDeferred(it)
          }
      waiting.retainAll(pending.toSet())
      waiting.addAll(pending.filterNot { it in waiting })
      if (waiting.isEmpty()) return
      var rejection: Exception? = null
      var incomplete: IncompleteAnalysisException? = null
      var inputRequired: AgentInputRequiredException? = null
      var progressed = false
      repeat(waiting.size) {
        if (!progressed) {
          val taskId = waiting.removeAt(0)
          try {
            world.timeline.atomic {
              val engine = engineFor(taskId)
              engine.transaction(Motivation.ADMIN_EAGER_POLICY) {
                command(taskId, Kind.DO_TASK_ID) { engine.doTask(taskId) }
              }
              validateApplication()
            }
            progressed = true
          } catch (e: AgentInputRequiredException) {
            waiting += taskId
            inputRequired = inputRequired ?: e
          } catch (e: IncompleteAnalysisException) {
            waiting += taskId
            incomplete = incomplete ?: e
          } catch (e: NotFullySpecifiedException) {
            waiting += taskId
            rejection = e
          } catch (e: RequirementException) {
            waiting += taskId
            rejection = e
          } catch (e: NotNowException) {
            waiting += taskId
            rejection = e
          } catch (e: DeadEndException) {
            waiting += taskId
            rejection = e
          }
        }
      }
      if (!progressed) {
        inputRequired?.let { throw it }
        incomplete?.let { throw it }
        throw checkNotNull(rejection) { "Admin made no progress without retaining a failure" }
      }
    }
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

  /** Admin is always fully automatic; a configured or overridden policy applies to Players only. */
  private fun policy(taskId: TaskId): AutoExecPolicy {
    val actor = allTasks.getTaskData(taskId).assignee
    if (actor == ADMIN) return AutoExecPolicy.EAGER
    return policies.getValue(actor).invoke()
  }

  private fun engineFor(taskId: TaskId): ActorEngine =
      world.actorEngine(allTasks.getTaskData(taskId).assignee)
}
