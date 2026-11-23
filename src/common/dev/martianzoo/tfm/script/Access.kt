package dev.martianzoo.tfm.script

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskResult

internal sealed class Access private constructor() {
  internal abstract fun exec(instruction: String): TaskResult

  internal abstract fun phase(phase: String): TaskResult

  /**
   * Puts the game in the named phase ex machina, without that phase's queued start-of-phase rules,
   * since outside purple mode the user, not the game, moves play forward.
   */
  internal fun doPhase(repl: ScriptSession, phase: String): TaskResult {
    val current =
        repl.game.reader
            .getComponents(repl.game.reader.resolve(Parsing.parse("Phase")))
            .elements
            .single()
            .className
    return repl.agents.exMachina(
        InstructionGroup.of(Parsing.parse<InstructionTree>("${phase}Phase FROM $current!"))
    )
  }

  internal fun execute(repl: ScriptSession, instruction: String): TaskResult =
      if (repl.currentActor is Player) repl.agent.beginOperation(instruction)
      else
          repl.agents.transact {
            check(repl.game.tasks.isEmpty()) {
              "Finish pending tasks before starting another operation"
            }
            repl.game
                .actorEngine(repl.currentActor)
                .addTasks(InstructionGroup.of(repl.parse<InstructionTree>(instruction)))
          }

  internal open fun dropTask(id: TaskId): Unit = error("not allowed in this mode")

  // PURPLE: Game integrity: the game's own rules drive every turn and phase
  internal class PurpleMode : Access() {
    override fun phase(phase: String): TaskResult = error("not allowed in this mode")

    override fun exec(instruction: String): TaskResult = error("not allowed in this mode")
  }

  // BLUE: Turn integrity: must perform a valid game turn for this phase
  internal class BlueMode(private val repl: ScriptSession) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(repl, phase)

    override fun exec(instruction: String): TaskResult = error("not allowed in this mode")
  }

  // GREEN: Operation integrity: clear task queue before starting new operation
  internal class GreenMode(private val repl: ScriptSession) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(repl, phase)

    override fun exec(instruction: String) = execute(repl, instruction)
  }

  // YELLOW: Task integrity: changes have consequences
  internal class YellowMode(private val repl: ScriptSession) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(repl, phase)

    override fun exec(instruction: String) = execute(repl, instruction)

    override fun dropTask(id: TaskId) {
      repl.agents.transact { repl.game.actorEngine(repl.currentActor).dropTask(id) }
    }
  }

  // RED: Change integrity: make changes without triggered effects
  internal class RedMode(private val repl: ScriptSession) : Access() {

    override fun phase(phase: String): TaskResult = doPhase(repl, phase)

    override fun exec(instruction: String) =
        if (repl.currentActor is Player) repl.agent.exMachina(instruction)
        else repl.agents.exMachina(InstructionGroup.of(repl.parse<InstructionTree>(instruction)))

    override fun dropTask(id: TaskId) {
      repl.agents.transact { repl.game.actorEngine(repl.currentActor).dropTask(id) }
    }
  }
}
