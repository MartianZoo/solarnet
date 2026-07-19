package dev.martianzoo.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.types.MClassTable

internal class Initializer(
    private val gameplay: Gameplay,
    private val classes: MClassTable,
    private val timeline: TimelineImpl,
) {
  // Taking 14% of total solo game time
  internal fun initialize() {
    var fakeCause: Cause? = null

    fun exec(instruction: String) {
      with(gameplay.godMode()) {
        addTasks("$instruction!", fakeCause).forEach(::doTask) // TODO why ! ?
      }
    }

    exec("$ENGINE")
    fakeCause = Cause(ENGINE.expression, 0)

    classes
        .allClasses()
        .filter { c -> c.isSingletonType() }
        .flatMap { it.baseType.concreteSubtypesSameClass() }
        .forEach { exec("${it.expression}") }

    timeline.initializationFinished()
    exec("SetupPhase")
  }
}
