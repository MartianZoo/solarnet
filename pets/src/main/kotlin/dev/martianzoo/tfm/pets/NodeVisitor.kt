package dev.martianzoo.tfm.pets

import dev.martianzoo.util.toSetCareful

open class NodeVisitor {
  private fun <P : PetsNode> s(ns: List<P>) = ns.map { s(it) }
  private fun <P : PetsNode> s(ns: Set<P>) = ns.map { s(it) }.toSetCareful()

  open fun <P : PetsNode?> s(node: P): P {
    if (node == null) return null as P
    return node.run {
      when (this) {
        is TypeExpression -> TypeExpression(className, s(specializations), s(predicate))
        is QuantifiedExpression -> QuantifiedExpression(s(typeExpression), scalar)

        is Predicate.Or -> Predicate.or(s(predicates))
        is Predicate.And -> Predicate.and(s(predicates))
        is Predicate.Min -> copy(s(qe))
        is Predicate.Max -> copy(s(qe))
        is Predicate.Exact -> copy(s(qe))
        is Predicate.Prod -> copy(s(predicate))

        is Instruction.Gain -> copy(s(qe))
        is Instruction.Remove -> copy(s(qe))
        is Instruction.Gated -> copy(s(predicate), s(instruction))
        is Instruction.Then -> Instruction.then(s(instructions))
        is Instruction.Or -> Instruction.or(s(instructions))
        is Instruction.Multi -> Instruction.multi(s(instructions))
        is Instruction.Transmute -> copy(s(trans), scalar)
        is Instruction.ComplexFrom -> copy(className, s(specializations), s(predicate))
        is Instruction.SimpleFrom -> copy(s(to), s(from))
        is Instruction.TypeInFrom -> copy(s(type))
        is Instruction.Per -> copy(s(instruction), s(qe))
        is Instruction.Prod -> copy(s(instruction))
        is Instruction.Custom -> copy(name, s(arguments))

        is Effect.Trigger.OnGain -> copy(s(expression))
        is Effect.Trigger.OnRemove -> copy(s(expression))
        is Effect.Trigger.Conditional -> copy(s(trigger), s(predicate))
        is Effect.Trigger.Now -> copy(s(predicate))
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
