package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.From
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScalarAndType
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.ast.TypeExpr.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpr.GenericTypeExpr
import kotlin.reflect.KClass
import kotlin.reflect.cast

public fun visit(node: PetNode, visitor: (PetNode) -> Boolean) = PetVisitor(visitor).visit(node)

public fun countNodesInTree(root: PetNode): Int {
  var count = 0
  visit(root) { count++; true }
  return count
}

public inline fun <reified P : PetNode> childNodesOfType(root: PetNode): Set<P> =
    childNodesOfType(P::class, root)

public fun <P : PetNode> childNodesOfType(type: KClass<P>, root: PetNode): Set<P> {
  val found = mutableSetOf<P>()
  visit(root) {
    if (type.isInstance(it)) found += type.cast(it)
    true
  }
  return found
}

private class PetVisitor(val visitor: (PetNode) -> Boolean) {

  fun visit(node: PetNode) {
    if (visitor(node)) recurse(node)
  }

  private fun visit(nodes: Iterable<PetNode>) = nodes.forEach(::visit)

  private fun recurse(node: PetNode) {
    node.run {
      when (this) {
        is ClassName -> {}
        is TypeExpr ->
            when (this) {
              is ClassLiteral -> visit(className)
              is GenericTypeExpr -> {
                visit(root)
                visit(args)
                refinement?.let(::visit)
              }
            }
        is ScalarAndType -> visit(typeExpr)
        is Requirement ->
            when (this) {
              is Requirement.Min -> visit(sat)
              is Requirement.Max -> visit(sat)
              is Requirement.Exact -> visit(sat)
              is Requirement.Or -> visit(requirements)
              is Requirement.And -> visit(requirements)
              is Requirement.Transform -> visit(requirement)
            }
        is Instruction ->
            when (this) {
              is Instruction.Gain -> visit(sat)
              is Instruction.Remove -> visit(sat)
              is Instruction.Per -> {
                visit(sat)
                visit(instruction)
              }
              is Instruction.Gated -> {
                visit(gate)
                visit(instruction)
              }
              is Instruction.Transmute -> visit(from)
              is Instruction.Custom -> visit(arguments)
              is Instruction.Then -> visit(instructions)
              is Instruction.Or -> visit(instructions)
              is Instruction.Multi -> visit(instructions)
              is Instruction.Transform -> visit(instruction)
            }
        is From ->
            when (this) {
              is From.SimpleFrom -> {
                visit(toType)
                visit(fromType)
              }
              is From.ComplexFrom -> {
                visit(className)
                visit(arguments)
                refinement?.let(::visit)
              }
              is From.TypeAsFrom -> visit(typeExpr)
            }
        is Effect -> {
          visit(trigger)
          visit(instruction)
        }
        is Trigger ->
            when (this) {
              is Trigger.OnGain -> visit(typeExpr)
              is Trigger.OnRemove -> visit(typeExpr)
              is Trigger.Transform -> visit(trigger)
            }
        is Action -> {
          cost?.let(::visit)
          visit(instruction)
        }
        is Cost ->
            when (this) {
              is Cost.Spend -> visit(sat)
              is Cost.Per -> {
                visit(cost)
                visit(sat)
              }
              is Cost.Or -> visit(costs)
              is Cost.Multi -> visit(costs)
              is Cost.Transform -> visit(cost)
            }
      }
    }
  }
}
