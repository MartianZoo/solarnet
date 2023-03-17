package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.data.Actor.Companion.ENGINE
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.MClassLoader

/** Has functions for setting up new games and stuff. */
public object Engine {
  public fun loadClasses(setup: GameSetup): MClassLoader {
    val loader = MClassLoader(setup.authority, autoLoadDependencies = true)

    val classNames =
        listOf(GAME) +
            setup.allDefinitions().classNames() + // all cards etc.
            (1..setup.players).map { cn("Player$it") }

    loader.loadAll(classNames)
    loader.frozen = true
    return loader
  }

  public fun newGame(setup: GameSetup): Game {
    val loader = loadClasses(setup)

    // TODO get @createSingletons to work as a real CustomInstruction
    // setup.authority.customInstructions += customInstr(loader)

    val game = Game(setup, loader)

    val result: Result = game.forActor(ENGINE).initiate(instruction("Game!"))
    require(result.newTaskIdsAdded.none())
    require(game.taskQueue.isEmpty())

    val firstEvent = result.changes.single()
    val fakeCause = Cause(GAME.expr, firstEvent.ordinal)

    singletonCreateInstructions(loader).forEach {
      game.forActor(ENGINE).initiate(it, fakeCause)
      require(game.taskQueue.isEmpty()) { "Unexpected tasks: ${game.taskQueue}" }
    }
    return game
  }

  fun singletonCreateInstructions(loader: MClassLoader): List<Instruction> {
    val singletonTypes =
        loader.allClasses
            .filter { it.hasSingletonTypes() }
            .flatMap { it.baseType.concreteSubtypesSameClass() }
    return singletonTypes.map { instruction("${it.expressionFull}!") }
  }
}
