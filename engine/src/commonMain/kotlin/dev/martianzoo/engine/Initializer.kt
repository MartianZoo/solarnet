package dev.martianzoo.engine

import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.invalidPetDefinition
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.ModuleProvenance
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type

internal class Initializer(
    private val gameplay: Gameplay,
    private val instructor: Instructor,
    private val tasks: TaskQueues,
    private val classTable: ClassTable,
    private val timeline: TimelineImpl,
    private val premise: GamePremise,
) {
  // Taking 14% of total solo game time
  internal fun initialize() {
    val engineEvent = execute("$ENGINE", cause = null).changes.first()
    val engineCause = Cause(ENGINE.expression, engineEvent.ordinal)
    createSingletons(engineCause)
    createInitialComponents(engineCause)
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
    val orderedModules = orderModulesByActiveProvenance()
    val moduleNames = premise.modules.toSet()
    val playerNames = premise.playerClassNames.toSet()
    createComponents(
        premise.playerClassNames.map(classTable::getClass).flatMap {
          classTable.concreteSubtypesSameClass(it.baseType)
        } +
            orderedModules.flatMap { classTable.concreteSubtypesSameClass(it.baseType) } +
            classTable
                .allClasses()
                .filter {
                  it.className !in moduleNames &&
                      it.className !in playerNames &&
                      it.isSingletonType()
                }
                .flatMap { classTable.concreteSubtypesSameClass(it.baseType) },
        cause,
        "singleton",
    )
  }

  /**
   * Orders Modules needed to evaluate provenance conditions before the source whose condition
   * observes them. A source gets the first opportunity to create its target; the ordinary singleton
   * pass later supplies any target that remains absent.
   */
  private fun orderModulesByActiveProvenance(): List<Class> {
    val modules = premise.modules.associateWith(classTable::getClass)
    val dependencies = modules.mapValues { (_, source) ->
      ModuleProvenance.gains(source.declaration).flatMapTo(linkedSetOf()) { gain ->
        val observedModules =
            gain.requirements
                .flatMap { it.descendantsOfType<ClassName>() }
                .filter { it != THIS && classTable.findClass(it) != null }
                .flatMap { referencedName ->
                  val referenced = classTable.getClass(referencedName)
                  modules.values.filter { candidate ->
                    candidate.className != gain.target && candidate.isSubtypeOf(referenced)
                  }
                }
        observedModules
      } - source
    }
    val ordered = mutableListOf<Class>()
    val visiting = mutableSetOf<ClassName>()
    val visited = mutableSetOf<ClassName>()
    fun visit(name: ClassName) {
      if (name in visited) return
      require(visiting.add(name)) { "cyclic active Module provenance involving $name" }
      dependencies.getValue(name).forEach { visit(it.className) }
      visiting.remove(name)
      visited.add(name)
      ordered.add(modules.getValue(name))
    }
    premise.modules.forEach(::visit)
    return ordered
  }

  private fun createInitialComponents(cause: Cause) {
    createComponents(premise.initialComponentTypes.map(classTable::resolve), cause, "initial")
  }

  private fun createComponents(types: Collection<Type>, cause: Cause, description: String) {
    val remaining = types.toMutableList()
    val missingByType = mutableMapOf<Type, Collection<Type>>()
    // TODO: Ignore inactive gated gains here; false mutual provenance can otherwise deadlock.
    val moduleSourcesByTarget =
        premise.modules
            .flatMap { source ->
              ModuleProvenance.gains(classTable.getClass(source).declaration)
                  .filter { it.target in premise.modules }
                  .map { it.target to source }
            }
            .groupBy({ it.first }, { it.second })
    val sourcesByConstructiveType =
        premise.modules
            .flatMap { source ->
              ModuleProvenance.gains(classTable.getClass(source).declaration)
                  .filter { THIS !in it.expression.descendantsOfType<ClassName>() }
                  .mapNotNull { gain ->
                    runCatching { classTable.resolve(gain.expression) }
                        .getOrNull()
                        ?.let { it to source }
                  }
            }
            .groupBy({ it.first }, { it.second })

    while (remaining.isNotEmpty()) {
      var progress = false
      val round = remaining.toList()
      for (type in round) {
        if (
            (moduleSourcesByTarget[type.className].orEmpty() +
                    sourcesByConstructiveType[type].orEmpty())
                .any { source ->
                  gameplay.count("$source") == 0
                }
        ) {
          continue
        }
        if (gameplay.count("${type.expression}") > 0) {
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
                  } ?: "is waiting for a constructive source"
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
        gameplay.count("${dependency.expression}") > 0
      }
}
