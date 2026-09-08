package dev.martianzoo.engine

import dev.martianzoo.engine.Agent.Companion.parse
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.invalidPetDefinition
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type

internal class Initializer(
    private val agent: Agent,
    private val instructor: Instructor,
    private val tasks: TaskQueues,
    private val classTable: ClassTable,
    private val timeline: TimelineImpl,
    private val premise: GamePremise,
) {
  // Taking 14% of total solo game time
  internal fun initialize() {
    val adminEvent = execute("$ADMIN", cause = null).changes.first()
    val adminCause = Cause(ADMIN.expression, adminEvent.ordinal)
    val premiseCause = createBootstrapComponent(adminCause) ?: adminCause
    createPremiseComponents(premiseCause)
    drainBootstrapTasks()
    createInitialComponents(adminCause)
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
    instructor
        .execute(agent.parse<Instruction>("$instruction!"), cause, ADMIN)
        .forEach(tasks::addTasks)
  }

  /** Creates the resolved premise recipe; direct creation remains a custom-premise fallback. */
  private fun createPremiseComponents(cause: Cause) {
    premise.premiseClassName?.let { execute("$it", cause) }
    createComponents(
        premise.playerNames.map(classTable::getClass).flatMap {
          classTable.concreteSubtypesSameClass(it.baseType)
        },
        cause,
        "premise",
    )
  }

  private fun createInitialComponents(cause: Cause) {
    createComponents(premise.initialComponentTypes.map(classTable::resolve), cause, "initial")
  }

  /** Runs choice-free queued initialization work in stable insertion order. */
  private fun drainBootstrapTasks() {
    agent.autoExecNow()
    tasks.all().requireAllQueuesEmpty()
  }

  private fun verifyCompletedBootstrap() {
    val expected =
        listOfNotNull(premise.bootstrapClassName, premise.premiseClassName)
            .map(classTable::getClass)
            .map(Class::baseType) +
            premise.modules.map(classTable::getClass).map(Class::baseType) +
            premise.playerNames.map(classTable::getClass).map(Class::baseType) +
            premise.initialComponentTypes.map(classTable::resolve)
    val missing = expected.filter { agent.count("${it.expression}") == 0 }
    if (missing.isNotEmpty()) {
      throw invalidPetDefinition(
          "Bootstrap completed without required components: " +
              missing.joinToString { "${it.expressionFull}" }
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
        if (agent.count("${type.expression}") == 0) {
          try {
            execute("${type.expression}", cause)
          } catch (e: DependencyException) {
            missingByType[type] = e.dependencies
            continue
          }
        }
        remaining.remove(type)
        missingByType.remove(type)
        progress = true
        if (aBlockedTypeCanNowProceed(missingByType)) break
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
        agent.count("${dependency.expression}") > 0
      }
}
