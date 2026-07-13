package dev.martianzoo.engine

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
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a new game, initialized for the given [setup], and ready for gameplay to begin. */
  public fun newGame(setup: GameSetup): Game {
    val koin = koinApplication { modules(gameModule(setup)) }.koin

    val game = koin.get<Game>()
    var initializer: Initializer? = null
    val playerComponents =
        setup.players().associateWith { player ->
          val scope = koin.createScope<PlayerScopeId>("$player")
          scope.declare(player)
          if (player == ENGINE) initializer = scope.get<Initializer>()
          scope.get<PlayerComponent>()
        }
    initializer!!.initialize()
    game.playerComponents = playerComponents
    return game
  }

  private class PlayerScopeId

  private fun gameModule(setup: GameSetup) = module {
    single { setup }
    single { MClassLoader(setup) } bind MClassTable::class
    single { Effector(lazy { get<GameReaderImpl>() }) }
    singleOf(::WritableEventLog) {
      bind<EventLog>()
      bind<TaskListener>()
      bind<ChangeLogger>()
    }
    singleOf(::WritableComponentGraph) {
      bind<ComponentGraph>()
      bind<Updater>()
    }
    singleOf(::TaskQueues)
    single<TaskQueue> { get<TaskQueues>().all() }
    singleOf(::Transformers)
    singleOf(::GameReaderImpl) { bind<GameReader>() }
    singleOf(::TimelineImpl) { bind<Timeline>() }
    singleOf(::Limiter)
    singleOf(::Game)

    scope<PlayerScopeId> {
      scoped<WritableTaskQueue> { get<TaskQueues>()[get<Player>()] }
      scoped<TaskQueue> { get<WritableTaskQueue>() }
      scopedOf(::Changer)
      scoped {
        Instructor(get(), get(), get(), get(), get())
      } // Changer? and Effector? are nullable
      scopedOf(::Implementations)
      scoped {
        val game = get<Game>()
        ApiTranslation(get(), get(), get(), get(), get(), get(), get()) { game.onAtomicComplete() }
      } bind Gameplay::class
      scopedOf(::Initializer)
      scopedOf(::PlayerComponent)
    }
  }

  internal data class PlayerComponent(internal val gameplay: Gameplay)

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
