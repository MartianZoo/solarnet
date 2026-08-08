package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GamePremise
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.types.ClassTable

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a game at its committed pre-setup baseline, ready to be given to a workflow. */
  public fun newGame(
      premise: GamePremise,
      locale: String = Vocabulary.ENGLISH,
      inputOnlySynonyms: Iterable<Pair<String, String>> = emptyList(),
  ): World {
    return newWorld(premise, locale, inputOnlySynonyms)
  }

  /**
   * Validates a quiescent [setupWorld], snapshots it through [assemble], and creates a playable
   * game.
   */
  public fun newGame(
      setupWorld: World,
      assemble: (GameReader) -> GamePremise,
      locale: String = setupWorld.vocabulary.locale,
      inputOnlySynonyms: Iterable<Pair<String, String>> =
          setupWorld.vocabulary.inputOnlySynonyms.map { (synonym, canonical) ->
            synonym.toString() to canonical.toString()
          },
  ): World {
    if (!setupWorld.isIdle()) throw TaskException("a completed setup world must be idle")
    setupWorld.gameplay(ENGINE).godMode().manual("ValidateSetup")
    if (!setupWorld.isIdle()) throw TaskException("setup validation did not leave the world idle")
    return newGame(assemble(setupWorld.reader), locale, inputOnlySynonyms)
  }

  /** Creates a standalone setup world and resolves its choice-free initialization tasks. */
  public fun newSetupWorld(
      premise: GamePremise,
      locale: String = Vocabulary.ENGLISH,
      inputOnlySynonyms: Iterable<Pair<String, String>> = emptyList(),
  ): World =
      newWorld(premise, locale, inputOnlySynonyms).also {
        with(it.gameplay(ENGINE)) {
          autoExecMode = SAFE
          autoExecNow()
        }
      }

  private fun newWorld(
      premise: GamePremise,
      locale: String,
      inputOnlySynonyms: Iterable<Pair<String, String>>,
  ): WholeWorld = Wiring(premise, locale, inputOnlySynonyms).createWorld()

  /** Constructs one engine world and owns the lifetimes of all its collaborators. */
  internal class Wiring(
      private val premise: GamePremise,
      locale: String,
      inputOnlySynonyms: Iterable<Pair<String, String>>,
  ) {
    init {
      require(ENGINE in premise.actors) { "Game premise must include $ENGINE as an actor" }
    }

    private val vocabulary: Vocabulary =
        Vocabulary.create(premise.ruleset, locale, inputOnlySynonyms)
    private val classTable: ClassTable = ClassTable.forPremise(premise)
    private val transformers: Transformers = Transformers(classTable)
    private val customClasses = CustomClassRuntime(premise.ruleset, transformers)

    // Reader construction depends on the component graph, whose effector in turn needs the reader.
    // The effector does not read it until components begin changing, after construction is
    // complete.
    private val effector: Effector = Effector(transformers) { reader }
    private val components = ComponentGraph(effector, classTable)
    private val events = EventLog()
    private val taskQueues = TaskQueues(events, classTable)
    private val reader: GameReaderImpl =
        GameReaderImpl(classTable, components, transformers, customClasses, premise)
    private val timeline = TimelineImpl(reader, components, events, taskQueues)
    private val limiter = Limiter(classTable, components)
    private val atomicOperationBoundary: AtomicOperationBoundary =
        AtomicOperationBoundary(timeline) {
          world.onAtomicComplete()
        }
    private val changerByActor: Map<Actor, Changer> =
        premise.actors.associateWith { Changer(reader, components, events, it) }
    private val instructorByActor: Map<Actor, Instructor> =
        premise.actors.associateWith {
          Instructor(
              reader,
              limiter,
              changerByActor.getValue(it),
              effector,
              classTable,
              it,
              customClasses,
          )
        }
    private val gameplayByActor: Map<Actor, Gameplay> =
        premise.actors.associateWith(::createGameplay)
    private val initializer =
        Initializer(
            gameplayByActor.getValue(ENGINE),
            instructorByActor.getValue(ENGINE),
            taskQueues,
            classTable,
            timeline,
            premise,
        )
    private val world: WholeWorld =
        WholeWorld(
            components,
            events,
            taskQueues.all(),
            timeline,
            reader,
            classTable,
            vocabulary,
            gameplayByActor,
        )

    internal fun createWorld(): WholeWorld {
      initializer.initialize()
      return world
    }

    private fun createGameplay(actor: Actor): Gameplay {
      val tasks = taskQueues[actor]
      val changer = changerByActor.getValue(actor)
      val instructor = instructorByActor.getValue(actor)
      val implementations =
          Implementations(tasks, taskQueues, reader, timeline, actor, instructor, changer)
      val gameplay =
          ApiTranslation(
              actor,
              reader,
              timeline,
              implementations,
              tasks,
              classTable,
              transformers,
              vocabulary,
              atomicOperationBoundary,
          )
      return gameplay
    }
  }
}
