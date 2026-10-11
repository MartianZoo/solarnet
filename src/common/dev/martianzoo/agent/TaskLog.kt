package dev.martianzoo.agent

import dev.martianzoo.engine.World
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId

/**
 * Opt-in task text for engine reconstruction, retaining submitted instruction syntax.
 *
 * Captures task submissions, selections, form commits, successful tries, drops, [exMachina], and
 * policy-executed Player tasks. Ordinary lines use `Actor: instruction` for [Agent.doTasks]; `DO`
 * preserves [Agent.doTask] single-task execution, and `CHOOSE` preserves selection or form
 * narrowing that may leave child tasks pending. `DROP` and `EX MACHINA` retain explicit
 * interventions. Failed, no-op, and rolled-back submissions are omitted. Later form commits replace
 * earlier form narrowings of that task when those earlier steps only edited its instruction.
 * Selection steps remain because later narrowing may require the selected Actor. Intermediate event
 * history is not preserved.
 *
 * Reconstruction uses the same premise, implementation and workflow, with Player autoexecution
 * disabled and Admin remaining aggressive. Routine Admin work is omitted; explicit Admin choices
 * are retained. The text does not contain the premise, injected operations, direct `sneak` changes,
 * or external card-name annotations. Task ids and context-only disambiguation are not serialized;
 * imports fail when the instruction cannot identify equivalent pending work.
 */
public class TaskLog internal constructor(private val world: World) {
  private class Submission(
      val line: String,
      val formTask: TaskId?,
      var anchor: GameEvent? = null,
      var onlyNarrows: Boolean = false,
  )

  private var recording = false
  private val submissions = mutableListOf<Submission>()

  /** Starts retaining subsequent submissions. Call before starting the recorded gameplay. */
  public fun start() {
    check(!recording) { "task recording has already started" }
    recording = true
  }

  /** Exports surviving submissions in order, with a trailing newline when nonempty. */
  public fun text(): String {
    val events = world.events.entriesSince(Checkpoint(0))
    val surviving = submissions.filter { item ->
      item.anchor?.let { events.getOrNull(it.ordinal) === it } == true
    }
    val lastFormCommit = surviving.filter { it.formTask != null }.associateBy { it.formTask }
    return surviving
        .filter { !it.onlyNarrows || lastFormCommit[it.formTask] === it }
        .joinToString("") { "${it.line}\n" }
  }

  internal fun <T> capture(
      actor: Actor,
      instruction: String,
      formTask: TaskId? = null,
      block: () -> T,
  ): T {
    if (!recording) return block()
    require('\n' !in instruction && '\r' !in instruction) { "task text must fit on a line" }
    val checkpoint = world.timeline.checkpoint()
    val submission = Submission("$actor: $instruction", formTask)
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

  public companion object {
    /**
     * Applies nonblank lines to their named Actors. The caller first reconstructs the premise,
     * disables Player autoexecution, leaves Admin aggressive, and starts the normal workflow.
     * Selection matches and narrows pending tasks directly; it does not try alternative executions
     * or backtrack. Source task ids and recorded consequences are unused. An invalid, ambiguous, or
     * failed command stops import with its line number; preceding successful lines remain applied.
     */
    public fun replay(text: String, agents: Agents) {
      val actors = agents.world.actors.associateBy { it.toString() }
      text.lineSequence().forEachIndexed { index, line ->
        if (line.isBlank()) return@forEachIndexed
        try {
          val colon = line.indexOf(": ")
          require(colon > 0) { "expected Actor: instruction" }
          val agent = agents[requireNotNull(actors[line.substring(0, colon)]) { "unknown Actor" }]
          val instruction = line.substring(colon + 2)
          when {
            instruction.startsWith("DO ") -> agent.doTask(instruction.removePrefix("DO "))
            instruction.startsWith("CHOOSE ") -> choose(agent, instruction.removePrefix("CHOOSE "))
            instruction.startsWith("EX MACHINA ") ->
                agents.exMachina(agent.actor, instruction.removePrefix("EX MACHINA "))
            instruction.startsWith("DROP ") ->
                agent.dropTask(
                    requireNotNull(matchingTask(agent, instruction.removePrefix("DROP "))).id
                )
            else -> agent.doTasks(instruction)
          }
        } catch (e: Exception) {
          throw IllegalArgumentException("cannot import task line ${index + 1}: $line", e)
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
