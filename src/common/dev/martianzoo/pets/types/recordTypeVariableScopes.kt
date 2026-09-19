package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.PetElement
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
            recordLocalScope(
                transformed,
                listOf(transformed.trigger, transformed.instruction),
                transformed.trigger.descendantsOfType(),
                "Effect",
                transformed.trigger.constructLocalTypeVariableDeclarations(),
            )
          }
          is Action -> {
            recordLocalScope(
                transformed,
                listOfNotNull(transformed.cost, transformed.instruction),
                transformed.cost?.descendantsOfType<Expression>().orEmpty(),
                "Action",
                transformed.cost?.constructLocalTypeVariableDeclarations().orEmpty(),
            )
          }
          is Instruction.Then -> {
            recordLocalScope(
                transformed,
                transformed.instructions,
                transformed.localTypeVariableDeclarations(),
                "THEN",
            )
          }
          is Instruction.Transmute -> {
            recordLocalScope(
                transformed,
                listOf(transformed.gaining, transformed.removing),
                transformed.localTypeVariableDeclarations(),
                "Transmutation",
            )
          }
          else -> transformed
        }
      }

      private fun TypeVariableScope.variableIdentities() =
          variables
              .mapNotNull { it.declaration.expression.typeVariableName as? Declaration }
              .mapTo(mutableSetOf()) { it.identity }

      private fun <P : PetElement> recordLocalScope(
          node: P,
          regions: List<PetNode>,
          declarationCandidates: Iterable<Expression>,
          construct: String,
          constructLocalDeclarations: List<Expression> = emptyList(),
      ): P {
        val visibleIdentities = node.typeVariables.variableIdentities()
        val namedDeclarations = declarationCandidates.filter {
          it.typeVariableName is Declaration &&
              constructLocalDeclarations.none { local -> local === it } &&
              it.typeVariableName.identity !in visibleIdentities
        }
        val localScope =
            TypeVariableScope.fromDeclarations(
                regions,
                this@recordTypeVariableScopes,
                namedDeclarations = namedDeclarations,
            )
        requireSharedAcrossRegions(localScope, construct)
        return node.withTypeVariables(node.typeVariables + localScope)
      }

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
