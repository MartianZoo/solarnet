package dev.martianzoo.tfm.engine

import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Task

/** Whether [task] is the optional second action offered by the ordinary action-phase workflow. */
public fun isActionPhaseSecondAction(game: World, task: Task): Boolean {
  val origin = task.cause ?: return false
  if (origin.context.className != cn("ActionPhase")) return false
  val trigger = game.events.changeAt(origin.triggerEvent)
  return trigger?.change?.gaining?.className == cn("SecondAction")
}
