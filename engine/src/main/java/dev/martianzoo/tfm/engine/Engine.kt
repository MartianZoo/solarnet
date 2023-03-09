package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
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
    // game.execute(instruction("Game!"), withEffects = true, hidden = true)

    game.execute(
        instruction("Game!"),
        initialCause = Cause(null, contextComponent = GAME.expr, doer = GAME),
        hidden = true).single()
    val fakeCause = Cause(0, contextComponent = GAME.expr, doer = GAME)

    game.executeAll(
        Instruction.split(customInstr(loader)),
        withEffects = true,
        initialCause = fakeCause,
        hidden = true)
    require(game.pendingAbstractTasks.none())
    return game
  }

  fun customInstr(loader: MClassLoader): Instruction {
      val singletonTypes = loader.allClasses
          .filter { it.hasSingletonTypes() }
          .flatMap { it.baseType.concreteSubtypesSameClass() }
      return Multi.create(singletonTypes.map { instruction("${it.expressionFull}!") })!!
      //
      // return object : CustomInstruction("createSingletons") {
      //   override fun translate(game: GameStateReader, arguments: List<Type>) = instr
      // }
  }
}
