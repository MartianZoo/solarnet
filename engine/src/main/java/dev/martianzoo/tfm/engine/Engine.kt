package dev.martianzoo.tfm.engine

import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEditedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.engine.Game.EventLog
import dev.martianzoo.tfm.engine.Game.TaskQueue
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.types.MClassTable

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a new game, initialized for the given [setup], and ready for gameplay to begin. */
  public fun newGame(setup: GameSetup) = newGame(MClassTable.forSetup(setup))

  /** Creates a new game using an existing class table, ready for gameplay to begin. */
  public fun newGame(table: MClassTable): Game {
    val game = Game.createEmpty(table)

    val session = PlayerSession(game, ENGINE)
    val firstEvent: ChangeEvent = session.operation("$ENGINE!").changes.first()
    val fakeCause = Cause(ENGINE.expression, firstEvent.ordinal)

    table.singletons.forEach { session.initiateOnly(parse("${it.expression}!"), fakeCause) }
    session.autoExec(safely = false)
    game.timeline.setupFinished()

    session.operation("CorporationPhase FROM Phase")
    return game
  }

  // Internal wiring stuff

  @Module
  internal class GameModule(private val table: MClassTable) {
    @Provides fun table(): MClassTable = table
    @Provides fun components(x: WritableComponentGraph): ComponentGraph = x
    @Provides fun updater(x: WritableComponentGraph): Updater = x
    @Provides fun tasks(x: WritableTaskQueue): TaskQueue = x
    @Provides fun events(x: WritableEventLog): EventLog = x
    @Provides fun changelog(x: WritableEventLog): ChangeLogger = x
    @Provides fun listener(x: WritableEventLog): TaskListener = x
    @Provides fun reader(x: GameReaderImpl): GameReader = x
  }

  @Subcomponent(modules = [PlayerModule::class])
  internal abstract class PlayerComponent {
    abstract val writer: GameWriter
  }

  @Module
  internal class PlayerModule(private val player: Player) {
    @Provides fun b(): Player = player
    @Provides fun a(x: GameWriterImpl): GameWriter = x
  }

  // Some minor helper interfaces... may classes just need one small part of another class's
  // functionality, and I wanted to try to expose less.

  internal interface ChangeLogger {
    fun addChangeEvent(change: StateChange, player: Player, cause: Cause?): ChangeEvent
  }

  internal interface TaskListener {
    fun taskAdded(task: Task): TaskAddedEvent
    fun taskRemoved(task: Task): TaskRemovedEvent
    fun taskReplaced(oldTask: Task, newTask: Task): TaskEditedEvent
  }

  internal interface Updater {
    fun update(count: Int, gaining: Component?, removing: Component?): StateChange
  }
}
