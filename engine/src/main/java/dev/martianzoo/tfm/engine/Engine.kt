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
import javax.inject.Scope
import javax.inject.Singleton

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

  @Singleton
  @dagger.Component(modules = [GameModule::class])
  internal interface GameComponent {
    val game: Game
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
  }

  @Scope
  annotation class PlayerScope

  @PlayerScope
  @Subcomponent(modules = [PlayerModule::class])
  internal interface PlayerComponent {
    val writer: GameWriter // TODO phase out?
    val gameplay: Gameplay
    val initter: Initializer // only used for Engine
  }

  @Module
  internal class PlayerModule(private val player: Player) {
    @Provides fun player(): Player = player
    @Provides fun writer(x: PlayerAgent): GameWriter = x

    @Provides fun gamesLayer(x: ApiTranslation): Gameplay = x
    @Provides fun turnsLayer(x: ApiTranslation): Gameplay.TurnLayer = x
    @Provides fun operationsLayer(x: ApiTranslation): Gameplay.OperationLayer = x
    @Provides fun tasksLayer(x: ApiTranslation): Gameplay.TaskLayer = x
    @Provides fun changesLayer(x: ApiTranslation): Gameplay.ChangeLayer = x
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
