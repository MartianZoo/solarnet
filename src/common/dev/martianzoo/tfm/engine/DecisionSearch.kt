package dev.martianzoo.tfm.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agent.TaskForm.Decision.Kind
import dev.martianzoo.engine.AgentInputRequiredException
import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Deferral
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Searches a private copy of a live game for ways [player] can finish the work under way. The work
 * is over when the World's `FINALLY` work begins, as for [dev.martianzoo.agent.Agent]'s operation
 * completion, or when no task remains. The live game is never changed.
 *
 * Every Player edge is tried: selecting each pending task and each complete form instruction
 * reachable through listed choices. Admin performs its own work eagerly, as in play.
 *
 * Speculation never names cards. Unplayed cards are anonymous card backs, as the World models them,
 * so no deck is consulted and which card moves is never a branch. [tracker] is only read, once, for
 * the faces the Player holds when the search begins; a narrowing that names a card face offers
 * those. A face drawn during the explored line is unknowable, so a line that needs one is
 * undecided, never a dead end. Merger is exempt: its rules let a Player start it without proving
 * the turn can finish, and roll back if the drawn corporations make that impossible.
 *
 * With [playerPolicy] SAFE, tasks proven safe are performed as soon as they are available, without
 * trying their other orders; that is sound exactly when the SAFE proof is. NONE explores every
 * order and serves as a reference.
 *
 * Verdict calls on the same search share [budget] and optional [maxDuration], so displaying many
 * options cannot multiply its work without limit. Results are exact while the search stays within
 * both limits. Anything the search cannot evaluate, including an exhausted budget, a choice the
 * options API cannot list, a choice with more options than the search lists, or an engine failure
 * that is not a gameplay rejection, makes the answer [Verdict.UNKNOWN] rather than a dead end.
 */
