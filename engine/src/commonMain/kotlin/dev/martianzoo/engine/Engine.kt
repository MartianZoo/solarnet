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
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.Task
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.types.ClassLoader
import dev.martianzoo.types.ClassTable
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a game at its committed pre-setup baseline, ready to be given to a workflow. */
  public fun newGame(premise: GamePremise): World {
    return newWorld(premise)
  }

  /**
   * Validates a quiescent [setupWorld], snapshots it through [assemble], and creates a playable
   * game.
   */
  public fun newGame(
      setupWorld: World,
      assemble: (GameReader) -> GamePremise,
  ): World {
    require(setupWorld.isIdle()) { "a completed setup world must be idle" }
    setupWorld.gameplay(ENGINE).godMode().manual("ValidateSetup")
    check(setupWorld.isIdle()) { "setup validation did not leave the world idle" }
    return newGame(assemble(setupWorld.reader))
  }

  /** Creates a standalone setup world and resolves its choice-free initialization tasks. */
  public fun newSetupWorld(premise: GamePremise): World =
      newWorld(premise).also {
        with(it.gameplay(ENGINE)) {
          autoExecMode = SAFE
          autoExecNow()
        }
      }

  private fun newWorld(premise: GamePremise): WholeWorld {
    val koin = koinApplication { modules(gameModule(premise)) }.koin

    val game = koin.get<WholeWorld>()
    var initializer: Initializer? = null
    val gameplayByActor =
        premise.actors.associateWith { actor ->
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

  private fun gameModule(premise: GamePremise) = module {
    single { premise }
    single { loadClassTable(premise) } bind ClassTable::class
    singleOf(::Transformers)
    single { Effector(get(), lazy { get<GameReaderImpl>() }) }
    single { WritableEventLog() }
    single<EventLog> { get<WritableEventLog>() }
    single<TaskListener> { get<WritableEventLog>() }
    single<ChangeLogger> { get<WritableEventLog>() }
    single<WritableComponentGraph> { WritableComponentGraph.Whole(get(), get()) }
    single<ComponentGraph> { get<WritableComponentGraph>() }
    single<Updater> { get<WritableComponentGraph>() }
    singleOf(::TaskQueues)
    single<TaskQueue> { get<TaskQueues>().all() }
    singleOf(::GameReaderImpl) { bind<GameReader>() }
    singleOf(::TimelineImpl) { bind<Timeline>() }
    singleOf(::Limiter)
    singleOf(::WholeWorld) { bind<World>() }

    scope<ActorScopeId> {
      scoped<WritableTaskQueue> { get<TaskQueues>()[get<Actor>()] }
      scoped<TaskQueue> { get<WritableTaskQueue>() }
      scopedOf(::Changer)
      scoped {
        Instructor(get(), get(), get(), get(), get())
      } // Changer? and Effector? are nullable
      scopedOf(::Implementations)
      scoped {
        val world = get<World>()
        ApiTranslation(get(), get(), get(), get(), get(), get(), get()) { world.onAtomicComplete() }
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

internal fun loadClassTable(premise: GamePremise): ClassTable {
  val ruleset = premise.ruleset

  fun isAutoLoad(declaration: ClassDeclaration): Boolean =
      declaration.className == AUTO_LOAD ||
          declaration.supertypes.any {
            isAutoLoad(ruleset.classDeclaration(it.className))
          }

  val rootClassNames =
      premise.actors.classNames() +
          premise.rootClassNames +
          ruleset.allClassDeclarations.filterValues(::isAutoLoad).keys

  return ClassLoader(ruleset).apply { rootClassNames.forEach(::load) }.freeze()
}
