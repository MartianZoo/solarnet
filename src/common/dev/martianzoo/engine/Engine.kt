package dev.martianzoo.engine

import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ModuleProperties.PREMISE_REQUIREMENT
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.CustomClass
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.actors
import dev.martianzoo.state.validateCustomClasses

/**
 * Constructs live game state from a resolved [GamePremise]. A returned [World] contains the
 * configured components, shared rule definitions, and the Actor-scoped engines that interpret
 * subsequent commands. Construction does not create clients or obtain Player choices.
 *
 * The [proposed interaction contract](../../../../../docs/agents/ENGINE_CONTRACT.md) remains under
 * human review. In particular, existing copy APIs below are not a decision to retain speculative
 * execution in that design.
 */
public object Engine {

  /**
   * Creates a World after its configured bootstrap has finished. The returned World has Admin, the
   * selected class representatives, and the required premise components, with no pending bootstrap
   * tasks. Its initial position is recorded and is the earliest navigable position. Game setup that
   * requires Player choices must be started afterward through ordinary commands.
   *
   * [customClasses] supplies implementations for every custom declaration in the premise's catalog,
   * including declarations excluded from this game. The empty default is valid only when that
   * catalog requires no such implementation. Missing, mismatched, or duplicate implementations fail
   * validation. The World retains the supplied implementations; it does not copy their Kotlin
   * objects.
   *
   * Configuration, unresolved bootstrap choices, or invalid resulting counts cause construction to
   * fail without returning a partial World. No guard or completion callback is installed.
   */
  public fun newGame(
      premise: GamePremise,
      customClasses: Set<CustomClass> = emptySet(),
  ): World {
    validateCustomClasses(premise.catalog, customClasses)
    return Wiring(premise, GameWorld(premise, customClasses = customClasses)).createWorld()
  }

  /**
   * Copies [source]'s current components, tasks, event history, and recorded positions into an
   * independently mutable World. The source must be at the end of its recorded history. The copy
   * cannot seek before its creation position, even though its earlier events remain readable.
   *
   * Premise data and custom-class implementation objects are shared. Mutable engine collections and
   * event commentary are copied; guards, completion callbacks, component listeners, and client
   * objects are not. Copying a [SpeculativeWorld] materializes its inherited and local history.
   * Subsequent gameplay in either World leaves the other's game state unchanged.
   *
   * @throws IllegalArgumentException if [source] was not created by this Engine
   * @throws IllegalStateException if [source] is not at the end of its recorded history or its
   *   underlying source has invalidated it
   *
   * TODO(ENGINE_CONTRACT): Investigate whether independent copying belongs in the public engine API
   *   after the client contract is reviewed; existing test-fixture use does not select a gameplay
   *   contract.
   */
  public fun fork(source: World): World {
    val (wholeWorld, positions) = forkSource(source)
    val gameWorld = wholeWorld.gameWorld.fork()
    return Wiring(gameWorld.premise, gameWorld).createFork(positions, wholeWorld.effector)
  }

  /**
   * Creates the existing [SpeculativeWorld] implementation without changing [source]. Local changes
   * affect only the returned World. Source game-state changes invalidate it and its retained read
   * views, even if those source changes are subsequently undone. Event commentary alone does not
   * invalidate it. Source commands that temporarily apply events have the same invalidating effect.
   *
   * [source] must satisfy [fork]'s requirements. A source positioned in its recorded past must
   * discard its retained future before this call. [SpeculativeWorld.reset] restores the creation
   * state; [fork] produces an independent copy. Guards cannot be installed on this result, so
   * application checks such as card-identity tracking are absent.
   *
   * This documents an existing capability pending the removal review on [SpeculativeWorld];
   * speculative execution is not selected by the proposed client contract.
   *
   * @throws IllegalArgumentException if [source] was not created by this Engine
   * @throws IllegalStateException if [source] is not at the end of valid recorded history
   */
  public fun overlay(source: World): SpeculativeWorld {
    val (wholeWorld, positions) = forkSource(source)
    val gameWorld = wholeWorld.gameWorld.overlay()
    return Wiring(gameWorld.premise, gameWorld, overlaySource = wholeWorld).createOverlay(positions)
  }

