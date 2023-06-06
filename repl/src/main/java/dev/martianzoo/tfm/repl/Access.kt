package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Gameplay
import dev.martianzoo.tfm.engine.Gameplay.Companion.parse
import dev.martianzoo.tfm.engine.Gameplay.GodMode
import dev.martianzoo.tfm.engine.Gameplay.OperationLayer
import dev.martianzoo.tfm.engine.Gameplay.TaskLayer
import dev.martianzoo.tfm.engine.Gameplay.TurnLayer
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.types.MType

sealed class Access {
  abstract fun exec(instruction: String): TaskResult

  open fun dropTask(id: TaskId): Unit = error("not allowed in this mode")

  // PURPLE: Game integrity: the engine fully controls the workflow
  class PurpleMode(val gameplay: Gameplay) : Access() {
    override fun exec(instruction: String): TaskResult = error("not allowed in this mode")
  }

  // BLUE: Turn integrity: must perform a valid game turn for this phase
  class BlueMode(gameplayIn: Gameplay) : Access() {
    private val gameplay = gameplayIn as TurnLayer

    override fun exec(instruction: String): TaskResult {
      val instr: Instruction = gameplay.parse(instruction)

      // This is weird. We should have special commands... and get rid of this NewTurn2 biz TODO
      return when {
        instr.isGainOf(cn("Phase")) -> gameplay.godMode().manual(instruction)
        instr.isGainOf(cn("NewTurn")) -> gameplay.startTurn()
        instr.isGainOf(cn("NewTurn2")) -> gameplay.startTurn2()
        else -> error("not allowed in this mode")
      }
    }

    private fun Instruction.isGainOf(superclass: ClassName): Boolean =
        when (this) {
          is Change ->
            gaining?.let {
              val t = gameplay.resolve(it.toString()) as MType
              t.isSubtypeOf(gameplay.resolve(superclass.expression.toString()) as MType)
            } ?: false
          is Instruction.Transform -> instruction.isGainOf(superclass)
          else -> false
        }
  }

  // GREEN: Operation integrity: clear task queue before starting new operation
  class GreenMode(gameplayIn: Gameplay) : Access() {
    private val gameplay = gameplayIn as OperationLayer
    override fun exec(instruction: String) = gameplay.beginManual(instruction)
  }

  // YELLOW: Task integrity: changes have consequences
  class YellowMode(gameplayIn: Gameplay) : Access() {
    private val gameplay = gameplayIn as TaskLayer
    override fun dropTask(id: TaskId) { gameplay.dropTask(id) }
    override fun exec(instruction: String) = gameplay.beginManual(instruction)
  }

  // RED: Change integrity: make changes without triggered effects
  class RedMode(gameplayIn: Gameplay) : Access() {
    private val gameplay = gameplayIn as GodMode
    override fun dropTask(id: TaskId) { gameplay.dropTask(id) }
    override fun exec(instruction: String) = gameplay.sneak(instruction)
  }
}
