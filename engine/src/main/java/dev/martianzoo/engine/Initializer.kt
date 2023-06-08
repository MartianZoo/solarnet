package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.engine.Engine.PlayerScoped
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.types.MClassTable
import javax.inject.Inject

@PlayerScoped
internal class Initializer
@Inject
constructor(val writer: GameWriter, val table: MClassTable, val timeline: Timeline) {
  fun initialize() {
    var fakeCause: Cause? = null

    fun exec(instruction: String) = writer.executeFully(parse(instruction), fakeCause)

    exec("$ENGINE!")
    fakeCause = Cause(ENGINE.expression, 0)

    table.allClasses
        .filter { 0 !in it.componentCountRange }
        .flatMap { it.baseType.concreteSubtypesSameClass() }
        .forEach { exec("${it.expression}!") }

    timeline.setupFinished()
  }
}
