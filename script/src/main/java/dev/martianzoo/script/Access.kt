package dev.martianzoo.script

import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Gameplay
import dev.martianzoo.engine.Gameplay.GodMode
import dev.martianzoo.engine.Gameplay.OperationLayer
import dev.martianzoo.engine.Gameplay.TaskLayer
import dev.martianzoo.engine.Gameplay.TurnLayer

internal sealed class Access {
  public abstract fun exec(instruction: String): TaskResult

  public abstract fun newTurn(): TaskResult

  public abstract fun phase(phase: String): TaskResult

  internal fun doPhase(gameplay: OperationLayer, phase: String): TaskResult =
      gameplay.beginManual("${phase}Phase FROM Phase")

  public open fun dropTask(id: TaskId): Unit = error("not allowed in this mode")

  // PURPLE: Game integrity: the engine fully controls the workflow
  internal class PurpleMode(private val gameplay: Gameplay) : Access() {
    override fun phase(phase: String): TaskResult = error("not allowed in this mode")

    override fun newTurn(): TaskResult = error("not allowed in this mode")

    override fun exec(instruction: String): TaskResult = error("not allowed in this mode")
  }

  // BLUE: Turn integrity: must perform a valid game turn for this phase
  internal class BlueMode(gameplayIn: Gameplay) : Access() {
    private val gameplay = gameplayIn as TurnLayer

    override fun phase(phase: String): TaskResult = doPhase(gameplay as OperationLayer, phase)

    override fun newTurn() = gameplay.startTurn()

    override fun exec(instruction: String): TaskResult = error("not allowed in this mode")
  }

  // GREEN: Operation integrity: clear task queue before starting new operation
  internal class GreenMode(gameplayIn: Gameplay) : Access() {
    private val gameplay = gameplayIn as OperationLayer

    override fun phase(phase: String): TaskResult = doPhase(gameplay, phase)

    override fun newTurn() = gameplay.startTurn()

    override fun exec(instruction: String) = gameplay.beginManual(instruction)
  }

  // YELLOW: Task integrity: changes have consequences
  internal class YellowMode(gameplayIn: Gameplay) : Access() {
    private val gameplay = gameplayIn as TaskLayer

    override fun phase(phase: String): TaskResult = doPhase(gameplay, phase)

    override fun newTurn() = gameplay.startTurn()

    override fun exec(instruction: String) = gameplay.beginManual(instruction)

    override fun dropTask(id: TaskId) {
      gameplay.dropTask(id)
    }
  }

  // RED: Change integrity: make changes without triggered effects
  internal class RedMode(gameplayIn: Gameplay) : Access() {
    private val gameplay = gameplayIn as GodMode

    override fun phase(phase: String): TaskResult = doPhase(gameplay, phase)

    override fun newTurn() = gameplay.startTurn()

    override fun exec(instruction: String) = gameplay.sneak(instruction)

    override fun dropTask(id: TaskId) {
      gameplay.dropTask(id)
    }
  }
}
