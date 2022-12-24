package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.toSetStrict

object Vanillafier {
    fun san(i: Int?): Int? {
        return when (i) {
            null -> null
            0 -> 0
            else -> 5
        }
    }

    fun <P : PetsNode> san(coll: List<P>) = coll.map { san(it) }.sortedBy { it.toString().length }
    fun <P : PetsNode> san(coll: Set<P>) = coll.map { san(it) }.sortedBy { it.toString().length }.toSetStrict()

    @Suppress("UNCHECKED_CAST")
    fun <P : PetsNode?> san(n: P): P {
        if (n == null) return null as P
        return n.apply {
            when (this) {
                is TypeExpression -> TypeExpression("Foo", san(specializations), san(requirement))
                is QuantifiedExpression -> QuantifiedExpression(san(typeExpression), san(scalar))

                is Requirement.Or -> Requirement.Or(san(requirements))
                is Requirement.And -> Requirement.And(san(requirements))
                is Requirement.Min -> copy(san(qe))
                is Requirement.Max -> copy(san(qe))
                is Requirement.Exact -> copy(san(qe))
                is Requirement.Prod -> copy(san(requirement))

                is Instruction.Gain -> copy(san(qe))
                is Instruction.Remove -> copy(san(qe))
                is Instruction.Gated -> copy(san(requirement), san(instruction))
                is Instruction.Then -> Instruction.Then(san(instructions))
                is Instruction.Or -> Instruction.Or(san(instructions))
                is Instruction.Multi -> Instruction.Multi(san(instructions))
                is Instruction.Transmute -> copy(san(trans), san(scalar))
                is Instruction.ComplexFrom -> copy("Foo", san(specializations), san(requirement))
                is Instruction.SimpleFrom -> copy(san(to), san(from))
                is Instruction.TypeInFrom -> copy(san(type))
                is Instruction.Per -> copy(san(instruction), san(qe))
                is Instruction.Prod -> copy(san(instruction))
                is Instruction.Custom -> copy("foo", san(arguments))

                is Effect.Trigger.OnGain -> copy(san(expression))
                is Effect.Trigger.OnRemove -> copy(san(expression))
                is Effect.Trigger.Now -> copy(san(requirement))
                is Effect.Trigger.Prod -> copy(san(trigger))
                is Effect -> copy(san(trigger), san(instruction))

                is Action.Cost.Spend -> copy(san(qe))
                is Action.Cost.Per -> copy(san(cost), san(qe))
                is Action.Cost.Or -> Action.Cost.Or(san(costs))
                is Action.Cost.Multi -> Action.Cost.Multi(san(costs))
                is Action.Cost.Prod -> copy(san(cost))
                is Action -> copy(san(cost), san(instruction))

                is Requirement -> TODO()
                is Instruction -> TODO()
                is Action.Cost -> TODO()
                is Instruction.FromExpression -> TODO()
                is Effect.Trigger -> TODO()

                else -> {
                    error("this really oughtta be impossible")
                }
            } as P
        }
    }
}
