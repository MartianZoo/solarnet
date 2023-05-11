package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
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
    val session = game.session(ENGINE)

    fun gain(thing: HasExpression, cause: Cause? = null): TaskResult {
      val instr: Instruction = parse("${thing.expression}!")
      return session.atomic {
        session.writer.doTask(instr, cause)
        session.tryToDrain()
      }
    }

    val event: ChangeEvent = gain(ENGINE).changes.single()
    val becauseISaidSo = Cause(ENGINE.expression, event.ordinal)

    singletonTypes(table).forEach { gain(it, becauseISaidSo) }
    gain(cn("SetupPhase"), becauseISaidSo)
    game.setupFinished()
    return game
  }

  private fun singletonTypes(table: MClassTable): List<Component> =
      table.allClasses
          .filter { 0 !in it.componentCountRange }
          .flatMap { it.baseType.concreteSubtypesSameClass() }
          .map(Component::ofType)
}
