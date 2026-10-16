package dev.martianzoo.tfm.script

import dev.martianzoo.agent.Agent
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskResult

internal sealed class Access private constructor() {
  internal abstract fun exec(instruction: String): TaskResult

  internal abstract fun phase(phase: String): TaskResult

  /**
   * Puts the game in the named phase ex machina, without that phase's queued start-of-phase rules,
   * since outside purple mode the user, not the game, moves play forward.
   */
  internal fun doPhase(agent: Agent, phase: String): TaskResult {
    val current = agent.reader.getComponents(agent.resolve("Phase")).elements.single().className
    return agent.sneak("${phase}Phase FROM $current")
  }

  internal open fun dropTask(id: TaskId): Unit = error("not allowed in this mode")

  // PURPLE: Game integrity: the game's own rules drive every turn and phase
  internal class PurpleMode : Access() {
    override fun phase(phase: String): TaskResult = error("not allowed in this mode")

    override fun exec(instruction: String): TaskResult = error("not allowed in this mode")
  }

  // BLUE: Turn integrity: must perform a valid game turn for this phase
  internal class BlueMode(private val agent: Agent) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(agent, phase)

    override fun exec(instruction: String): TaskResult = error("not allowed in this mode")
  }

  // GREEN: Operation integrity: clear task queue before starting new operation
  internal class GreenMode(private val agent: Agent) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(agent, phase)

    override fun exec(instruction: String) = agent.beginOperation(instruction)
  }

  // YELLOW: Task integrity: changes have consequences
  internal class YellowMode(private val agent: Agent) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(agent, phase)

    override fun exec(instruction: String) = agent.beginOperation(instruction)

    override fun dropTask(id: TaskId) {
      agent.dropTask(id)
    }
  }

  // RED: Change integrity: make changes without triggered effects
  internal class RedMode(private val agent: Agent) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(agent, phase)

    override fun exec(instruction: String) = agent.sneak(instruction)

    override fun dropTask(id: TaskId) {
      agent.dropTask(id)
    }
  }
}
