package dev.martianzoo.engine

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.CustomCodeException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.state.Component

/** Engine runtime for Kotlin-provided instruction behavior of Pets custom classes. */
internal class CustomInstructionRuntime(
    private val catalog: Catalog,
    private val elaborator: PetElaborator,
) {
  internal fun translateInstruction(component: Component, reader: GameReader): InstructionTree {
    require(component.isCustom)
    require(elaborator.classTable.isInhabited(component.type))

    val type = component.type
    val implementation = catalog.customClass(type.className)
    val args = type.typeDependencies.map { it.boundType }
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

    return elaborator.elaborateCustomInstruction(translated, component.owner)
  }
}
