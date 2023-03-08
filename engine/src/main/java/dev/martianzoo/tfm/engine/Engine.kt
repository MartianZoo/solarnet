package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MType

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
    val game = Game(setup, loader)

    game.execute(
        instruction("Game!"),
        initialCause = Cause(null, contextComponent = GAME.expr, doer = GAME),
        hidden = true).single()
    val fakeCause = Cause(0, contextComponent = GAME.expr, doer = GAME)

    val singletons: List<MType> = singletons(loader.allClasses)
    game.executeAll(
        singletons.map { instruction("${it.expressionFull}!") },
        withEffects = true,
        initialCause = fakeCause,
        hidden = true)
    require(game.pendingAbstractTasks.none())
    return game
  }

  private fun singletons(all: Set<MClass>): List<MType> =
      all.filter { it.hasSingletonTypes() }.flatMap { it.baseType.concreteSubtypesSameClass() }
}
