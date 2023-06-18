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
    private val gameplay: Gameplay,
    private val table: MClassTable,
    private val timeline: TimelineImpl,
    private val setup: GameSetup,
    private val tasks: TaskQueue,
) {
  fun initialize() {
    var fakeCause: Cause? = null

    fun exec(instruction: String) {
      with(gameplay.godMode()) {
        addTasks("$instruction!", fakeCause) // TODO why ! ?
        do {
          doTask(tasks.ids().first())
        } while (tasks.ids().any())
      }
    }

    exec("$ENGINE")
    fakeCause = Cause(ENGINE.expression, 0)

    table.allClasses()
        .filter { c -> c.isSingletonType() }
        .flatMap { it.baseType.concreteSubtypesSameClass() }
        .forEach { exec("${it.expression}") }

    // Colonies-specific setup... TODO where does this really belong?
    if ("C" in setup.bundles) {
      setup.colonyTiles.forEach { exec("AddColonyTile<Class<${it.className}>>") }

      var letter = "A"
      (setup.players() - ENGINE).forEach {
        exec("TradeFleet$letter<$it>")
        letter = "${letter[0] + 1}"
      }
    }
    timeline.initializationFinished()
    exec("SetupPhase")
  }
}
