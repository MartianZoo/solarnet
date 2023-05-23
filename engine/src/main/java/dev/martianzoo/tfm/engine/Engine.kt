package dev.martianzoo.tfm.engine

import dagger.Component
import dagger.Module
import dagger.Provides
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.engine.Game.EventLog
import dev.martianzoo.tfm.engine.Game.SnReader
import dev.martianzoo.tfm.engine.Game.TaskQueue
import dev.martianzoo.tfm.engine.Game.Timeline
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.types.MClassTable
import javax.inject.Singleton

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a new game, initialized for the given [setup], and ready for gameplay to begin. */
  public fun newGame(setup: GameSetup) = newGame(MClassTable.forSetup(setup))

  /** Creates a new game using an existing class table, ready for gameplay to begin. */
  public fun newGame(table: MClassTable): Game {
    val game =
        DaggerEngine_GameComponent.builder()
            .gameModule(GameModule(table))
            .build()
            .game

    val session = game.session(ENGINE)

    val firstEvent: ChangeEvent = session.operation("$ENGINE!").changes.first()
    val fakeCause = Cause(ENGINE.expression, firstEvent.ordinal)

    table.singletons.forEach { session.initiateOnly(Parsing.parse("${it.expression}!"), fakeCause) }
    session.autoExec(false)
    (game.timeline as TimelineImpl).setupFinished()

    session.operation("CorporationPhase FROM Phase")
    return game
  }

  @Singleton
  @Component(modules = [GameModule::class])
  internal abstract class GameComponent {
    abstract val game: Game
  }

  @Module
  internal class GameModule(val loader: MClassTable) {
    @Provides fun a(x: GameReaderImpl): SnReader = x
    @Provides fun c(): MClassTable = loader
    @Provides fun d(x: TimelineImpl): Timeline = x
    @Provides fun e(x: WritableComponentGraph): ComponentGraph = x
    @Provides fun f(x: WritableComponentGraph): Updater = x
    @Provides fun g(x: WritableEventLog): ChangeLogger = x
    @Provides fun h(x: WritableEventLog): EventLog = x
    @Provides fun i(x: WritableEventLog): TaskListener = x
    @Provides fun j(x: WritableTaskQueue): TaskQueue = x
  }
}
