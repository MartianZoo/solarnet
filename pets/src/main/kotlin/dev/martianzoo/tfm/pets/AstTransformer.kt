package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.toSetStrict

open class AstTransformer {
  fun <P : PetsNode> transform(nodes: List<P>) = nodes.map { transform(it) }
  fun <P : PetsNode> transform(nodes: Set<P>) = nodes.map { transform(it) }.toSetStrict()

  open fun <P : PetsNode?> transform(node: P): P {
    if (node == null) return null as P // TODO how'm I even getting away with this
    return node.run {
      when (this) {
        is TypeExpression -> TypeExpression(className, x(specializations), x(requirement))
        is QuantifiedExpression -> QuantifiedExpression(x(typeExpression), scalar)

        is Requirement.Min -> copy(x(qe))
        is Requirement.Max -> copy(x(qe))
        is Requirement.Exact -> copy(x(qe))
        is Requirement.Or -> Requirement.or(x(requirements))
        is Requirement.And -> Requirement.and(x(requirements))
        is Requirement.Prod -> copy(x(requirement))

        is Instruction.Gain -> copy(x(qe))
        is Instruction.Remove -> copy(x(qe))
        is Instruction.Per -> copy(x(instruction), x(qe))
        is Instruction.Gated -> copy(x(requirement), x(instruction))
        is Instruction.Transmute -> copy(x(trans), scalar)
        is Instruction.SimpleFrom -> copy(x(to), x(from))
        is Instruction.ComplexFrom -> copy(className, x(specializations), x(requirement))
        is Instruction.TypeInFrom -> copy(x(type))
        is Instruction.Custom -> copy(name, x(arguments))
        is Instruction.Then -> Instruction.Then(x(instructions))
        is Instruction.Or -> Instruction.Or(x(instructions))
        is Instruction.Multi -> Instruction.Multi(x(instructions))
        is Instruction.Prod -> copy(x(instruction))

        is Effect.Trigger.OnGain -> copy(x(expression))
        is Effect.Trigger.OnRemove -> copy(x(expression))
        is Effect.Trigger.Prod -> copy(x(trigger))
        is Effect -> copy(x(trigger), x(instruction))

        is Action.Cost.Spend -> copy(x(qe))
        is Action.Cost.Per -> copy(x(cost), x(qe))
        is Action.Cost.Or -> Action.Cost.Or(x(costs))
        is Action.Cost.Multi -> Action.Cost.Multi(x(costs))
        is Action.Cost.Prod -> copy(x(cost))
        is Action -> copy(x(cost), x(instruction))

        else -> error("Forgot to add new node type ${this::class.simpleName}")
      } as P
    }
  }

  private fun <P : PetsNode?> x(node: P) = transform(node)
  private fun <P : PetsNode> x(nodes: List<P>) = transform(nodes)
  private fun <P : PetsNode> x(nodes: Set<P>) = transform(nodes)
}
