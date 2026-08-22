package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.invalidPetDefinition
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.ModuleProvenance
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.types.Class
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Type

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
    createComponents(
        orderedModules.flatMap { it.baseType.concreteSubtypesSameClass() } +
            classTable
                .allClasses()
                .filter { it.className !in moduleNames && it.isSingletonType() }
                .flatMap { it.baseType.concreteSubtypesSameClass() },
        cause,
        "singleton",
    )
  }

  /**
   * Materializes provenance targets and Modules observed by provenance conditions before their
   * sources. The source effects then confirm already-selected singleton state without leaving
   * bootstrap tasks pending.
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
                  modules.values.filter { candidate -> candidate.isSubtypeOf(referenced) }
                }
        observedModules + listOfNotNull(modules[gain.target])
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

    while (remaining.isNotEmpty()) {
      var progress = false
      val round = remaining.toList()
      for (type in round) {
        if (gameplay.count("${type.expression}") > 0) {
          remaining.remove(type)
          missingByType.remove(type)
          progress = true
          continue
        }
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
        throw invalidPetDefinition(
            "Could not create $description components; dependencies remain missing:\n$diagnostic"
        )
      }
    }
  }
}
