package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.petaform.Action
import dev.martianzoo.tfm.petaform.Component
import dev.martianzoo.tfm.petaform.Effect
import dev.martianzoo.tfm.petaform.Instruction
import dev.martianzoo.tfm.petaform.PetaformNode
import dev.martianzoo.tfm.petaform.Predicate
import dev.martianzoo.tfm.petaform.QuantifiedExpression
import dev.martianzoo.tfm.petaform.TypeExpression

object Vanillafier {
    fun san(i: Int?): Int? {
        return when (i) {
            null -> null
            0 -> 0
            else -> 5
        }
    }

    fun <P : PetaformNode> san(coll: Iterable<P>) = coll.map { san(it) }.sortedBy { it.toString().length }

    fun <P : PetaformNode?> san(n: P): P {
        if (n == null) return null as P
        return n.apply {
            when (this) {
                is TypeExpression -> TypeExpression("Foo", san(specializations), san(predicate))
                is QuantifiedExpression -> QuantifiedExpression(san(typeExpression), san(scalar))

                is Predicate.Or -> Predicate.Or(san(predicates))
                is Predicate.And -> Predicate.And(san(predicates))
                is Predicate.Min -> Predicate.Min(san(qe))
                is Predicate.Max -> Predicate.Max(san(qe))
                is Predicate.Exact -> Predicate.Exact(san(qe))
                is Predicate.Prod -> Predicate.Prod(san(predicate))

                is Instruction.Gain -> copy(san(qe))
                is Instruction.Remove -> copy(san(qe))
                is Instruction.Gated -> Instruction.Gated(san(predicate), san(instruction))
                is Instruction.Then -> Instruction.Then(san(instructions))
                is Instruction.Or -> Instruction.Or(san(instructions))
                is Instruction.Multi -> Instruction.Multi(san(instructions))
                is Instruction.Transmute -> copy(san(trans), san(scalar))
                is Instruction.FromIsBelow -> Instruction.FromIsBelow("Foo", san(specializations), san(predicate))
                is Instruction.FromIsRightHere -> Instruction.FromIsRightHere(san(to), san(from))
                is Instruction.FromIsNowhere -> Instruction.FromIsNowhere(san(type))
                is Instruction.Per -> Instruction.Per(san(instruction), san(qe))
                is Instruction.Prod -> Instruction.Prod(san(instruction))
                is Instruction.Custom -> Instruction.Custom("foo", san(arguments))

                is Effect.Trigger.OnGain -> copy(san(expression))
                is Effect.Trigger.OnRemove -> copy(san(expression))
                is Effect.Trigger.Conditional -> copy(san(trigger), san(predicate))
                is Effect.Trigger.Now -> Effect.Trigger.Now(san(predicate))
                is Effect.Trigger.Prod -> copy(san(trigger))
                is Effect -> copy(san(trigger), san(instruction))

                is Action.Cost.Spend -> copy(san(qe))
                is Action.Cost.Per -> copy(san(cost), san(qe))
                is Action.Cost.Or -> copy(san(costs))
                is Action.Cost.Multi -> copy(san(costs))
                is Action.Cost.Prod -> copy(san(cost))
                is Action -> copy(san(cost), san(instruction))

                is Predicate -> TODO()
                is Instruction -> TODO()
                is Action.Cost -> TODO()
                is Instruction.FromExpression -> TODO()
                is Effect.Trigger -> TODO()
                is Component -> TODO()

                else -> {
                    error("this really oughtta be impossible")
                }
            } as P
        }
    }
}
