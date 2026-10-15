package dev.martianzoo.pets

import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Expression

/** Source-only expression; extracting its local declaration produces an ordinary Expression. */
internal class SourceExpression(expression: Expression, val body: ClassBody) :
    Expression(
        expression.className,
        expression.arguments,
        expression.refinement,
        expression.argumentsSpecified,
    ) {
  init {
    if (expression.className == THIS) {
      throw PetSyntaxException(
          "`This` refers to the enclosing class and cannot declare an owner-local class",
          sourceLocation = expression.sourceLocation,
      )
    }
    if (expression.typeVariableName != null) {
      throw PetSyntaxException(
          "owner-local class expressions cannot have a type-variable marker on their root; declare the class separately",
          sourceLocation = expression.sourceLocation,
      )
    }
  }

  override fun equals(other: Any?): Boolean =
      this === other || (other is SourceExpression && body === other.body && super.equals(other))

  override fun hashCode(): Int = 31 * super.hashCode() + body.hashCode()
}
