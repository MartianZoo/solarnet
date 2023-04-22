package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.ENGINE
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.MClassLoader

/** Has functions for setting up new games and stuff. */
public object Engine {
  val loaderCache = mutableMapOf<GameSetup, MClassLoader>()

  public fun loadClasses(setup: GameSetup): MClassLoader {
    // if (setup in loaderCache) return loaderCache[setup]!!

    val loader = MClassLoader(setup.authority, autoLoadDependencies = true)
    val toLoad: List<HasClassName> = setup.allDefinitions() + setup.players()

    loader.loadAll(toLoad.classNames())

    if ("P" in setup.bundles) loader.load(cn("PreludePhase"))

    loader.frozen = true
    // loaderCache[setup] = loader
    return loader
  }

  public fun newGame(setup: GameSetup): Game {
    val loader = loadClasses(setup)
    val game = Game(setup, loader)
    val agent = game.asPlayer(Player.ENGINE)

    val result: Result = agent.initiate(Gain.gain(scaledEx(1, ENGINE)))
    require(result.newTaskIdsAdded.none())
    require(game.taskQueue.isEmpty())

    val fakeCause = Cause(Player.ENGINE, result.changes.first())

    singletonCreateInstructions(loader).forEach {
      agent.initiate(it, fakeCause)
      require(game.taskQueue.isEmpty()) { "Unexpected tasks: ${game.taskQueue}" }
    }
    game.setupFinished()
    return game
  }

  private fun singletonCreateInstructions(loader: MClassLoader): List<Instruction> =
      loader.allClasses
          .filter { 0 !in it.componentCountRange }
          .flatMap { it.baseType.concreteSubtypesSameClass() }
          .map { Gain.gain(scaledEx(1, it)) }
}
