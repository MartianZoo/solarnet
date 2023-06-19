package dev.martianzoo.engine

import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dev.martianzoo.api.GameReader
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskEditedEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Player
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Task
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.types.MClassLoader
import dev.martianzoo.types.MClassTable
import javax.inject.Scope

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a new game, initialized for the given [setup], and ready for gameplay to begin. */
  public fun newGame(setup: GameSetup): Game {
    val component = DaggerEngine_GameComponent.builder().gameModule(GameModule(setup)).build()

    component.game.playerComponents =
        setup.players().associateWith { p ->
          component.player(PlayerModule(p)).also {
            if (p == ENGINE) it.initter.initialize() // not ideal
          }
        }

    return component.game
  }

  @GameScoped
  @dagger.Component(modules = [GameModule::class])
  internal interface GameComponent {
    val game: Game
    val table: MClassTable
    fun player(module: PlayerModule): PlayerComponent
  }

  @Module
  internal class GameModule(private val setup: GameSetup) {
    @Provides fun setup(): GameSetup = setup
    @Provides fun table(x: MClassLoader): MClassTable = x
    @Provides fun components(x: WritableComponentGraph): ComponentGraph = x
    @Provides fun updater(x: WritableComponentGraph): Updater = x
    @Provides fun tasks(x: WritableTaskQueue): TaskQueue = x
    @Provides fun events(x: WritableEventLog): EventLog = x
    @Provides fun changelog(x: WritableEventLog): ChangeLogger = x
    @Provides fun listener(x: WritableEventLog): TaskListener = x
    @Provides fun reader(x: GameReaderImpl): GameReader = x
    @Provides fun timeline(x: TimelineImpl): Timeline = x
  }

  @Scope internal annotation class GameScoped
  @Scope internal annotation class PlayerScoped

  @PlayerScoped
  @Subcomponent(modules = [PlayerModule::class])
  internal interface PlayerComponent {
    val gameplay: Gameplay
    val initter: Initializer // only used for Engine
  }

  @Module
  internal class PlayerModule(private val player: Player) {
    @Provides fun player(): Player = player
    @Provides fun gameplay(x: ApiTranslation): Gameplay = x
  }

  // Some minor helper interfaces... many classes just need one small part of another class's
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
