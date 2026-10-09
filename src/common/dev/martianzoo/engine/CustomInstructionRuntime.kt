package dev.martianzoo.engine

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.CustomCodeException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Component
import dev.martianzoo.state.CustomInstruction
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.GameWorld

/** Engine runtime for Kotlin-provided instructions of computed Signals. */
internal class CustomInstructionRuntime(
    private val gameWorld: GameWorld,
    private val elaborator: PetElaborator,
) {
  internal fun translateInstruction(component: Component, reader: GameReader): InstructionTree {
    require(elaborator.classTable.isInhabited(component.type))

    val type = component.type
    val implementation =
        gameWorld.customClassOrNull(type.className) as? CustomInstruction
            ?: throw ExpressionException(
                "custom instruction `${type.className}` has no implementation"
            )
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
                    "custom instruction types with ${args.size} dependencies are not supported: " +
                        "`${type.expressionFull}`"
                )
          }
        } catch (e: NotImplementedError) {
          throw CustomCodeException(
              "custom instruction failed for `${type.expressionFull}`: ${e.message}",
              e,
          )
        } catch (e: RuntimeException) {
          throw CustomCodeException(
              "custom instruction failed for `${type.expressionFull}`: ${e.message}",
              e,
          )
        }

    return try {
      elaborator.elaborateCustomInstruction(translated, component.owner)
    } catch (e: PetException) {
      throw CustomCodeException(
          "custom instruction for `${type.expressionFull}` returned invalid Pets `$translated`: ${e.detail}",
          e,
          e.sourceLocation,
      )
    }
  }
}
