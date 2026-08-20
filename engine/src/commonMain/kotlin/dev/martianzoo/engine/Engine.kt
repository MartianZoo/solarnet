package dev.martianzoo.engine

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.ModuleProperties.PREMISE_REQUIREMENT
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.ClassTable

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {

  /** Creates a game at its committed pre-setup baseline, ready to be given to a workflow. */
  public fun newGame(
      premise: GamePremise,
      locale: String = Vocabulary.ENGLISH,
      inputOnlySynonyms: Iterable<Pair<String, String>> = emptyList(),
  ): World = Wiring(premise, locale, inputOnlySynonyms).createWorld()

  /** Constructs one engine world and owns the lifetimes of all its collaborators. */
  internal class Wiring(
      private val premise: GamePremise,
      locale: String,
      inputOnlySynonyms: Iterable<Pair<String, String>>,
  ) {
    private val classTable: ClassTable = ClassTable.forPremise(premise).also(::validatePremise)
    private val vocabulary: Vocabulary =
        premise.createVocabulary(
            classTable.allClassNames,
            locale,
            inputOnlySynonyms,
        )
    private val transformers: Transformers = Transformers(classTable)
    private val customClasses = CustomClassRuntime(premise.authority, transformers)

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

    private fun validatePremise(classTable: ClassTable) {
      premise.initialComponentTypes.forEach { expression ->
        val type = classTable.resolve(expression)
        require(
            !type.abstract &&
                !type.phantom &&
                !type.rootClass.declaration.custom &&
                !type.rootClass.isSingletonType()
        ) {
          "initial component type must be concrete, active, instantiable, and non-singleton: $expression"
        }
      }

      fun countActiveClasses(metric: Metric): Int {
        require(metric is Count) { "Module invariants must count classes: $metric" }
        if (metric.expression.className == CLASS) {
          val representedClass = metric.expression.arguments.singleOrNull()
          require(representedClass?.simple == true) {
            "Module Class invariants must name one simple Class: $metric"
          }
          return if (classTable.isActive(representedClass.className)) 1 else 0
        }
        require(metric.expression.simple) {
          "Module invariants must count a simple class: $metric"
        }
        val type = classTable.findActiveClass(metric.expression.className)?.baseType ?: return 0
        return classTable.allClasses().count { klass ->
          !klass.abstract &&
              klass.baseType.isSubtypeOf(type) &&
              (klass.isSingletonType() ||
                  premise.classSelections.any { selection ->
                    selection.included && selection.className == klass.className
                  })
        }
      }

      fun holds(requirement: Requirement): Boolean = requirement.isMetBy(::countActiveClasses)

      premise.modules
          .flatMap { moduleName -> classTable.getClass(moduleName).invariants() }
          .filter { requirement ->
            THIS !in requirement.descendantsOfType<ClassName>()
          }
          .forEach { requirement ->
            require(holds(requirement)) {
              "game premise fails Module invariant: $requirement"
            }
          }

      premise.modules.forEach { moduleName ->
        val property = classTable.getClass(moduleName).properties[PREMISE_REQUIREMENT]
        val requirement = (property as? RequirementValue)?.value ?: return@forEach
        require(holds(requirement)) {
          "game premise fails $moduleName requirement: $requirement"
        }
      }
    }

    private fun createGameplay(actor: Actor): Gameplay {
      val tasks = taskQueues[actor]
      val changer = changerByActor.getValue(actor)
      val instructor = instructorByActor.getValue(actor)
      val implementations =
          Implementations(tasks, taskQueues, reader, timeline, actor, instructor, changer)
      return ApiTranslation(
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
    }
  }
}
