package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.SystemClasses.ACTOR
import dev.martianzoo.pets.api.SystemClasses.ANYONE
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.withTypeVariables

/** Returns a transformer that discovers and records every authored Type-variable scope. */
public fun ClassTable.inferTypeVariables(): PetTransformer =
    object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        val transformed = transformChildren(node)
        return when (transformed) {
          is Effect -> {
            val actorClass = resolve(ACTOR.expression).rootClass
            val actorDeclarations =
                transformed.trigger.descendantsOfType<ByTrigger>().map(ByTrigger::by).filter {
                    selector ->
                  !selector.complement &&
                      selector.simple &&
                      selector.className != ANYONE &&
                      transformed.typeVariables.variableAt(selector) == null &&
                      resolve(selector).rootClass.let { it.abstract && it.isSubtypeOf(actorClass) }
                }
            transformed.withTypeVariables(
                transformed.typeVariables +
                    TypeVariableScope.infer(
                        listOf(transformed.trigger, transformed.instruction),
                        this@inferTypeVariables,
                        explicitDeclarations = actorDeclarations,
                        visibleScope = transformed.typeVariables,
                    )
            )
          }
          is Action ->
              transformed.withTypeVariables(
                  transformed.typeVariables +
                      TypeVariableScope.infer(
                          listOfNotNull(transformed.cost, transformed.instruction),
                          this@inferTypeVariables,
                      )
              )
          is Instruction.Then ->
              transformed.withTypeVariables(
                  transformed.typeVariables +
                      TypeVariableScope.infer(
                          transformed.instructions,
                          this@inferTypeVariables,
                      )
              )
          is Instruction.Transmute ->
              transformed.withTypeVariables(
                  transformed.typeVariables +
                      TypeVariableScope.infer(
                          listOf(transformed.gaining, transformed.removing),
                          this@inferTypeVariables,
                          includeRegionRoots = false,
                      )
              )
          else -> transformed
        }
      }
    }
