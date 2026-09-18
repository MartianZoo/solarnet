package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.SystemClasses.ACTOR
import dev.martianzoo.pets.api.SystemClasses.ANYONE
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.localTypeVariableDeclarations
import dev.martianzoo.pets.ast.withTypeVariables

/**
 * Returns a transformer that records explicit names and the structural choices made by compact
 * transmutations and actor selectors. It applies the region, exclusion, and actor-selector rules in
 * [rules T13-6 through T13-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
 */
public fun ClassTable.inferTypeVariables(): PetTransformer =
    object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        val transformed = transformChildren(node)
        return when (transformed) {
          is Effect -> {
            val visibleNames = transformed.typeVariables.variables.mapNotNull { it.name }.toSet()
            val namedDeclarations =
                transformed.trigger.descendantsOfType<Expression>().filter {
                  it.typeVariableName is Declaration && it.typeVariableName.name !in visibleNames
                }
            validateTypeVariableNames(namedDeclarations)
            val actorClass = resolve(ACTOR.expression).rootClass
            val actorDeclarations =
                transformed.trigger.descendantsOfType<ByTrigger>().map(ByTrigger::by).filter {
                    selector ->
                  selector.simple &&
                      selector.typeVariableName == null &&
                      selector.className != ANYONE &&
                      transformed.typeVariables.variableAt(selector) == null &&
                      resolve(selector).rootClass.let { it.abstract && it.isSubtypeOf(actorClass) }
                }
            transformed.withTypeVariables(
                transformed.typeVariables +
                    TypeVariableScope.fromDeclarations(
                        listOf(transformed.trigger, transformed.instruction),
                        this@inferTypeVariables,
                        unnamedDeclarations = actorDeclarations,
                        namedDeclarations = namedDeclarations,
                    )
            )
          }
          is Action -> {
            val visibleNames = transformed.typeVariables.variables.mapNotNull { it.name }.toSet()
            val namedDeclarations =
                transformed.cost
                    ?.descendantsOfType<Expression>()
                    ?.filter {
                      it.typeVariableName is Declaration &&
                          it.typeVariableName.name !in visibleNames
                    }
                    .orEmpty()
            validateTypeVariableNames(namedDeclarations)
            val localScope =
                TypeVariableScope.fromDeclarations(
                    listOfNotNull(transformed.cost, transformed.instruction),
                    this@inferTypeVariables,
                    namedDeclarations = namedDeclarations,
                )
            requireSharedAcrossRegions(localScope, "Action")
            transformed.withTypeVariables(transformed.typeVariables + localScope)
          }
          is Instruction.Then -> {
            val visibleNames = transformed.typeVariables.variables.mapNotNull { it.name }.toSet()
            val namedDeclarations =
                transformed.localTypeVariableDeclarations().filter {
                  it.typeVariableName!!.name !in visibleNames
                }
            validateTypeVariableNames(namedDeclarations)
            val localScope =
                TypeVariableScope.fromDeclarations(
                    transformed.instructions,
                    this@inferTypeVariables,
                    namedDeclarations = namedDeclarations,
                )
            requireSharedAcrossRegions(localScope, "THEN")
            transformed.withTypeVariables(transformed.typeVariables + localScope)
          }
          is Instruction.Transmute -> {
            val visibleNames = transformed.typeVariables.variables.mapNotNull { it.name }.toSet()
            val namedDeclarations =
                transformed.localTypeVariableDeclarations().filter {
                  it.typeVariableName!!.name !in visibleNames
                }
            validateTypeVariableNames(namedDeclarations)
            val structuralDeclarations =
                (transformed.fromEx as? FromExpression.Compact)
                    ?.arguments
                    ?.filterIsInstance<FromExpression.Unchanged>()
                    ?.map(FromExpression.Unchanged::expression)
                    ?.filter { expression ->
                      expression.typeVariableName == null &&
                          resolve(expression).abstract &&
                          transformed.typeVariables.variableAt(expression) == null
                    }
                    .orEmpty()
            val localScope =
                TypeVariableScope.fromDeclarations(
                    listOf(transformed.gaining, transformed.removing),
                    this@inferTypeVariables,
                    unnamedDeclarations = structuralDeclarations,
                    namedDeclarations = namedDeclarations,
                )
            requireSharedAcrossRegions(localScope, "Transmutation")
            transformed.withTypeVariables(transformed.typeVariables + localScope)
          }
          else -> transformed
        }
      }

      private fun validateTypeVariableNames(declarations: List<Expression>) {
        declarations.forEach { declaration ->
          val name = declaration.typeVariableName!!.name
          if (name in allClassNames) {
            throw ExpressionException("Type-variable name $name is already a Type name")
          }
        }
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
