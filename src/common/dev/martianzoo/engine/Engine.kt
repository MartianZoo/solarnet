package dev.martianzoo.engine

import dev.martianzoo.pets.PetElaborator
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
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.ModuleProperties.PREMISE_REQUIREMENT
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.GameWorld

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a game at its committed initialization state, ready to be given to a workflow. */
  public fun newGame(premise: GamePremise): World = Wiring(premise).createWorld()

  /** Constructs one engine world and owns the lifetimes of all its collaborators. */
  private class Wiring(
      private val premise: GamePremise,
  ) {
    private val classTable = premise.classTable.also(::validatePremise)
    private val elaborator: PetElaborator = PetElaborator(classTable)
    private val customClasses = CustomClassRuntime(premise.catalog, elaborator)

    private val gameWorld = GameWorld(classTable)

    // Effect compilation needs the reader, but no effect is read until state begins changing.
    private val effector: Effector = Effector(elaborator) { reader }
    private val taskQueues = TaskQueues(gameWorld, classTable)
    private val recordingPositions = RecordingPositions()
    private val reader: GameReaderImpl =
        GameReaderImpl(classTable, gameWorld, elaborator, customClasses, premise)
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
        Instructor(reader, limiter, changer, effector, classTable, elaborator, customClasses)
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
            timeline,
            recordingPositions,
        )

    internal fun createWorld(): WholeWorld {
      initializer.initialize()
      recordingPositions.record(timeline.checkpoint().ordinal)
      return world
    }

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
      require(premise.modules.isEmpty() || premise.premiseClassName != null) {
        "a premise with Modules must provide a premise Class"
      }
      premise.initialComponentTypes.forEach { expression ->
        val type = classTable.resolve(expression)
        require(
            !type.abstract && classTable.isInhabited(type) && !type.rootClass.declaration.custom
        ) {
          "initial component type must be concrete, inhabited, and instantiable: $expression"
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
          require(representedClass?.simple == true) {
            "Module Class invariants must name one simple Class: $count"
          }
          return if (classTable.isInhabited(representedClass.className)) 1 else 0
        }
        require(count.expression.simple) { "Module invariants must count a simple class: $count" }
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
              { property -> error("Module premise metrics cannot read properties: $property") },
              { union -> error("Module premise metrics cannot use OR: $union") },
              { rank -> error("Module premise metrics cannot use RANK: $rank") },
          )

      fun holds(requirement: Requirement): Boolean = requirement.isMetBy(::evaluateInhabitedClasses)

      premise.modules
          .flatMap { moduleName -> classTable.getClass(moduleName).invariants }
          .filter { requirement -> THIS !in requirement.descendantsOfType<ClassName>() }
          .forEach { requirement ->
            require(holds(requirement)) { "game premise fails Module invariant: $requirement" }
          }

      premise.modules.forEach { moduleName ->
        val property = classTable.getClass(moduleName).properties[PREMISE_REQUIREMENT]
        val requirement = (property as? RequirementValue)?.value ?: return@forEach
        require(holds(requirement)) { "game premise fails $moduleName requirement: $requirement" }
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
            changer,
            worldTransaction,
        )
  }
}
