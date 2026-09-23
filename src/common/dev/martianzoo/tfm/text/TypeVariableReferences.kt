package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.types.TypeVariable
import dev.martianzoo.pets.types.TypeVariable.Occurrence
import dev.martianzoo.pets.types.TypeVariableScope

/** Matches a declaration with a reference to its named variable within one Pets construct. */
internal fun sameNamedTypeVariable(first: Expression, second: Expression): Boolean {
  val left = first.typeVariableName ?: return false
  val right = second.typeVariableName ?: return false
  return left.name == right.name && left.boundClassName == right.boundClassName
}

/** Type-variable identities available while realizing one enclosing Pets element. */
internal class TypeVariableReferences(private val scopes: List<TypeVariableScope> = emptyList()) {
  internal fun including(element: PetElement): TypeVariableReferences {
    val scope = element.typeVariables
    return if (scope.isEmpty || scopes.any { it === scope }) this
    else TypeVariableReferences(scopes + scope)
  }

  internal fun variableUsedAt(expression: Expression): TypeVariable? =
      scopes
          .mapNotNull { scope ->
            scope.variables.singleOrNull { variable ->
              val usage =
                  variable.usages.singleOrNull { scope.currentExpression(it) === expression }
                      ?: return@singleOrNull false
              variable.occurrences.any { occurrence ->
                occurrence.ordinal < usage.ordinal && scope.currentExpression(occurrence) != null
              }
            }
          }
          .distinct()
          .singleOrNull()

  internal fun variableUsedWithin(node: PetNode): TypeVariable? =
      node.descendantsOfType<Expression>().mapNotNull(::variableUsedAt).distinct().singleOrNull()

  internal companion object {
    val EMPTY: TypeVariableReferences = TypeVariableReferences(emptyList())

    fun from(element: PetElement): TypeVariableReferences = EMPTY.including(element)
  }

  private fun TypeVariableScope.currentExpression(occurrence: Occurrence): Expression? =
  // A Class-scoped variable can have occurrences outside the Effect represented by this scope.
  runCatching {
    expressionOf(occurrence)
  }
      .getOrNull()
}
