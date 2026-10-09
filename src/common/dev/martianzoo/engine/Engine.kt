package dev.martianzoo.engine

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.MUST_CLEAN_UP
import dev.martianzoo.pets.api.SystemClasses.TEMPORARY
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ModuleProperties.PREMISE_REQUIREMENT
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.CustomClass
import dev.martianzoo.state.GamePremise
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.validateCustomClasses

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a game at its committed initialization state, ready to be given to a workflow. */
  public fun newGame(
      premise: GamePremise,
      customClasses: Set<CustomClass> = emptySet(),
  ): World {
    validateCustomClasses(premise.catalog, customClasses)
    return Wiring(premise, GameWorld(premise, customClasses = customClasses)).createWorld()
  }

  /**
   * Creates an independently mutable live World at the same completed gameplay position as
   * [source]. Immutable premise data is shared, while state and every engine service are rebuilt.
   * Application callbacks, component listeners, Agents, provisional forms, and workflow control
   * state are not copied.
   *
   * @throws IllegalArgumentException if [source] was not created by this Engine
   * @throws IllegalStateException if [source] is between completed gameplay positions
   */
  public fun fork(source: World): World {
    val wholeWorld =
        source as? WholeWorld
            ?: throw IllegalArgumentException("Unknown World implementation: ${source::class}")
    val current = source.timeline.checkpoint()
    val positions = wholeWorld.recordingPositions.snapshot()
    check(positions.lastOrNull() == current) {
      "cannot fork a World between completed gameplay positions"
    }
    val gameWorld = wholeWorld.gameWorld.fork()
    return Wiring(gameWorld.premise, gameWorld).createFork(positions, wholeWorld.effector)
  }

  /** Constructs one engine world and owns the lifetimes of all its collaborators. */
  private class Wiring(
      private val premise: GamePremise,
      private val gameWorld: GameWorld,
  ) {
    private val classTable = premise.classTable.also(::validatePremise)
    private val elaborator: PetElaborator = PetElaborator(classTable)
    private val customClasses = CustomInstructionRuntime(gameWorld, elaborator)

    // Effect compilation needs the reader, but no effect is read until state begins changing.
    private val effector: Effector = Effector(elaborator, customClasses) { reader }
    private val taskQueues = TaskQueues(gameWorld, classTable)
    private val recordingPositions = RecordingPositions()
    private val reader: GameReader = gameWorld.reader
    private val changer = Changer(reader, gameWorld, effector)
    private val timeline = TimelineImpl(gameWorld, changer, recordingPositions)
    private val limiter = Limiter(classTable, gameWorld)
    private val worldTransaction: WorldTransaction =
        WorldTransaction(
            timeline,
            { world.onTransactionComplete() },
            recordingPositions,
            ::removeTemporaryComponent,
        )
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
        )
    private val world: WholeWorld =
        WholeWorld(
            gameWorld,
            timeline,
            reader,
            classTable,
            actorEngines,
            recordingPositions,
            effector,
        )

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
      recordingPositions.record(timeline.checkpoint().ordinal)
      return world
    }

    internal fun createFork(positions: List<Checkpoint>, sourceEffector: Effector): WholeWorld {
      effector.copyIndexFrom(sourceEffector)
      positions.forEach { recordingPositions.record(it.ordinal) }
      timeline.commit()
      limiter.checkRequiredCounts = true
      return world
    }

    /**
     * Removes all copies of one eligible concrete `Temporary` Type.
     *
     * Cleanup is offered only while the whole task pool is empty. A Type is ineligible while any
     * direct or indirect dependent is itself `Temporary` or `MustCleanUp`, which makes nested
     * lifetimes retire from the inside out. Returning after one Type lets transaction settlement
     * process removal effects and re-read both tasks and dependencies before the next attempt.
     */
    private fun removeTemporaryComponent(): Boolean {
      if (!gameWorld.tasks.isEmpty()) return false
      val temporary = classTable.getClass(TEMPORARY).baseType
      val temporaryComponents = reader.getComponents(temporary)
      if (temporaryComponents.isEmpty()) return false

      val mustCleanUp = classTable.getClass(MUST_CLEAN_UP).baseType
      val type =
          temporaryComponents.elements.firstOrNull { type ->
            !gameWorld.components.hasDependentMatching(type, mustCleanUp, reader) &&
                !gameWorld.components.hasDependentMatching(type, temporary, reader)
          } ?: return false

      instructor
          .execute(remove(type, reader.countComponent(type)), cause = null, actor = ADMIN)
          .forEach(taskQueues::addTasks)
      return true
    }

    private fun validatePremise(classTable: ClassTable) {
      if (premise.modules.isNotEmpty() && premise.premiseClassName == null) {
        throw InvalidGameConfigException("a premise with modules must provide a premise class")
      }
      premise.initialComponentTypes.forEach { expression ->
        val type =
            try {
              classTable.resolve(expression)
            } catch (e: ExpressionException) {
              throw InvalidGameConfigException(
                  "invalid initial component type `$expression`: ${e.detail}",
                  e,
                  e.sourceLocation ?: expression.sourceLocation,
              )
            }
        if (
            type.abstract ||
                !classTable.isInhabited(type) ||
                type.rootClass.declaration.customMetric
        ) {
          val reason =
              when {
                type.rootClass.declaration.customMetric ->
                    "is a virtual custom metric; it cannot be stored as a component"
                !classTable.isInhabited(type) ->
                    "has no realizable concrete type in this game's selected classes"
                else -> "is abstract; specify one concrete type"
              }
          throw InvalidGameConfigException(
              "initial component type `$expression` $reason",
              sourceLocation = expression.sourceLocation,
          )
        }
      }

      val initiallyPresentClassNames =
          premise.modules +
              premise.playerNames +
              listOfNotNull(premise.bootstrapClassName, premise.premiseClassName) +
              premise.classSelections.filter { it.included }.map { it.className } +
              premise.initialComponentTypes.map { classTable.resolve(it).className }
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
        )
  }
}
