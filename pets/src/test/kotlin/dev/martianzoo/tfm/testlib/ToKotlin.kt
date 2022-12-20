package dev.martianzoo.tfm.testlib

import dev.martianzoo.tfm.pets.Action
import dev.martianzoo.tfm.pets.Action.Cost
import dev.martianzoo.tfm.pets.Action.Cost.Spend
import dev.martianzoo.tfm.pets.Effect
import dev.martianzoo.tfm.pets.Effect.Trigger
import dev.martianzoo.tfm.pets.Instruction
import dev.martianzoo.tfm.pets.Instruction.ComplexFrom
import dev.martianzoo.tfm.pets.Instruction.Custom
import dev.martianzoo.tfm.pets.Instruction.Gain
import dev.martianzoo.tfm.pets.Instruction.Gated
import dev.martianzoo.tfm.pets.Instruction.Multi
import dev.martianzoo.tfm.pets.Instruction.Per
import dev.martianzoo.tfm.pets.Instruction.Remove
import dev.martianzoo.tfm.pets.Instruction.SimpleFrom
import dev.martianzoo.tfm.pets.Instruction.Then
import dev.martianzoo.tfm.pets.Instruction.Transmute
import dev.martianzoo.tfm.pets.Instruction.TypeInFrom
import dev.martianzoo.tfm.pets.Parser
import dev.martianzoo.tfm.pets.PetsNode
import dev.martianzoo.tfm.pets.QuantifiedExpression
import dev.martianzoo.tfm.pets.Requirement.And
import dev.martianzoo.tfm.pets.Requirement.Exact
import dev.martianzoo.tfm.pets.Requirement.Max
import dev.martianzoo.tfm.pets.Requirement.Min
import dev.martianzoo.tfm.pets.Requirement.Or
import dev.martianzoo.tfm.pets.Requirement.Prod
import dev.martianzoo.tfm.pets.TypeExpression

object ToKotlin {
  fun pp(instr: String): String {
    return pp(Parser.parse<Instruction>(instr))
  }

  fun <T : Any?> T.surround(prefix: String, suffix: String, fn: (T) -> String = { "$it" }) =
      if (this == null) "" else "$prefix${fn(this)}$suffix"

  fun <T : Any?> T.pre(prefix: String, fn: (T) -> String = { "$it" }) = surround(prefix, "", fn)
  fun <T : Any?> T.suf(suffix: String, fn: (T) -> String = { "$it" }) = surround("", suffix, fn)

  fun <T : PetsNode?> T.pre(prefix: String): String = pre(prefix, ToKotlin::pp)

  fun pp(n: PetsNode?): String {
    n.apply {
      return when (this) {
        null -> "null"

        is TypeExpression -> "TypeExpression(\"$className\"" +
            specializations.joinToString(", ", ", listOf(", ")") { pp(it) } +
            "${requirement.pre(", " + if (specializations.isEmpty()) "requirement=" else "")})"
        is QuantifiedExpression -> "QuantifiedExpression(${pp(typeExpression)}${scalar.pre(", ")})"

        is Or -> "Requirement.or(${requirements.joinToString{ pp(it) }})"
        is And -> "Requirement.and(${requirements.joinToString{ pp(it) }})"
        is Min -> "Min(${pp(qe.typeExpression)}${qe.scalar.pre(", ")})"
        is Max -> "Max(${pp(qe.typeExpression)}, ${qe.scalar})"
        is Exact -> "Exact(${pp(qe.typeExpression)}, ${qe.scalar})"
        is Prod -> "Requirement.Prod(${pp(requirement)})"

        is Gain -> "Gain(${pp(qe.typeExpression)}${qe.scalar.pre(", ")}${intensity.pre(if (qe.scalar != null) ", " else ", intensity=")})"
        is Remove -> "Remove(${pp(qe.typeExpression)}${qe.scalar.pre(", ")}${intensity.pre(", ")})"
        is Gated -> "Gated(${pp(requirement)}, ${pp(instruction)})"
        is Then -> "Instruction.then(${instructions.joinToString{ pp(it) }})"
        is Instruction.Or -> "Instruction.or(${instructions.joinToString{ pp(it) }})"
        is Multi -> "Instruction.multi(${instructions.joinToString{ pp(it) }})"
        is Transmute -> "Transmute(${pp(trans)}${scalar.pre(", ")}${intensity.pre(", ")})"
        is ComplexFrom -> "ComplexFrom(\"$className\", listOf(${specializations.joinToString{ pp(it) }})${requirement.pre(", ")}"
        is SimpleFrom -> "SimpleFrom(${pp(to)}, ${pp(from)})"
        is TypeInFrom -> "TypeInFrom(${pp(type)})"
        is Per -> "Instruction.Per(${pp(instruction)}, ${pp(qe)})"
        is Instruction.Prod -> "Instruction.Prod(${pp(instruction)})"
        is Custom -> "Instruction.Custom(\"$name\", listOf(${pp(arguments.joinToString())}))"

        is Trigger.OnGain -> "OnGain(${pp(expression)})"
        is Trigger.OnRemove -> "OnRemove(${pp(expression)})"
        is Trigger.Now -> "Now(${pp(requirement)})"
        is Trigger.Prod -> "Trigger.Prod(${pp(trigger)})"
        is Effect -> "Effect(${pp(trigger)}, ${pp(instruction)})"

        is Spend -> "Spend(${pp(qe.typeExpression)}${qe.scalar.pre(", ")}"
        is Cost.Per -> "Cost.Per(${pp(cost)}, ${pp(qe)})"
        is Cost.Or -> "Action.or(${costs.joinToString{ pp(it) }})"
        is Cost.Multi -> "Action.and(${costs.joinToString{ pp(it) }})"
        is Cost.Prod -> "Action.Prod(${pp(cost)})"
        is Action -> "Action(${pp(cost)}, ${pp(instruction)})"

        // I can't figure out wtf is missing.. and all the classes are sealed
        else -> error("")
      }
    }
  }

}
