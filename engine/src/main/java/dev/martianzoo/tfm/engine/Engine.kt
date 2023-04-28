package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Result
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MClassTable

/** Has functions for setting up new games and stuff. */
public object Engine {
  private val classTableCache = mutableMapOf<GameSetup, MClassTable>()
  private val gameTemplateCache = mutableMapOf<GameSetup, Game>()

  public fun loadClasses(setup: GameSetup): MClassTable {
    if (setup in classTableCache) return classTableCache[setup]!!

    val loader = MClassLoader(setup.authority)
    val toLoad: List<HasClassName> = setup.allDefinitions() + setup.players()

    loader.loadAll(toLoad.classNames())

    if ("P" in setup.bundles) loader.load(cn("PreludePhase"))

    return loader.freeze().also { classTableCache[setup] = it }
  }

  public fun newGame(setup: GameSetup): Game {
    return newGame(setup, loadClasses(setup))
  }

  public fun newGame(setup: GameSetup, table: MClassTable): Game {
    if (setup in gameTemplateCache) return gameTemplateCache[setup]!!.clone()
    val game = Game(table)
    val agent = game.asPlayer(ENGINE)

    val result: Result = agent.initiate(Gain.gain(scaledEx(1, ENGINE)))
    require(game.tasks.isEmpty())

    val fakeCause = Cause(ENGINE.expression, result.changes.first().ordinal)

    singletonCreateInstructions(table).forEach {
      agent.initiate(it, fakeCause)
      require(game.tasks.isEmpty()) { "Unexpected tasks: ${game.tasks}" }
    }
    game.setupFinished()
    gameTemplateCache[setup] = game.clone()
    return game
  }

  private fun singletonCreateInstructions(table: MClassTable): List<Instruction> =
      table.allClasses
          .filter { 0 !in it.componentCountRange }
          .flatMap { it.baseType.concreteSubtypesSameClass() }
          .map { Gain.gain(scaledEx(1, it)) }
}
