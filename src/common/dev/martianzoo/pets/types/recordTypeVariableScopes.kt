package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.constructLocalTypeVariableDeclarations
import dev.martianzoo.pets.ast.localTypeVariableDeclarations
import dev.martianzoo.pets.ast.withTypeVariables

/**
 * Returns a transformer that records explicitly named Type-variable scopes. It applies the region,
 * exclusion, and actor-selector rules in
 * [rules T13-6 through T13-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
 */
public fun ClassTable.recordTypeVariableScopes(): PetTransformer =
    object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        val transformed = transformChildren(node)
        return when (transformed) {
          is Effect -> {
            val visibleIdentities = transformed.typeVariables.variableIdentities()
            val constructLocalDeclarations =
                transformed.trigger.constructLocalTypeVariableDeclarations()
            val namedDeclarations =
                transformed.trigger.descendantsOfType<Expression>().filter {
                  it.typeVariableName is Declaration &&
                      constructLocalDeclarations.none { local -> local === it } &&
                      it.typeVariableName.identity !in visibleIdentities
                }
            val localScope =
                TypeVariableScope.fromDeclarations(
                    listOf(transformed.trigger, transformed.instruction),
                    this@recordTypeVariableScopes,
                    namedDeclarations = namedDeclarations,
                )
            requireSharedAcrossRegions(localScope, "Effect")
            transformed.withTypeVariables(transformed.typeVariables + localScope)
          }
          is Action -> {
            val visibleIdentities = transformed.typeVariables.variableIdentities()
            val constructLocalDeclarations =
                transformed.cost?.constructLocalTypeVariableDeclarations().orEmpty()
            val namedDeclarations =
                transformed.cost
                    ?.descendantsOfType<Expression>()
                    ?.filter {
                      it.typeVariableName is Declaration &&
                          constructLocalDeclarations.none { local -> local === it } &&
                          it.typeVariableName.identity !in visibleIdentities
                    }
                    .orEmpty()
            val localScope =
                TypeVariableScope.fromDeclarations(
                    listOfNotNull(transformed.cost, transformed.instruction),
                    this@recordTypeVariableScopes,
                    namedDeclarations = namedDeclarations,
                )
            requireSharedAcrossRegions(localScope, "Action")
            transformed.withTypeVariables(transformed.typeVariables + localScope)
          }
          is Instruction.Then -> {
            val visibleIdentities = transformed.typeVariables.variableIdentities()
            val namedDeclarations =
                transformed.localTypeVariableDeclarations().filter {
                  it.typeVariableName!!.identity !in visibleIdentities
                }
            val localScope =
                TypeVariableScope.fromDeclarations(
                    transformed.instructions,
                    this@recordTypeVariableScopes,
                    namedDeclarations = namedDeclarations,
                )
            requireSharedAcrossRegions(localScope, "THEN")
            transformed.withTypeVariables(transformed.typeVariables + localScope)
          }
          is Instruction.Transmute -> {
            val scoped = transformed
            val visibleIdentities = scoped.typeVariables.variableIdentities()
            val namedDeclarations =
                scoped.localTypeVariableDeclarations().filter {
                  it.typeVariableName!!.identity !in visibleIdentities
                }
            val localScope =
                TypeVariableScope.fromDeclarations(
                    listOf(scoped.gaining, scoped.removing),
                    this@recordTypeVariableScopes,
                    namedDeclarations = namedDeclarations,
                )
            requireSharedAcrossRegions(localScope, "Transmutation")
            scoped.withTypeVariables(scoped.typeVariables + localScope)
          }
          else -> transformed
        }
      }

      private fun TypeVariableScope.variableIdentities() =
          variables
              .mapNotNull { it.declaration.expression.typeVariableName as? Declaration }
              .mapTo(mutableSetOf()) { it.identity }

      private fun requireSharedAcrossRegions(scope: TypeVariableScope, construct: String) {
        scope.variables
            .firstOrNull { variable ->
              variable.occurrences.map { it.region }.distinct().size < 2
            }
            ?.let {
              throw ExpressionException(
                  "A $construct Type variable must be used in more than one region: $it"
              )
            }
      }
    }
