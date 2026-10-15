package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.RecordedInput
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId

/**
 * Opt-in Actor-input capture for engine reconstruction, retaining submitted instruction syntax.
 *
 * Captures task submissions, selections, form commits, successful tries, drops, [exMachina], and
 * policy-executed tasks for every Actor. [RecordedInput.Command.kind] preserves distinct Agent
 * operations, including task selection, single-task execution, and corrections. Failed, no-op, and
 * rolled-back submissions are omitted. Later form commits replace earlier form narrowings of that
 * task when those earlier steps only edited its instruction. Selection steps remain because later
 * narrowing may require the selected Actor. Intermediate event history is not preserved.
 *
 * Reconstruction uses the same premise and implementation, with every Actor's autoexecution
 * disabled. Game start is recorded explicitly. Task selectors retain causal context instead of
 * source event ordinals. External card identities are carried by [RecordedInput.Command], but their
 * Agent-side capture awaits integration with CardTracking.
 */
public class TaskLog internal constructor(private val world: World) {
  private class Submission(
      val actor: Actor,
      val kind: RecordedInput.Kind,
      val instruction: String,
      val taskSelector: RecordedInput.TaskSelector?,
      val formTask: TaskId?,
      var anchor: GameEvent? = null,
      var onlyNarrows: Boolean = false,
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

  /** Structured inputs for the shared game file, with rolled-back submissions removed. */
  public fun inputs(): List<RecordedInput> {
    check(gameStartRecorded) { "game start was not recorded" }
    return listOf(RecordedInput.StartGame) +
        survivingSubmissions().map { item ->
          RecordedInput.Command(
              item.actor,
              item.kind,
              item.instruction,
              taskSelector = item.taskSelector,
          )
        }
  }

  private fun survivingSubmissions(): List<Submission> {
    val events = world.events.entriesSince(Checkpoint(0))
    val surviving = submissions.filter { item ->
      item.anchor?.let { events.getOrNull(it.ordinal) === it } == true
    }
    val lastFormCommit = surviving.filter { it.formTask != null }.associateBy { it.formTask }
    return surviving.filter { !it.onlyNarrows || lastFormCommit[it.formTask] === it }
  }

  internal fun <T> capture(
      actor: Actor,
      instruction: String,
      formTask: TaskId? = null,
      kind: RecordedInput.Kind = RecordedInput.Kind.DO_TASKS,
      taskId: TaskId? = null,
      block: () -> T,
  ): T {
    if (!recording) return block()
    val checkpoint = world.timeline.checkpoint()
    val submission =
        Submission(
            actor,
            kind,
            instruction,
            taskId?.let { taskSelectorFor(actor, it) },
            formTask,
        )
    submissions += submission
    return try {
      block().also {
        val events = world.events.entriesSince(checkpoint)
        submission.anchor = events.firstOrNull()
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
    val peers = matches.filter { causeSteps(world, it) == cause }
    return RecordedInput.TaskSelector(taskInstruction, cause, peers.indexOf(target))
  }

  public companion object {
    /**
     * Replays the shared file's inputs with every Actor policy disabled. [startGame] starts the
     * game; [supplyCardInputs] runs before a command that needs supplied card names. A failed
     * command stops replay and leaves earlier successful commands applied.
     */
    public fun replay(
        inputs: List<RecordedInput>,
        agents: Agents,
        startGame: () -> Unit,
        supplyCardInputs: ((RecordedInput.Command) -> Unit)? = null,
    ) {
      require(inputs.firstOrNull() == RecordedInput.StartGame) { "game inputs must start the game" }
      require(inputs.drop(1).none { it == RecordedInput.StartGame })
      agents.world.actors.forEach { agents[it].autoExecPolicy = NONE }
      startGame()
      inputs.drop(1).forEachIndexed { index, input ->
        val command = input as RecordedInput.Command
        try {
          if (command.cardInputs.isNotEmpty()) {
            requireNotNull(supplyCardInputs) { "card identities have no input supplier" }(command)
          }
          val agent = agents[command.actor]
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
                if (target == null) agent.doTask(command.instruction)
                else agent.doTask(command.instruction, target.id)
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
                                agent.parseAs(Instruction::class, command.instruction)
                                    as Instruction
                        )
                    )
            RecordedInput.Kind.TRY_SELECTED ->
                agent.tryTask(checkNotNull(agent.tasks.selectedTask()) { "no selected task" })
            RecordedInput.Kind.DROP ->
                agent.dropTask(
                    (target ?: requireNotNull(matchingTask(agent, command.instruction))).id
                )
            RecordedInput.Kind.EX_MACHINA -> agents.exMachina(command.actor, command.instruction)
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

private fun causeSteps(world: World, task: Task): List<RecordedInput.CauseStep> {
  val cause = task.cause ?: return emptyList()
  val trigger = checkNotNull(world.events.changeAt(cause.triggerEvent))
  return listOf(
      RecordedInput.CauseStep(
          cause.context.toString(),
          trigger.actor.className.toString(),
          trigger.change.toString(),
      )
  ) + causeSteps(world, task.copy(cause = trigger.cause))
}
