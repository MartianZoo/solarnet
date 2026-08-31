package dev.martianzoo.pets.ast

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.types.TypeVariableScope
import dev.martianzoo.pets.types.inferTypeVariables

/**
 * A "major" kind of Pets node, like an [Instruction], but not an ancillary type like
 * [FromExpression], [ScaledExpression], or [ClassName].
 */
public sealed class PetElement : PetNode() {
  private var typeVariablesIn: TypeVariableScope = TypeVariableScope.EMPTY

  /** Authored Type variables visible in this element's own choice scope. */
  public val typeVariables: TypeVariableScope
    get() = typeVariablesIn

  internal fun recordTypeVariables(scope: TypeVariableScope) {
    typeVariablesIn = scope
  }
}

internal fun <P : PetElement> P.withTypeVariables(scope: TypeVariableScope): P = apply {
  recordTypeVariables(scope)
}

internal fun PetElement.typeVariablesFor(info: TypeInfo): TypeVariableScope {
  if (!typeVariables.isEmpty) return typeVariables
  val table = (info as? GameReader)?.catalog?.classTable ?: return typeVariables
  return table.inferTypeVariables().transformElement(this).typeVariables
}
