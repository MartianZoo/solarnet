package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.Player
import dev.martianzoo.state.RecordedInput
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId

/**
 * Opt-in Actor-input capture for engine reconstruction, retaining submitted instruction syntax.
 *
 * Captures task submissions, selections, form commits, successful tries, drops, [Agent.exMachina],
 * and policy-executed tasks for every Actor. [RecordedInput.Command.kind] preserves distinct Agent
 * operations, including task selection, single-task execution, and corrections. A task completed
 * through a handoff records the originating choice separately from the receiving Actor's execution.
 * Failed, no-op, and rolled-back submissions are omitted. Later form commits replace earlier form
 * narrowings of that task when those earlier steps only edited its instruction. Selection steps
 * remain because later narrowing may require the selected Actor. Intermediate event history is not
 * preserved.
 *
 * Reconstruction uses the same premise and implementation, with every Player's autoexecution
 * disabled; Admin's handling is automatic and always runs. Game start is recorded explicitly. Task
 * selectors retain causal context instead of source event ordinals. External card identities are
 * carried by [RecordedInput.Command], but their Agent-side capture awaits integration with
 * CardTracking.
 */
public class TaskLog internal constructor(private val world: World) {
  private class Submission(
      val actor: Actor,
      val kind: RecordedInput.Kind,
      val instruction: String,
      val taskSelector: RecordedInput.TaskSelector?,
      val formTask: TaskId?,
      val automatic: Boolean = false,
      val contextClass: ClassName? = null,
      var anchor: GameEvent? = null,
      var onlyNarrows: Boolean = false,
      var selectsTask: Boolean = false,
      var awaitsInput: Boolean = false,
  )

  private var recording = false
  private var gameStartRecorded = false
  private val submissions = mutableListOf<Submission>()

  /** Starts retaining subsequent submissions. Call before starting the recorded gameplay. */
  public fun start() {
    check(!recording) { "task recording has already started" }
    recording = true
  }

  /** Records the game start, which precedes every submitted game task. */
  public fun gameStarted() {
    if (!recording) return
    check(!gameStartRecorded) { "game start already recorded" }
    gameStartRecorded = true
  }

  /**
   * Structured inputs for the shared game file, with rolled-back submissions removed.
   *
   * Admin's automatic handling is left out. It is not configurable, so reconstruction performs it
   * again on its own; recording it would replay work that is already done and fail to find its
   * task. Admin's own corrections are still recorded, since nothing repeats those.
   */
  public fun inputs(): List<RecordedInput> {
    check(gameStartRecorded) { "game start was not recorded" }
    return listOf(RecordedInput.StartGame) +
        survivingSubmissions()
            .filterNot { it.automatic && it.actor == ADMIN }
            .map { item ->
              RecordedInput.Command(
                  item.actor,
                  item.kind,
                  item.instruction,
                  taskSelector = item.taskSelector,
                  contextClass =
                      item.contextClass.takeIf { item.kind == RecordedInput.Kind.DO_TASK },
              )
            }
  }

  private fun survivingSubmissions(): List<Submission> {
    val events = world.events.entriesSince(0)
    val surviving = submissions.filter { item ->
      item.anchor?.let { events.getOrNull(it.ordinal) === it } == true
    }
    val lastFormCommit = surviving.filter { it.formTask != null }.associateBy { it.formTask }
    return surviving.filter { !it.onlyNarrows || lastFormCommit[it.formTask] === it }
  }

  /**
   * Positions before Player task selections and replay corrections. An automatic selection that
   * leaves an abstract task awaiting input also exposes a choice; automatic execution does not.
   */
  public fun selectionCheckpoints(): List<Checkpoint> =
      survivingSubmissions()
          .filter {
            it.kind == RecordedInput.Kind.EX_MACHINA ||
                (it.actor is Player && it.selectsTask && (!it.automatic || it.awaitsInput))
          }
          .map { Checkpoint(checkNotNull(checkNotNull(it.anchor).transaction).serial) }
          .distinct()

  internal fun <T> capture(
      actor: Actor,
      instruction: String,
      formTask: TaskId? = null,
      kind: RecordedInput.Kind = RecordedInput.Kind.DO_TASKS,
      taskId: TaskId? = null,
      automatic: Boolean = false,
      contextClass: ClassName? = null,
      block: () -> T,
  ): T {
    if (!recording) return block()
    val eventCursor = world.events.nextOrdinal
    val submission =
        Submission(
            actor,
            kind,
            instruction,
            taskId?.let { taskSelectorFor(actor, it) },
            formTask,
            automatic = automatic,
            contextClass = contextClass,
        )
    submissions += submission
    return try {
      block().also {
        val events = world.events.entriesSince(eventCursor)
        submission.anchor = events.firstOrNull()
        submission.selectsTask = events.any { event ->
          when (event) {
            is GameEvent.TaskEditedEvent -> !event.oldTask.selected && event.task.selected
            is GameEvent.TaskRemovedEvent -> !event.task.selected && kind != RecordedInput.Kind.DROP
            else -> false
          }
        }
        submission.awaitsInput =
            world.tasks.selectedTask()?.let {
              val instruction = world.tasks.getTaskData(it).instruction
              instruction !is Each && instruction.isAbstract(world.reader)
            } == true
        // A later full form instruction includes provisional instruction choices. Keep selection,
        // structural changes, continuations and Actor handoffs, which have independent effects.
        submission.onlyNarrows =
            formTask != null &&
                events.all {
                  it is GameEvent.TaskEditedEvent &&
                      it.task.id == formTask &&
                      it.oldTask.copy(
                          instruction = it.task.instruction,
                      ) == it.task
                }
      }
    } finally {
      if (submission.anchor == null) submissions.remove(submission)
    }
  }

