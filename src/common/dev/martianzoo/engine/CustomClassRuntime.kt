package dev.martianzoo.engine

import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.api.Exceptions.CustomCodeException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.types.Type

/** Engine runtime for Kotlin-provided instruction and metric behavior of Pets custom classes. */
internal class CustomClassRuntime(
    private val catalog: Catalog,
    private val transformers: Transformers,
) {
  internal fun translateInstruction(component: Component, reader: GameReader): InstructionTree {
    require(component.isCustom)
    require(transformers.classTable.isActive(component.type))

    val type = component.type
    val implementation = catalog.customClass(type.className)
    val args = type.expressionFull.arguments.map(reader::resolve)
    val missing = args.filter { reader.countComponent(it) == 0 }
    if (missing.any()) throw DependencyException(missing)

    val translated =
        try {
          when (args.size) {
            0 -> implementation.translate(reader)
            1 -> implementation.translate(reader, args[0])
            2 -> implementation.translate(reader, args[0], args[1])
            3 -> implementation.translate(reader, args[0], args[1], args[2])
            4 -> implementation.translate(reader, args[0], args[1], args[2], args[3])
            else ->
                throw ExpressionException(
                    "Custom instruction types with ${args.size} dependencies are not supported: " +
                        type.expressionFull
                )
          }
        } catch (e: NotImplementedError) {
          throw ExpressionException(
              "Custom type ${type.expressionFull} has no instruction behavior for " +
                  "${args.size} dependencies",
              e,
          )
        } catch (e: RuntimeException) {
          throw CustomCodeException("Custom instruction failed for ${type.expressionFull}", e)
        }

    val outputTransformer =
        with(transformers) {
          chain(
              atomizer(),
              insertDefaults(),
              component.owner?.let(::replaceOwnerWith),
          )
        }
    return outputTransformer.transformInstructionTree(translated)
  }

  internal fun count(type: Type, reader: GameReaderImpl): Int {
    require(type.rootClass.declaration.custom)
    require(transformers.classTable.isActive(type))

    if (type.abstract) {
      val candidates =
          transformers.classTable.allConcreteSubtypes(type, reader::matchingComponentTypes)
      return (if (type.refinement == null) candidates
          else candidates.filter { it.narrows(type, reader) })
          .sumOf { countConcrete(it, reader) }
    }

    if (type.typeDependencies.any { reader.countComponent(it.boundType) == 0 }) return 0

    return countConcrete(type, reader)
  }

  private fun countConcrete(type: Type, reader: GameReader): Int {
    val implementation =
        catalog.customMetric(type.className)
            ?: throw CustomCodeException(
                "Custom class `${type.className}` has no metric implementation"
            )

    val count =
        try {
          implementation.count(reader, type)
        } catch (e: RuntimeException) {
          throw CustomCodeException("Custom metric failed for ${type.expressionFull}", e)
        }
    if (count < 0) {
      throw CustomCodeException("Custom metric `${type.expressionFull}` returned $count")
    }
    return count
  }
}
