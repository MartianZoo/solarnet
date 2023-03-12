package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.LogEntry.ChangeEvent.Cause
import dev.martianzoo.tfm.engine.SingleExecution.ExecutionResult
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
    // TODO game -> engine
    val result: ExecutionResult = game.initiate(instruction("Game!"), Actor.ENGINE)
    require(result.fullSuccess) { result }
    require(result.newTaskIdsAdded.none())
    val firstEvent = result.changes.single()
    val fakeCause = Cause(GAME.expr, firstEvent.ordinal, Actor.ENGINE)

    customInstr(loader).forEach {
      val result = game.initiate(it, Actor.ENGINE, fakeCause)
      require(result.fullSuccess) { result }
      require(game.taskQueue.isEmpty()) { "Something was left in the task queue" }
    }
    return game
  }

  fun customInstr(loader: MClassLoader): List<Instruction> {
    val singletonTypes = loader.allClasses
        .filter { it.hasSingletonTypes() }
        .flatMap { it.baseType.concreteSubtypesSameClass() }
    return singletonTypes.map { instruction("${it.expressionFull}!") }
    //
    // return object : CustomInstruction("createSingletons") {
    //   override fun translate(game: GameStateReader, arguments: List<Type>) = instr
    // }
  }
}
