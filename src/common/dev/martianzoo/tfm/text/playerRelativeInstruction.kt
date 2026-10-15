package dev.martianzoo.tfm.text

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.types.Dependency.Key

/** Makes the addressed player's ownership implicit without dropping other dependency roles. */
internal fun playerRelativeInstruction(
    instruction: InstructionTree,
    player: ClassName,
    describers: Describers,
): InstructionTree {
  // An existing lexical owner may belong to a fanout, rather than to the addressed player.
  if (instruction.descendantsOfType<Expression>().any { it.typeVariableName?.name == "Me" }) {
    return instruction
  }
  val ownerKey = Key(OWNED, 0)
  val transformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (node == player.expression) return describers.ownerExpression
          if (node is FromExpression.Compact) {
            return transformFromExpression(
                FromExpression.Full(node.toExpression, node.fromExpression)
            )
          }
          val transformed = transformChildren(node)
          val expression = transformed as? Expression ?: return transformed
          val resolved = describers.resolveExpression(expression) ?: return expression
          if (resolved.sourceDependency(ownerKey) != describers.ownerExpression) return expression
          val arguments = expression.arguments.filterNot { it == describers.ownerExpression }
          val implicit =
              expression.copy(arguments = arguments, argumentsSpecified = arguments.isNotEmpty())
          // A second player argument could otherwise move into the omitted owner's role.
          return implicit.takeIf {
            describers.resolveExpression(it)?.sourceDependencies ==
                resolved.sourceDependencies - ownerKey
          } ?: expression
        }
      }
  return transformer.transformInstructionTree(instruction)
}
