package dev.martianzoo.testsupport

import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.types.ClassLimitTable
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.GroundType

/** Definition admissibility belongs to tests, not construction of every playable world. */
internal fun validatedComponentLimits(table: ClassTable): ClassLimitTable {
  val limits = table.componentLimits
  val uniqueTargets = mutableMapOf<GroundType, Boolean>()
  val invalid =
      table.allInhabitedConcreteClasses().mapNotNull { dependent ->
        dependent.dependencies
            .typeDependencies()
            .asSequence()
            .flatMap { dependency ->
              val target = dependency.boundType
              if (limits.limitsFor(target).any { it.range.last <= 1 }) emptySequence()
              else table.allConcreteSubtypes(target)
            }
            .firstOrNull { target ->
              !uniqueTargets.getOrPut(target) {
                limits.limitsFor(target).any { it.range.last <= 1 }
              }
            }
            ?.let { dependent to it }
      }
  if (invalid.isNotEmpty()) {
    throw InvalidPetDefinitionException(
        "dependencies must target types with maximum multiplicity 1; first violation per class:\n" +
            invalid.joinToString("\n") { (dependent, target) ->
              "  `${dependent.className}` -> `${target.expressionFull}`"
            },
        sourceLocation = invalid.first().first.className.sourceLocation,
    )
  }
  return limits
}
