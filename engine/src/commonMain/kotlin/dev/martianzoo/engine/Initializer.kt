package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Type
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.types.MClassTable
import dev.martianzoo.types.MType

internal class Initializer(
    private val gameplay: Gameplay,
    private val instructor: Instructor,
    private val tasks: TaskQueues,
    private val classes: MClassTable,
    private val timeline: TimelineImpl,
) {
  // Taking 14% of total solo game time
  internal fun initialize() {
    val engineEvent = execute("$ENGINE", cause = null).changes.first()
    val engineCause = Cause(ENGINE.expression, engineEvent.ordinal)
    createSingletons(engineCause)
    timeline.initializationFinished()
    timeline.commit()
  }

  /** Executes a bootstrap instruction without creating a task for the instruction itself. */
  private fun execute(instruction: String, cause: Cause?): TaskResult = timeline.atomic {
    instructor.execute(gameplay.parse<Instruction>("$instruction!"), cause).forEach(tasks::addTasks)
  }

  /**
   * Singleton types are discovered by class, not dependency order. Retry only dependency-blocked
   * types until each dependency has had a chance to be created by an earlier round.
   */
  private fun createSingletons(cause: Cause) {
    val remaining =
        classes
            .allClasses()
            .filter { it.isSingletonType() }
            .flatMap { it.baseType.concreteSubtypesSameClass() }
            .toMutableList()
    val missingByType = mutableMapOf<MType, Collection<Type>>()

    while (remaining.isNotEmpty()) {
      var progress = false
      val round = remaining.toList()
      for (type in round) {
        try {
          execute("${type.expression}", cause)
          remaining.remove(type)
          missingByType.remove(type)
          progress = true
        } catch (e: DependencyException) {
          missingByType[type] = e.dependencies
        }
      }

      if (!progress) {
        val diagnostic =
            remaining.joinToString(separator = "\n") { type ->
              val missing = missingByType.getValue(type).joinToString { "${it.expressionFull}" }
              "  ${type.expressionFull} requires $missing"
            }
        error("Could not create singleton components; dependencies remain missing:\n$diagnostic")
      }
    }
  }
}
