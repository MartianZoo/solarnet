package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.Action
import dev.martianzoo.tfm.pets.Component
import dev.martianzoo.tfm.pets.Effect
import dev.martianzoo.tfm.pets.Instruction
import dev.martianzoo.tfm.pets.PetsNode
import dev.martianzoo.tfm.pets.Predicate
import dev.martianzoo.tfm.pets.QuantifiedExpression
import dev.martianzoo.tfm.pets.TypeExpression

object Vanillafier {
    fun san(i: Int?): Int? {
        return when (i) {
            null -> null
            0 -> 0
            else -> 5
        }
    }

    fun <P : PetsNode> san(coll: Iterable<P>) = coll.map { san(it) }.sortedBy { it.toString().length }

    fun <P : PetsNode?> san(n: P): P {
        if (n == null) return null as P
        return n.apply {
            when (this) {
                is TypeExpression -> TypeExpression("Foo", san(specializations), san(predicate))
                is QuantifiedExpression -> QuantifiedExpression(san(typeExpression), san(scalar))

                is Predicate.Or -> Predicate.or(san(predicates))
                is Predicate.And -> Predicate.and(san(predicates))
                is Predicate.Min -> copy(san(qe))
                is Predicate.Max -> copy(san(qe))
                is Predicate.Exact -> copy(san(qe))
                is Predicate.Prod -> copy(san(predicate))

                is Instruction.Gain -> copy(san(qe))
                is Instruction.Remove -> copy(san(qe))
                is Instruction.Gated -> copy(san(predicate), san(instruction))
                is Instruction.Then -> Instruction.then(san(instructions))
                is Instruction.Or -> Instruction.or(san(instructions))
                is Instruction.Multi -> Instruction.multi(san(instructions))
                is Instruction.Transmute -> copy(san(trans), san(scalar))
                is Instruction.ComplexFrom -> copy("Foo", san(specializations), san(predicate))
                is Instruction.SimpleFrom -> copy(san(to), san(from))
                is Instruction.TypeInFrom -> copy(san(type))
                is Instruction.Per -> copy(san(instruction), san(qe))
                is Instruction.Prod -> copy(san(instruction))
                is Instruction.Custom -> copy("foo", san(arguments))

                is Effect.Trigger.OnGain -> copy(san(expression))
                is Effect.Trigger.OnRemove -> copy(san(expression))
                is Effect.Trigger.Conditional -> copy(san(trigger), san(predicate))
                is Effect.Trigger.Now -> copy(san(predicate))
                is Effect.Trigger.Prod -> copy(san(trigger))
                is Effect -> copy(san(trigger), san(instruction))

                is Action.Cost.Spend -> copy(san(qe))
                is Action.Cost.Per -> copy(san(cost), san(qe))
                is Action.Cost.Or -> Action.Cost.or(san(costs))
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
