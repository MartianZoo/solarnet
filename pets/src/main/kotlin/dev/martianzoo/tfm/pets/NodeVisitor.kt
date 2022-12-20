package dev.martianzoo.tfm.pets

import dev.martianzoo.util.toSetStrict

open class NodeVisitor {
  private fun <P : PetsNode> s(ns: List<P>) = ns.map { s(it) }
  private fun <P : PetsNode> s(ns: Set<P>) = ns.map { s(it) }.toSetStrict()

  open fun <P : PetsNode?> s(node: P): P {
    if (node == null) return null as P
    return node.run {
      when (this) {
        is TypeExpression -> TypeExpression(className, s(specializations), s(requirement))
        is QuantifiedExpression -> QuantifiedExpression(s(typeExpression), scalar)

        is Requirement.Or -> Requirement.or(s(requirements))
        is Requirement.And -> Requirement.and(s(requirements))
        is Requirement.Min -> copy(s(qe))
        is Requirement.Max -> copy(s(qe))
        is Requirement.Exact -> copy(s(qe))
        is Requirement.Prod -> copy(s(requirement))

        is Instruction.Gain -> copy(s(qe))
        is Instruction.Remove -> copy(s(qe))
        is Instruction.Gated -> copy(s(requirement), s(instruction))
        is Instruction.Then -> Instruction.then(s(instructions))
        is Instruction.Or -> Instruction.or(s(instructions))
        is Instruction.Multi -> Instruction.multi(s(instructions))
        is Instruction.Transmute -> copy(s(trans), scalar)
        is Instruction.ComplexFrom -> copy(className, s(specializations), s(requirement))
        is Instruction.SimpleFrom -> copy(s(to), s(from))
        is Instruction.TypeInFrom -> copy(s(type))
        is Instruction.Per -> copy(s(instruction), s(qe))
        is Instruction.Prod -> copy(s(instruction))
        is Instruction.Custom -> copy(name, s(arguments))

        is Effect.Trigger.OnGain -> copy(s(expression))
        is Effect.Trigger.OnRemove -> copy(s(expression))
        is Effect.Trigger.Now -> copy(s(requirement))
        is Effect.Trigger.Prod -> copy(s(trigger))
        is Effect -> copy(s(trigger), s(instruction))
        is Action.Cost.Spend -> copy(s(qe))
        is Action.Cost.Per -> copy(s(cost), s(qe))
        is Action.Cost.Or -> Action.Cost.or(s(costs))
        is Action.Cost.Prod -> copy(s(cost))
        is Action -> copy(s(cost), s(instruction))

        else -> {
          error("this really oughtta be impossible")
        }
      } as P
    }
  }
}
