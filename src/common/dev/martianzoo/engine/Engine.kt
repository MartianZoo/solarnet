package dev.martianzoo.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.AgentImpl
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.SystemClasses.CLASS
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

    // Reader construction depends on the component graph, whose effector in turn needs the reader.
    // The effector does not read it until components begin changing, after construction is
    // complete.
    private val effector: Effector = Effector(elaborator) { reader }
    private val components = ComponentGraph(effector, classTable)
    private val events = EventLog()
    private val taskQueues = TaskQueues(events, classTable)
    private val recordingPositions = RecordingPositions()
    private val reader: GameReaderImpl =
        GameReaderImpl(classTable, components, elaborator, customClasses, premise)
    private val timeline = TimelineImpl(reader, components, events, taskQueues, recordingPositions)
    private val limiter = Limiter(classTable, components)
    private val worldTransaction: WorldTransaction =
        WorldTransaction(
            timeline,
            { world.onTransactionComplete() },
            recordingPositions,
            ::removeTemporaryComponents,
        )
    private val changer = Changer(reader, components, events)
    private val instructor =
        Instructor(reader, limiter, changer, effector, classTable, elaborator, customClasses)
    private val agentByActor: Map<Actor, Agent> = premise.actors.associateWith(::createAgent)
    private val initializer =
        Initializer(
            agentByActor.getValue(ADMIN),
            instructor,
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
            agentByActor,
            timeline,
            recordingPositions,
        )

    internal fun createWorld(): WholeWorld {
      initializer.initialize()
      return world
    }

    private fun removeTemporaryComponents(): Boolean {
      if (!taskQueues.all().isEmpty()) return false
      val temporaryComponents = reader.getComponents(classTable.getClass(TEMPORARY).baseType)
      if (temporaryComponents.isEmpty()) return false

      temporaryComponents.elements.forEach { type ->
        val count = reader.countComponent(type)
        if (count > 0) {
          instructor
              .execute(remove(type, count), cause = null, actor = ADMIN)
              .forEach(taskQueues::addTasks)
        }
      }
      return true
    }

    private fun validatePremise(classTable: ClassTable) {
      require(premise.modules.isEmpty() || premise.premiseClassName != null) {
        "a premise with Modules must provide a premise Class"
      }
      premise.initialComponentTypes.forEach { expression ->
        val type = classTable.resolve(expression)
        require(!type.abstract && classTable.isActive(type) && !type.rootClass.declaration.custom) {
          "initial component type must be concrete, active, and instantiable: $expression"
        }
      }

      val initiallyPresentClassNames =
          premise.modules +
              premise.playerNames +
              listOfNotNull(premise.bootstrapClassName, premise.premiseClassName) +
              premise.classSelections.filter { it.included }.map { it.className } +
              premise.initialComponentTypes.map { classTable.resolve(it).className }

      fun countActiveClasses(count: Count): Int {
        if (count.expression.className == CLASS) {
          val representedClass = count.expression.arguments.singleOrNull()
          require(representedClass?.simple == true) {
            "Module Class invariants must name one simple Class: $count"
          }
          return if (classTable.isActive(representedClass.className)) 1 else 0
        }
        require(count.expression.simple) { "Module invariants must count a simple class: $count" }
        val type = classTable.findActiveClass(count.expression.className)?.baseType ?: return 0
        return classTable.allClasses().count { klass ->
          !klass.abstract &&
              klass.baseType.isSubtypeOf(type) &&
              klass.className in initiallyPresentClassNames
        }
      }

      fun evaluateActiveClasses(metric: Metric): Int =
          metric.evaluate(
              ::countActiveClasses,
              { property -> error("Module premise metrics cannot read properties: $property") },
              { union -> error("Module premise metrics cannot use OR: $union") },
              { rank -> error("Module premise metrics cannot use RANK: $rank") },
          )

      fun holds(requirement: Requirement): Boolean = requirement.isMetBy(::evaluateActiveClasses)

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

    private fun createAgent(actor: Actor): Agent {
      val tasks = taskQueues[actor]
      val implementations =
          Implementations(
              tasks,
              taskQueues,
              reader,
              timeline,
              actor,
              instructor,
              changer,
          )
      return AgentImpl(
          actor,
          reader,
          implementations,
          tasks,
          elaborator,
          worldTransaction,
      )
    }
  }
}
