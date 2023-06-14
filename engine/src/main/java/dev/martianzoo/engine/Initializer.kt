package dev.martianzoo.engine

import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.engine.Engine.PlayerScoped
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.types.MClassTable
import javax.inject.Inject

@PlayerScoped
internal class Initializer
@Inject
constructor(
    val gameplay: Gameplay,
    val table: MClassTable,
    val timeline: Timeline,
    val setup: GameSetup,
    val tasks: TaskQueue,
) {
  fun initialize() {
    var fakeCause: Cause? = null

    fun exec(instruction: String) {
      with(gameplay.godMode()) {
        addTasks(instruction, fakeCause)
        do {
          doTask(tasks.ids().first())
        } while (tasks.ids().any())
      }
    }

    exec("$ENGINE!")
    fakeCause = Cause(ENGINE.expression, 0)

    table.allClasses
        .filter { c -> c.singleton }
        .flatMap { it.baseType.concreteSubtypesSameClass() }
        .forEach { exec("${it.expression}!") }

    // Colonies-specific setup... TODO where does this really belong?
    if ("C" in setup.bundles) {
      setup.colonyTiles.forEach {
        if (it.resourceType == null) {
          exec("${it.className}!")
        } else {
          exec("DelayedColonyTile<Class<${it.className}>, Class<${it.resourceType}>>!")
        }
      }

      var letter = "A"
      (setup.players() - ENGINE).forEach {
        exec("TradeFleet$letter<$it>!")
        letter = "${letter[0] + 1}"
      }
    }
    timeline.setupFinished()
  }
}