  private fun forkSource(source: World): Pair<WholeWorld, List<Checkpoint>> {
    val wholeWorld =
        source as? WholeWorld
            ?: throw IllegalArgumentException("Unknown World implementation: ${source::class}")
    val current = source.timeline.checkpoint()
    val positions = source.timeline.positions
    check(positions.lastOrNull() == current) {
      "cannot fork a World away from the end of completed gameplay history"
    }
    return wholeWorld to positions
  }

  /** Constructs one engine world and owns the lifetimes of all its collaborators. */
  private class Wiring(
      private val premise: GamePremise,
      private val gameWorld: GameWorld,
      private val overlaySource: WholeWorld? = null,
  ) {
    private val classTable = premise.classTable.also(::validatePremise)
    private val elaborator: PetElaborator = PetElaborator(classTable)
    private val customClasses = CustomInstructionRuntime(gameWorld, elaborator)

    // Effect compilation needs the reader, but no effect is read until state begins changing.
    private val effector: Effector =
        Effector(elaborator, customClasses, overlaySource?.effector) { reader }
    private val transactions = Transactions(gameWorld)
    private val taskQueues: TaskQueues =
        TaskQueues(gameWorld, transactions::forEvent) { timeline.requireAtEnd() }
    private val reader: GameReader = gameWorld.reader
    private val changer: Changer =
        Changer(reader, gameWorld, effector, transactions::forEvent) { timeline.requireAtEnd() }
    private val timeline: TimelineImpl = TimelineImpl(gameWorld, changer)
    private val limiter = Limiter(classTable, gameWorld)
    private val worldTransaction: WorldTransaction =
        WorldTransaction(timeline) { world.onTransactionComplete() }
    private val instructor =
        Instructor(reader, limiter, changer, effector, classTable, elaborator, timeline)
    private val actorEngines: Map<Actor, ActorEngine> =
        premise.actors.associateWith(::createActorEngine)
    private val initializer =
        Initializer(
            reader,
            elaborator,
            instructor,
            taskQueues,
            gameWorld,
            classTable,
            timeline,
            premise,
            actorEngines::getValue,
            transactions,
        )
    private val world: WholeWorld =
        if (overlaySource == null) {
          WholeWorld(gameWorld, timeline, reader, classTable, actorEngines, effector)
        } else {
          OverlayWorld(gameWorld, timeline, reader, classTable, actorEngines, effector)
        }

    internal fun createWorld(): WholeWorld {
      try {
        initializer.initialize()
        limiter.checkRequiredCounts = true
      } catch (e: GameplayException) {
        throw InvalidGameConfigException(
            "game setup cannot complete: ${e.detail}",
            e,
            e.sourceLocation,
        )
      } catch (e: NotFullySpecifiedException) {
        throw InvalidGameConfigException(
            "game setup cannot complete: ${e.detail}",
            e,
            e.sourceLocation,
        )
      }
      timeline.recordPosition()
      requireSignedSelectionsMatchClassComponents()
      return world
    }

    internal fun createFork(positions: List<Checkpoint>, sourceEffector: Effector): WholeWorld {
      transactions.initializationFinished()
      effector.copyIndexFrom(sourceEffector)
      timeline.restorePositions(positions)
      timeline.commit()
      limiter.checkRequiredCounts = true
      requireSignedSelectionsMatchClassComponents()
      return world
    }

    internal fun createOverlay(positions: List<Checkpoint>): SpeculativeWorld {
      transactions.initializationFinished()
      timeline.restorePositions(positions)
      timeline.commit()
      limiter.checkRequiredCounts = true
      return world as OverlayWorld
    }

    /** Every signed premise selection must agree with the live Class representatives. */
    private fun requireSignedSelectionsMatchClassComponents() {
      val present =
          gameWorld.components.getAll(classTable.classClass.baseType, NoGameState).elements.mapTo(
              linkedSetOf()
          ) {
            it.expression.arguments.single().className
          }
      val included =
          premise.classSelections
              .filter { it.included }
              .filter { selection ->
                classTable.getClass(selection.className).let { selectedClass ->
                  !selectedClass.abstract && classTable.isInhabited(selectedClass)
                }
              }
              .mapTo(linkedSetOf()) { it.className }
      val excluded =
          premise.classSelections.filterNot { it.included }.mapTo(linkedSetOf()) { it.className }
      val missing = included - present
      val unexpected = excluded.intersect(present)
      check(missing.isEmpty() && unexpected.isEmpty()) {
        buildString {
          append("World Class components do not match its signed GamePremise selections")
          if (missing.isNotEmpty()) append("; missing: ${missing.joinToString()}")
          if (unexpected.isNotEmpty()) append("; unexpected: ${unexpected.joinToString()}")
        }
      }
    }

    private fun validatePremise(classTable: ClassTable) {
      if (premise.modules.isNotEmpty() && premise.premiseClassName == null) {
        throw InvalidGameConfigException("a premise with modules must provide a premise class")
      }
      val initiallyPresentClassNames =
          premise.modules +
              premise.playerNames +
              listOfNotNull(premise.bootstrapClassName, premise.premiseClassName) +
              premise.classSelections.filter { it.included }.map { it.className }
      val inhabitedConcreteClasses = classTable.allInhabitedConcreteClasses()

      fun countInhabitedClasses(count: Count): Int {
        if (count.expression.className == CLASS) {
          val representedClass = count.expression.arguments.singleOrNull()
          if (representedClass?.simple != true) {
            throw InvalidPetDefinitionException(
                "module class invariants must name one simple class: `$count`",
                sourceLocation = count.sourceLocation,
            )
          }
          return if (classTable.isInhabited(representedClass.className)) 1 else 0
        }
        if (!count.expression.simple) {
          throw InvalidPetDefinitionException(
              "module invariants must count a simple class: `$count`",
              sourceLocation = count.sourceLocation,
          )
        }
        val type = classTable.findInhabitedClass(count.expression.className)?.baseType ?: return 0
        return classTable.allClasses().count { klass ->
          klass in inhabitedConcreteClasses &&
              klass.baseType.isSubtypeOf(type) &&
              klass.className in initiallyPresentClassNames
        }
      }

      fun evaluateInhabitedClasses(metric: Metric): Int =
          metric.evaluate(
              ::countInhabitedClasses,
              { property ->
                throw InvalidPetDefinitionException(
                    "module premise metrics cannot read properties: `$property`",
                    sourceLocation = property.sourceLocation,
                )
              },
              { union ->
                throw InvalidPetDefinitionException(
                    "module premise metrics cannot use `OR`: `$union`",
                    sourceLocation = union.sourceLocation,
                )
              },
              { rank ->
                throw InvalidPetDefinitionException(
                    "module premise metrics cannot use `RANK`: `$rank`",
                    sourceLocation = rank.sourceLocation,
                )
              },
          )

      fun holds(requirement: Requirement): Boolean = requirement.isMetBy(::evaluateInhabitedClasses)

      premise.modules
          .flatMap { moduleName -> classTable.getClass(moduleName).invariants }
          .filter { requirement -> THIS !in requirement.descendantsOfType<ClassName>() }
          .forEach { requirement ->
            if (!holds(requirement)) {
              throw InvalidGameConfigException(
                  "game premise fails module invariant: `$requirement`",
                  sourceLocation = requirement.sourceLocation,
              )
            }
          }

      premise.modules.forEach { moduleName ->
        val property = classTable.getClass(moduleName).properties[PREMISE_REQUIREMENT]
        val requirement = (property as? RequirementValue)?.value ?: return@forEach
        if (!holds(requirement)) {
          throw InvalidGameConfigException(
              "game premise fails `$moduleName` requirement: `$requirement`",
              sourceLocation = requirement.sourceLocation,
          )
        }
      }
    }

    private fun createActorEngine(actor: Actor): ActorEngine =
        ActorEngine(
            gameWorld.tasksFor(actor),
            gameWorld,
            taskQueues,
            reader,
            timeline,
            actor,
            instructor,
            worldTransaction,
            elaborator,
            transactions,
        )
  }
}
