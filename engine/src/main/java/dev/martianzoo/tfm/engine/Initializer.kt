package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.types.MClassTable
import javax.inject.Inject

internal class Initializer @Inject constructor(
    val unsafe: UnsafeGameWriter,
    val table: MClassTable,
    val timeline: Timeline
) {
  fun initialize() {
    var fakeCause: Cause? = null

    fun exec(instr: String) = unsafe.executeFully(Parsing.parse(instr), fakeCause)

    exec("${Player.ENGINE}!")
    fakeCause = Cause(Player.ENGINE.expression, 0)

    table.allClasses
        .filter { 0 !in it.componentCountRange }
        .flatMap { it.baseType.concreteSubtypesSameClass() }
        .forEach { exec("${it.expression}!") }

    timeline.setupFinished()

    exec("CorporationPhase FROM Phase")
  }
}
