package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.AUTO_LOAD
import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskEditedEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Task
import dev.martianzoo.pets.HasClassName.Companion.classNames
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

  /** Creates a game at its committed pre-setup baseline, ready to be given to a workflow. */
  public fun newGame(setup: GameSetup): Game {
    val koin = koinApplication { modules(gameModule(setup)) }.koin

    val game = koin.get<WholeGameState>()
    var initializer: Initializer? = null
    val gameplayByActor =
        setup.actors().associateWith { actor ->
          val scope = koin.createScope<ActorScopeId>("$actor")
          scope.declare(actor)
          if (actor == ENGINE) initializer = scope.get<Initializer>()
          scope.get<Gameplay>()
        }
    game.initializeGameplay(gameplayByActor)
    initializer!!.initialize()
    return game
  }

  private class ActorScopeId

  private fun gameModule(setup: GameSetup) = module {
    single { setup }
    single { loadClassTable(setup) } bind MClassTable::class
    singleOf(::Transformers)
    single { Effector(get(), lazy { get<GameReaderImpl>() }) }
    single { WritableEventLog() }
    single<EventLog> { get<WritableEventLog>() }
    single<TaskListener> { get<WritableEventLog>() }
    single<ChangeLogger> { get<WritableEventLog>() }
    single<WritableComponentGraph> { WritableComponentGraph.Whole(get()) }
    single<ComponentGraph> { get<WritableComponentGraph>() }
    single<Updater> { get<WritableComponentGraph>() }
    singleOf(::TaskQueues)
    single<TaskQueue> { get<TaskQueues>().all() }
    singleOf(::GameReaderImpl) { bind<GameReader>() }
    singleOf(::TimelineImpl) { bind<Timeline>() }
    singleOf(::Limiter)
    singleOf(::WholeGameState) { bind<Game>() }

    scope<ActorScopeId> {
      scoped<WritableTaskQueue> { get<TaskQueues>()[get<Actor>()] }
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
    }
  }

  internal interface ChangeLogger {
    fun addChangeEvent(change: StateChange, actor: Actor, cause: Cause?): ChangeEvent
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

internal fun loadClassTable(setup: GameSetup): MClassTable {
  val ruleset = setup.ruleset

  fun isAutoLoad(declaration: ClassDeclaration): Boolean =
      declaration.className == AUTO_LOAD ||
          declaration.supertypes.any {
            isAutoLoad(ruleset.classDeclaration(it.className))
          }

  val rootClassNames =
      setup.actors().classNames() +
          setup.options.enabled +
          ruleset.allClassDeclarations.filterValues(::isAutoLoad).keys +
          ruleset.allDefinitions.classNames()

  return MClassLoader(ruleset).apply { rootClassNames.forEach(::load) }.freeze()
}
