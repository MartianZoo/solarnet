package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MClassTable

/** Has functions for setting up new games and stuff. */
public object Engine {
  private val classTableCache = mutableMapOf<GameSetup, MClassTable>()

  public fun loadClasses(setup: GameSetup): MClassTable {
    if (setup in classTableCache) return classTableCache[setup]!!

    val loader = MClassLoader(setup.authority)
    val toLoad: List<HasClassName> = setup.allDefinitions() + setup.players()

    loader.loadAll(toLoad.classNames())

    if ("P" in setup.bundles) loader.load(cn("PreludePhase"))

    return loader.freeze().also { classTableCache[setup] = it }
  }

  public fun newGame(setup: GameSetup) = newGame(loadClasses(setup))

  public fun newGame(table: MClassTable): Game {
    val game = Game(table)
    val writer = game.writer(ENGINE)

    fun gain(thing: HasExpression, cause: Cause? = null) =
        writer.session().action(Gain.gain(scaledEx(1, thing)))

    val event: ChangeEvent = gain(ENGINE).changes.first()
    val becauseISaidSo = Cause(ENGINE.expression, event.ordinal)

    singletonTypes(table).forEach { gain(it, becauseISaidSo) }
    gain(cn("SetupPhase"), becauseISaidSo)

    // game.writer(ENGINE).session().tryToDrain() // TODO huh?
    game.setupFinished()
    return game
  }

  private fun singletonTypes(table: MClassTable): List<Component> =
      table.allClasses
          .filter { 0 !in it.componentCountRange }
          .flatMap { it.baseType.concreteSubtypesSameClass() }
          .map(Component::ofType)
}