  private fun taskSelectorFor(
      actor: Actor,
      taskId: TaskId,
  ): RecordedInput.TaskSelector {
    val taskInstruction = world.tasks.getTaskData(taskId).instruction.toString()
    val matches =
        world.tasks
            .extract { it }
            .filter {
              it.assignee == actor && it.instruction.toString() == taskInstruction
            }
    val target = matches.single { it.id == taskId }
    // A unique instruction identifies its task without copying the game's causal history.
    if (matches.size == 1) return RecordedInput.TaskSelector(taskInstruction, emptyList(), 0)
    val cause = causeSteps(world, target)
    val peers = matches.filter { it.id == taskId || causeSteps(world, it) == cause }
    return RecordedInput.TaskSelector(taskInstruction, cause, peers.indexOf(target))
  }

  public companion object {
    /**
     * Replays the shared file's inputs with every Player's policy disabled. Admin still performs
     * its own work, which is automatic and not configurable, so a recorded Admin command may find
     * its task already handled. [startGame] starts the game; [supplyCardInputs] runs before a
     * command that needs supplied card names. A failed command stops replay and leaves earlier
     * successful commands applied.
     */
    public fun replay(
        inputs: List<RecordedInput>,
        agents: Agents,
        startGame: () -> Unit,
        supplyCardInputs: ((RecordedInput.Command) -> Unit)? = null,
    ) {
      require(inputs.firstOrNull() == RecordedInput.StartGame) { "game inputs must start the game" }
      require(inputs.drop(1).none { it == RecordedInput.StartGame })
      agents.world.actors.filterIsInstance<Player>().forEach { agents[it].autoExecPolicy = NONE }
      startGame()
      inputs.drop(1).forEachIndexed { index, input ->
        val command = input as RecordedInput.Command
        try {
          if (command.cardInputs.isNotEmpty()) {
            requireNotNull(supplyCardInputs) { "card identities have no input supplier" }(command)
          }
          val agent = agents[command.actor]
          val contextClass = command.contextClass
          val target =
              command.taskSelector?.let { selector ->
                val candidates =
                    agent.tasks
                        .extract { it }
                        .filter { task ->
                          task.instruction.toString() == selector.instruction
                        }
                candidates.singleOrNull()
                    ?: candidates
                        .filter { causeSteps(agents.world, it) == selector.cause }
                        .getOrNull(selector.occurrence)
                    ?: error("recorded task is absent: ${command.instruction}")
              }
          when (command.kind) {
            RecordedInput.Kind.DO_TASKS -> agent.doTasks(command.instruction)
            RecordedInput.Kind.DO_TASK ->
                when {
                  contextClass != null -> agent.doTask(command.instruction, contextClass)
                  target != null -> agent.doTask(command.instruction, target.id)
                  else -> agent.doTask(command.instruction)
                }
            RecordedInput.Kind.DO_TASK_ID ->
                agent.doTask(
                    (target ?: requireNotNull(matchingTask(agent, command.instruction))).id
                )
            RecordedInput.Kind.TRY_TASK ->
                if (target == null) agent.tryTask(command.instruction)
                else agent.tryTask(command.instruction, target.id)
            RecordedInput.Kind.TRY_TASK_ID ->
                agent.tryTask(
                    (target ?: requireNotNull(matchingTask(agent, command.instruction))).id
                )
            RecordedInput.Kind.CHOOSE ->
                if (target == null) choose(agent, command.instruction)
                else if (target.instruction.toString() == command.instruction)
                    agent.selectTask(target.id)
                else
                    agent.selectTask(
                        target.copy(
                            instruction =
                                agent.parseAs(InstructionTree::class, command.instruction)
                                    as InstructionTree
                        )
                    )
            RecordedInput.Kind.TRY_SELECTED ->
                agent.tryTask(checkNotNull(agent.tasks.selectedTask()) { "no selected task" })
            RecordedInput.Kind.DROP ->
                agent.dropTask(
                    (target ?: requireNotNull(matchingTask(agent, command.instruction))).id
                )
            RecordedInput.Kind.EX_MACHINA -> agents[command.actor].exMachina(command.instruction)
          }
        } catch (e: Exception) {
          throw IllegalArgumentException("cannot replay game input ${index + 2}: $input", e)
        }
      }
    }

    private fun choose(agent: Agent, instruction: String) {
      val exact = matchingTask(agent, instruction)
      if (exact != null) agent.selectTask(exact.id) else agent.selectTask(instruction)
    }

    private fun matchingTask(agent: Agent, instruction: String): Task? {
      val matches = agent.tasks.extract { it }.filter { it.instruction.toString() == instruction }
      if (matches.isEmpty()) return null
      val first = matches.first()
      require(matches.all { it.copy(id = first.id, cause = first.cause) == first }) {
        "ambiguous task selection: $instruction"
      }
      return first
    }
  }
}

private fun causeSteps(world: World, task: Task): List<RecordedInput.CauseStep> = buildList {
  var cause = task.cause
  while (cause != null) {
    val trigger = checkNotNull(world.events.changeAt(cause.triggerEvent))
    add(
        RecordedInput.CauseStep(
            cause.context.toString(),
            trigger.actor.className.toString(),
            trigger.change.toString(),
        )
    )
    cause = trigger.cause
  }
}