public class DecisionSearch(
    source: Agents,
    private val tracker: CardTracking?,
    private val budget: Int = DEFAULT_BUDGET,
    playerPolicy: AutoExecPolicy = AutoExecPolicy.SAFE,
    private val trace: ((String) -> Unit)? = null,
    private val maxDuration: Duration? = null,
) {
  /** Whether a choice can still lead to the end of the work under way. */
  public enum class Verdict {
    COMPLETES,
    DEAD_END,
    UNKNOWN,
  }

  /** A completed state, compared by its components and pending work. */
  public class Outcome internal constructor(private val key: Any) {
    override fun equals(other: Any?): Boolean = other is Outcome && key == other.key

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = key.toString()
  }

  // A fork carries no transaction guard, so card movements in it stay anonymous.
  private val agents: Agents = source.fork()
  private val world: World = agents.world
  private val root: Checkpoint = world.timeline.checkpoint()
  private val cardBack = world.classTable.getClass(cn("CardBack"))

  init {
    world.actors.filterIsInstance<Player>().forEach { agents[it].autoExecPolicy = playerPolicy }
  }

  private var remaining = budget
  private var started = TimeSource.Monotonic.markNow()
  private var requireEveryPath = false

  /** Positions already shown to finish (true) or to be dead ends (false). */
  private val known = mutableMapOf<Any, Boolean>()

  private class OutOfBudget : RuntimeException()

  private class Unsupported(message: String) : RuntimeException(message)

  /** Whether [command], applied as the next input, can still lead to completion. */
  public fun verdict(command: (Agents) -> Unit): Verdict = verdictOf {
    attempt({ command(agents) }, ::settledLive)
  }

  /**
   * Whether the exact provisional [instruction] for [taskId] can finish. Remaining form choices are
   * explored before any form is committed, so a Class default cannot stand in for an open choice.
   * An invalid or unlistable form is undecided rather than proof that the task cannot finish.
   */
  public fun verdictForm(taskId: TaskId, instruction: InstructionTree): Verdict = verdictOf {
    val task = world.tasks.getTaskData(taskId)
    searchChoices(narrowings(task, instruction))
  }

  private fun verdictOf(evaluate: () -> Boolean?): Verdict {
    if (remaining <= 0 || expired()) return Verdict.UNKNOWN
    return try {
      when (evaluate()) {
        true -> Verdict.COMPLETES
        false -> Verdict.DEAD_END
        null -> Verdict.UNKNOWN
      }
    } catch (_: OutOfBudget) {
      trace?.invoke("unknown: search step or time limit reached")
      Verdict.UNKNOWN
    } catch (failure: Exception) {
      trace?.invoke("unknown: ${failure::class.simpleName}: ${failure.message}")
      Verdict.UNKNOWN
    }
  }

  /**
   * Every completed outcome reachable after [command], or from the current position when it is
   * null; null when the search cannot finish. For verification: the result ignores [playerPolicy]'s
   * reduction only if that policy is NONE.
   */
  public fun outcomes(command: ((Agents) -> Unit)? = null): Set<Outcome>? {
    remaining = budget
    started = TimeSource.Monotonic.markNow()
    requireEveryPath = false
    outcomeCache.clear()
    return try {
      if (command == null) settledOutcomes() else attemptOutcomes({ command(agents) })
    } catch (_: OutOfBudget) {
      null
    } catch (_: Unsupported) {
      null
    }
  }

  /** The sole completed outcome, only if every explored choice reaches it within [budget]. */
  public fun uniformOutcome(): Outcome? {
    remaining = budget
    started = TimeSource.Monotonic.markNow()
    requireEveryPath = true
    outcomeCache.clear()
    return try {
      settledOutcomes().singleOrNull()
    } catch (_: OutOfBudget) {
      null
    } catch (_: Unsupported) {
      null
    } finally {
      requireEveryPath = false
    }
  }

  // EXISTENCE SEARCH

  /**
   * Runs [command] in a rolled-back trial and evaluates the resulting position with [next]. True if
   * it completes, false if it is a dead end, null if unknown.
   */
  private fun attempt(command: () -> Unit, next: () -> Boolean?): Boolean? {
    var result: Boolean? = false
    try {
      world.timeline.atomic {
        command()
        result = next()
        throw AbortTransactionException()
      }
    } catch (failure: Exception) {
      val dead = isDeadEnd(failure)
      trace?.invoke(
          "${if (dead) "dead" else "unknown"}: ${failure::class.simpleName}: ${failure.message}"
      )
      return if (dead) false else null
    }
    return result
  }

  private fun settledLive(): Boolean? {
    spend()
    if (isOver()) return true
    adminWork()?.let { task ->
      return attempt({ agents[ADMIN].doTask(task) }, ::settledLive)
    }
    val key = position()
    known[key]?.let {
      return it
    }
    val choices =
        edges() ?: return null.also { trace?.invoke("unknown: unlisted choice in ${world.tasks}") }
    val result = searchChoices(choices)
    if (result != null) known[key] = result
    return result
  }

  private fun searchChoices(choices: Choices): Boolean? {
    var unknown = false
    for (edge in choices.commands) {
      when (attempt(edge, ::settledLive)) {
        true -> return true
        null -> unknown = true
        false -> {}
      }
    }
    choices.unlistedFaces?.let { task ->
      if (task.cause?.context?.className == MERGER) return true
      trace?.invoke("unknown: a face drawn during this line may be playable: ${task.instruction}")
      return null
    }
    choices.unlisted?.let {
      trace?.invoke("unknown: only the first $MAX_LISTED_OPTIONS options were tried for $it")
      return null
    }
    if (unknown) return null
    return false
  }

  // OUTCOME ENUMERATION

  private val outcomeCache = mutableMapOf<Any, Set<Outcome>>()

  private fun attemptOutcomes(command: () -> Unit): Set<Outcome> {
    if (requireEveryPath) spend()
    var result: Set<Outcome> = emptySet()
    try {
      world.timeline.atomic {
        command()
        result = settledOutcomes()
        throw AbortTransactionException()
      }
    } catch (failure: Exception) {
      if (failure is OutOfBudget || failure is Unsupported) throw failure
      trace?.invoke("${failure::class.simpleName}: ${failure.message}")
      if (isDeadEnd(failure)) {
        if (requireEveryPath) throw Unsupported("an ordering cannot complete")
        return emptySet()
      }
      throw Unsupported("${failure::class.simpleName}: ${failure.message}")
    }
    return result
  }

  private fun settledOutcomes(): Set<Outcome> {
    spend()
    if (isOver()) return setOf(Outcome(position()))
    adminWork()?.let { task ->
      return attemptOutcomes { agents[ADMIN].doTask(task) }
    }
    val key = position()
    outcomeCache[key]?.let {
      return it
    }
    val choices = edges() ?: throw Unsupported("unlisted choice at ${world.tasks}")
    if (choices.unlistedFaces != null) throw Unsupported("unknowable card face")
    val outcomes = mutableSetOf<Outcome>()
    for (command in choices.commands) outcomes += attemptOutcomes(command)
    if (choices.unlisted != null) {
      throw Unsupported("too many options for ${choices.unlisted}")
    }
    return outcomes.also { outcomeCache[key] = it }
  }

  // SHARED MECHANICS

  /**
   * The Player inputs available at a position. [unlistedFaces] is the task whose card-face choice
   * may also have faces drawn during this line, which no list can name. [unlisted] is the
   * instruction whose options continue past the ones listed in [commands].
   */
  private class Choices(
      val commands: List<() -> Unit>,
      val unlistedFaces: Task? = null,
      val unlisted: InstructionTree? = null,
  )

  /** An executable Admin task, which Admin performs eagerly in play. */
  private fun adminWork(): TaskId? {
    val admin = agents[ADMIN]
    return admin.tasks.matching { !it.deferred && admin.canExecuteTask(it.id) }.firstOrNull()
  }

  /** Whether the work under way is over: `FINALLY` work began, or nothing remains. */
  private fun isOver(): Boolean =
      world.tasks.isEmpty() ||
          world.events.entriesSince(root).any {
            it is TaskEditedEvent &&
                it.oldTask.deferral == Deferral.FINALLY &&
                it.task.deferral == Deferral.NONE
          }

  /** The Player inputs available here, each as a command; null if one cannot be listed. */
  private fun edges(): Choices? {
    val selected = world.tasks.selectedTask()?.let(world.tasks::getTaskData)
    if (selected != null) {
      // Admin settles its own work; a selected Admin task it cannot finish is not a Player edge.
      if (selected.assignee !is Player) return null
      return narrowings(selected)
    }
    val pending = world.tasks.extract { it }.filter { it.assignee is Player && !it.deferred }
    // Stuck Admin work is not shown to be a dead end; leave the question open.
    if (pending.isEmpty()) return null
    // Tasks caused by different components can produce different consequences when they finish.
    return Choices(
        pending
            .distinctBy { listOf(it.assignee, it.instruction, it.then, it.cause?.context) }
            .map { task -> { agents[task.assignee].selectTask(task.id) } }
    )
  }

  private fun narrowings(task: Task, initial: InstructionTree = task.instruction): Choices {
    val agent = agents[task.assignee]
    val commands = mutableListOf<() -> Unit>()
    var unlisted: InstructionTree? = null
    var unlistedFaces: Task? = null
    fun visit(instruction: InstructionTree) {
      spend()
      if (commands.size == MAX_LISTED_OPTIONS) {
        unlisted = instruction
        return
      }
      val form = agent.fillInTask(task.id, instruction)
      val decision =
          try {
            form.decisions().singleOrNull()
          } catch (_: UnsupportedOperationException) {
            unlisted = instruction
            return
          }
      if (decision == null) {
        commands.add {
          agent.fillInTask(task.id, instruction).commit()
        }
        return
      }
      // A named face drawn during this line cannot be known from the starting hand.
      val faces =
          tracker?.playableFaces(task.assignee as Player, instruction, decision.focus)?.mapTo(
              hashSetOf()
          ) {
            "$it"
          }
      if (faces != null && cardsDrawnSinceRoot()) unlistedFaces = task
      val passArms =
          (instruction as? Or)
              ?.instructions
              ?.filterIsInstance<Change>()
              ?.filter { it.gaining?.className == PASS || it.gaining?.className == PASS_BATON }
              ?.mapTo(hashSetOf()) { it.toString() }
              .orEmpty()
      val listed =
          try {
            form
                .options(decision)
                .filter { faces == null || it == "Ok" || it in faces }
                .onEach { if (expired()) throw OutOfBudget() }
                .take(MAX_LISTED_OPTIONS + 1)
                .toList()
          } catch (_: UnsupportedOperationException) {
            unlisted = instruction
            return
          }
      if (listed.size > MAX_LISTED_OPTIONS) unlisted = instruction
      // The middle and endpoints often witness a completable numeric range without walking up
      // from 1. The remaining amounts must still be tried before declaring a dead end.
      val candidates = listed.take(MAX_LISTED_OPTIONS)
      val ordered =
          if (decision.kind == Kind.AMOUNT) {
            (listOfNotNull(
                    candidates.firstOrNull { it == "Ok" },
                    candidates.getOrNull(candidates.size / 2),
                    candidates.lastOrNull(),
                    candidates.firstOrNull(),
                ) + candidates)
                .distinct()
          } else candidates.sortedByDescending { it in passArms || it == "Ok" }
      for (option in ordered) {
        val next = agent.fillInTask(task.id, instruction)
        next.choose(next.decisions().single(), option)
        visit(next.instruction)
      }
    }
    visit(initial)
    return Choices(commands, unlistedFaces, unlisted)
  }

  private fun expired(): Boolean = maxDuration?.let { started.elapsedNow() >= it } == true

  private fun spend() {
    if (--remaining < 0 || expired()) throw OutOfBudget()
  }

  /** Whether any card back arrived from outside the World during the explored line. */
  private fun cardsDrawnSinceRoot(): Boolean =
      world.events.changesSince(root).any { event ->
        event.change.gaining?.type?.rootClass?.isSubtypeOf(cardBack) == true &&
            event.change.removing?.type?.rootClass?.isSubtypeOf(cardBack) != true
      }

  /**
   * A key for this position: changes since the root and pending tasks. A task's cause enters only
   * as the component whose effect created it; trigger event ordinals are diagnostic identities that
   * no rule reads, and keeping them would stop different orders that reach one state from merging.
   */
  private fun position(): Any {
    val delta = HashMap<Type, Int>()
    for (event in world.events.changesSince(root)) {
      val change = event.change
      change.gaining?.let { delta[it.type] = (delta[it.type] ?: 0) + change.count }
      change.removing?.let { delta[it.type] = (delta[it.type] ?: 0) - change.count }
    }
    delta.values.removeAll { it == 0 }
    val tasks =
        world.tasks
            .extract {
              "${it.assignee}|${it.selected}|${it.deferral}|${it.instruction}|${it.then}|" +
                  "${it.cause?.context}"
            }
            .sorted()
    return listOf(delta, tasks)
  }

  /** Whether [failure] is the game rejecting this path, rather than an unanswerable question. */
  private fun isDeadEnd(failure: Exception): Boolean =
      when (failure) {
        is OutOfBudget,
        is Unsupported -> throw failure
        is AgentInputRequiredException -> false
        is DeadEndException -> failure.cause == null || failure.cause is GameplayException
        is NotNowException,
        is NarrowingException -> true
        else -> false
      }

  private companion object {
    const val DEFAULT_BUDGET = 20_000
    // Larger than any map's area choice or a hand of cards; amounts beyond it are left unknown.
    const val MAX_LISTED_OPTIONS = 100
    val PASS = cn("Pass")
    val PASS_BATON = cn("PassBaton")
    val MERGER = cn("Merger")
  }
}
