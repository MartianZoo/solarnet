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
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MClassTable

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a new game, initialized for the given [setup], and ready for gameplay to begin. */
  public fun newGame(setup: GameSetup): Game {
    val game = DaggerGame.builder().gameModule(GameModule(setup)).build()
    game.writer(ENGINE) // TODO hack - this triggers initialization
    return game
  }

  // Internal wiring stuff

  @Module
  internal class GameModule(private val setup: GameSetup) {
    @Provides fun setup(): GameSetup = setup

    // TODO absolutely stupid hack
    internal val table: MClassTable by lazy { MClassLoader(setup) }
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
    internal abstract val writer: Tasker
    internal abstract val initter: Initializer // only used for Engine
  }

  @Module
  internal class PlayerModule(private val player: Player) {
    @Provides fun a(): Player = player
    @Provides fun b(x: PlayerAgent): Tasker = x
    @Provides fun c(x: PlayerAgent): UnsafeGameWriter = x
  }

  public fun writers(game: Game, setup: GameSetup) =
      setup.players().associateWith {
        val module = game.playerModule(PlayerModule(it))
        if (it == ENGINE) module.initter.initialize()
        module.writer
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
