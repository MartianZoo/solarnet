package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.KindException
import dev.martianzoo.pets.api.Exceptions.invalidPetDefinition
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type

internal class Initializer(
    private val reader: GameReader,
    private val elaborator: PetElaborator,
    private val instructor: Instructor,
    private val tasks: TaskQueues,
    private val classTable: ClassTable,
    private val timeline: TimelineImpl,
    private val premise: GamePremise,
    private val actorEngines: (Actor) -> ActorEngine,
) {
  // Taking 14% of total solo game time
  internal fun initialize() {
    val adminEvent = execute("$ADMIN", cause = null).changes.first()
    val adminCause = Cause(ADMIN.expression, adminEvent.ordinal)
    val premiseCause = createBootstrapComponent(adminCause) ?: adminCause
    createConfiguredComponents(premiseCause, adminCause)
    drainBootstrapTasks()
    verifyCompletedBootstrap()
    timeline.initializationFinished()
    timeline.commit()
  }

  private fun createBootstrapComponent(cause: Cause): Cause? =
      premise.bootstrapClassName?.let { className ->
        val event = execute("$className", cause).changes.first()
        Cause(className.expression, event.ordinal)
      }

  /**
   * Executes a bootstrap instruction without creating a task for the instruction itself. Bootstrap
   * creation is explicitly mandatory because failure to create the first Actor or an initial
   * Component must stop initialization rather than become an omitted change.
   */
  private fun execute(instruction: String, cause: Cause?): TaskResult = timeline.atomic {
    val parsed = elaborator.elaborateInput(Parsing.parse<Instruction>("$instruction!"))
    if (parsed !is Instruction) {
      throw KindException("Preprocessing produced `$parsed`, which is not an Instruction")
    }
    instructor.execute(parsed, cause, ADMIN).forEach(tasks::addTasks)
  }

  /** Executes a generated premise recipe, or directly creates an uncompiled custom premise. */
  private fun createConfiguredComponents(premiseCause: Cause, fallbackCause: Cause) {
    premise.premiseClassName?.let {
      execute("$it", premiseCause)
      return
    }
    createComponents(
        premise.playerNames.map(classTable::getClass).flatMap {
          classTable.concreteSubtypesSameClass(it.baseType)
        },
        fallbackCause,
        "premise",
    )
    createComponents(
        premise.initialComponentTypes.map(classTable::resolve),
        fallbackCause,
        "initial",
    )
  }

  /** Runs choice-free queued initialization work in stable insertion order. */
  private fun drainBootstrapTasks() {
    val allTasks = tasks.all()
    while (!allTasks.isEmpty()) {
      if (allTasks.selectedTask() != null) break
      val taskId = allTasks.ids().first()
      val assignee = allTasks.getTaskData(taskId).assignee
      actorEngines(assignee).selectTask(taskId)
    }
    allTasks.requireAllQueuesEmpty()
  }

  private fun verifyCompletedBootstrap() {
    val expected =
        listOfNotNull(premise.bootstrapClassName, premise.premiseClassName)
            .map(classTable::getClass)
            .map(Class::baseType) +
            premise.modules.map(classTable::getClass).map(Class::baseType) +
            premise.playerNames.map(classTable::getClass).map(Class::baseType) +
            premise.initialComponentTypes.map(classTable::resolve)
    val invalidCounts = expected.associateWith(reader::count).filterValues { it != 1 }
    if (invalidCounts.isNotEmpty()) {
      throw invalidPetDefinition(
          "Bootstrap did not create each required component exactly once: " +
              invalidCounts.entries.joinToString { (type, count) ->
                "${type.expressionFull} (found $count)"
              }
      )
    }
  }

  private fun createComponents(types: Collection<Type>, cause: Cause, description: String) {
    val remaining = types.toMutableList()
    val missingByType = mutableMapOf<Type, Collection<Type>>()

    while (remaining.isNotEmpty()) {
      var progress = false
      val round = remaining.toList()
      for (type in round) {
        if (reader.count(type) > 0) {
          remaining.remove(type)
          missingByType.remove(type)
          progress = true
          if (aBlockedTypeCanNowProceed(missingByType)) break
          continue
        }
        try {
          execute("${type.expression}", cause)
          remaining.remove(type)
          missingByType.remove(type)
          progress = true
          if (aBlockedTypeCanNowProceed(missingByType)) break
        } catch (e: DependencyException) {
          missingByType[type] = e.dependencies
        }
      }

      if (!progress) {
        val diagnostic =
            remaining.joinToString(separator = "\n") { type ->
              val reason =
                  missingByType[type]?.let { dependencies ->
                    "requires " + dependencies.joinToString { "${it.expressionFull}" }
                  } ?: "could not be created"
              "  ${type.expressionFull} $reason"
            }
        throw invalidPetDefinition(
            "Could not create $description components; dependencies remain missing:\n$diagnostic"
        )
      }
    }
  }

  private fun aBlockedTypeCanNowProceed(
      missingByType: Map<Type, Collection<Type>>,
  ): Boolean =
      missingByType.values.flatten().any { dependency ->
        reader.count(dependency) > 0
      }
}
