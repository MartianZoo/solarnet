package dev.martianzoo.state

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.CustomCodeException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.types.Type

/** Invokes Catalog-provided metrics as passive queries over game state. */
internal class CustomMetricRuntime(
    private val catalog: Catalog,
    private val elaborator: PetElaborator,
) {
  internal fun count(type: Type, reader: GameReaderImpl): Int {
    require(type.rootClass.declaration.custom)
    require(elaborator.classTable.isInhabited(type))

    if (type.abstract) {
      invokeAbstract(type, reader)?.let {
        return it
      }
      val candidates =
          elaborator.classTable.allConcreteSubtypes(type, reader::matchingComponentTypes)
      return (if (type.refinement == null) candidates
          else candidates.filter { it.narrows(type, reader) })
          .sumOf { countConcrete(it, reader) }
    }

    if (type.typeDependencies.any { reader.countComponent(it.boundType) == 0 }) return 0
    return countConcrete(type, reader)
  }

  private fun invokeAbstract(type: Type, reader: GameReader): Int? {
    val implementation = implementationFor(type)
    val count =
        try {
          implementation.countAbstract(reader, type)
        } catch (e: RuntimeException) {
          throw CustomCodeException("Custom metric failed for ${type.expressionFull}", e)
        }
    count?.let { requireValidCount(type, it) }
    return count
  }

  private fun countConcrete(type: Type, reader: GameReader): Int {
    val count =
        try {
          implementationFor(type).count(reader, type)
        } catch (e: RuntimeException) {
          throw CustomCodeException("Custom metric failed for ${type.expressionFull}", e)
        }
    requireValidCount(type, count)
    return count
  }

  private fun implementationFor(type: Type) =
      catalog.customMetric(type.className)
          ?: throw CustomCodeException(
              "Custom class `${type.className}` has no metric implementation"
          )

  private fun requireValidCount(type: Type, count: Int) {
    if (count < 0) {
      throw CustomCodeException("Custom metric `${type.expressionFull}` returned $count")
    }
  }
}
