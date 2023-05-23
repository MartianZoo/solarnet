package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.engine.Game.SnReader
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.WritableComponentGraph.Limiter
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MClassTable

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a new game, initialized for the given [setup], and ready for gameplay to begin. */
  public fun newGame(setup: GameSetup) = newGame(MClassTable.forSetup(setup))

  /** Creates a new game using an existing class table, ready for gameplay to begin. */
  public fun newGame(table: MClassTable): Game {
    val game = wireItUp(table)
    val session = game.session(ENGINE)

    val firstEvent: ChangeEvent = session.operation("$ENGINE!").changes.first()
    val fakeCause = Cause(ENGINE.expression, firstEvent.ordinal)

    table.singletons.forEach { session.initiateOnly(Parsing.parse("${it.expression}!"), fakeCause) }
    session.autoExec(false)
    (game.timeline as TimelineImpl).setupFinished()

    session.operation("CorporationPhase FROM Phase")
    return game
  }

  /** Yes, we're doing all our ugly-as-sin wiring in one place. */
  private fun wireItUp(table: MClassTable): Game {
    val effector = Effector()
    val components: ComponentGraph = WritableComponentGraph(effector)

    val reader: SnReader = GameReaderImpl(table, components)
    effector.reader = reader

    val events = WritableEventLog()
    val tasks = WritableTaskQueue(events)

    val limiter = Limiter(table as MClassLoader, components)

    val updater: Updater = components as Updater
    val timeline = TimelineImpl(updater, events, tasks, reader)

    val players = table.allClasses.count { it.className.toString().matches(Regex("Player\\d")) }

    val writers: Map<Player, GameWriter> =
        Player.players(players)
            .filter { it.className in table.allClassNamesAndIds }
            .associateWith {
              val changer = Changer(reader, updater, events as ChangeLogger, it)
              val instructor = Instructor(reader, effector, limiter, changer)
              GameWriterImpl(tasks, reader, timeline, it, instructor, changer)
            }

    return Game(components, events, tasks, reader, timeline, writers)
  }
}
