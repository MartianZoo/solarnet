package dev.martianzoo.script

import dev.martianzoo.engine.Agent
import dev.martianzoo.pets.data.Task.TaskId
import dev.martianzoo.pets.data.TaskResult

internal sealed class Access {
  internal abstract fun exec(instruction: String): TaskResult

  internal abstract fun newTurn(): TaskResult

  internal abstract fun phase(phase: String): TaskResult

  internal fun doPhase(agent: Agent, phase: String): TaskResult =
      agent.beginManual("${phase}Phase FROM Phase")

  internal open fun dropTask(id: TaskId): Unit = error("not allowed in this mode")

  // PURPLE: Game integrity: the engine fully controls the workflow
  internal class PurpleMode : Access() {
    override fun phase(phase: String): TaskResult = error("not allowed in this mode")

    override fun newTurn(): TaskResult = error("not allowed in this mode")

    override fun exec(instruction: String): TaskResult = error("not allowed in this mode")
  }

  // BLUE: Turn integrity: must perform a valid game turn for this phase
  internal class BlueMode(private val agent: Agent) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(agent, phase)

    override fun newTurn() = agent.startTurn()

    override fun exec(instruction: String): TaskResult = error("not allowed in this mode")
  }

  // GREEN: Operation integrity: clear task queue before starting new operation
  internal class GreenMode(private val agent: Agent) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(agent, phase)

    override fun newTurn() = agent.startTurn()

    override fun exec(instruction: String) = agent.beginManual(instruction)
  }

  // YELLOW: Task integrity: changes have consequences
  internal class YellowMode(private val agent: Agent) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(agent, phase)

    override fun newTurn() = agent.startTurn()

    override fun exec(instruction: String) = agent.beginManual(instruction)

    override fun dropTask(id: TaskId) {
      agent.dropTask(id)
    }
  }

  // RED: Change integrity: make changes without triggered effects
  internal class RedMode(private val agent: Agent) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(agent, phase)

    override fun newTurn() = agent.startTurn()

    override fun exec(instruction: String) = agent.sneak(instruction)

    override fun dropTask(id: TaskId) {
      agent.dropTask(id)
    }
  }
}
