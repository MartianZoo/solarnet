package dev.martianzoo.tfm.testlib

import dev.martianzoo.tfm.pets.PetsParser
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.Action.Cost.Spend
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.ComplexFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.Instruction.TypeInFrom
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.Requirement.Or
import dev.martianzoo.tfm.pets.ast.Requirement.Prod
import dev.martianzoo.tfm.pets.ast.TypeExpression

object ToKotlin {
  fun pp(instr: String): String {
    return pp(PetsParser.parse<Instruction>(instr))
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
        is QuantifiedExpression -> "QuantifiedExpression(${pp(type)}${scalar.pre(", ")})"

        is Min -> "Min(${pp(qe.type)}${qe.scalar.pre(", ")})"
        is Max -> "Max(${pp(qe.type)}, ${qe.scalar})"
        is Exact -> "Exact(${pp(qe.type)}, ${qe.scalar})"
        is Or -> "Requirement.or(${requirements.joinToString{ pp(it) }})"
        is And -> "Requirement.and(${requirements.joinToString{ pp(it) }})"
        is Prod -> "Requirement.Prod(${pp(requirement)})"

        is Gain -> "Gain(${pp(qe.type)}${qe.scalar.pre(", ")}${intensity.pre(if (qe.scalar != null) ", " else ", intensity=")})"
        is Remove -> "Remove(${pp(qe.type)}${qe.scalar.pre(", ")}${intensity.pre(", ")})"
        is Per -> "Instruction.Per(${pp(instruction)}, ${pp(qe)})"
        is Gated -> "Gated(${pp(requirement)}, ${pp(instruction)})"
        is Transmute -> "Transmute(${pp(fromExpression)}${scalar.pre(", ")}${intensity.pre(", ")})"
        is ComplexFrom -> "ComplexFrom(\"$className\", listOf(${specializations.joinToString{ pp(it) }})${requirement.pre(", ")}"
        is SimpleFrom -> "SimpleFrom(${pp(toType)}, ${pp(fromType)})"
        is TypeInFrom -> "TypeInFrom(${pp(type)})"
        is Custom -> "Instruction.Custom(\"$functionName\", listOf(${pp(arguments.joinToString())}))"
        is Then -> "Instruction.then(${instructions.joinToString{ pp(it) }})"
        is Instruction.Or -> "Instruction.or(${instructions.joinToString{ pp(it) }})"
        is Multi -> "Instruction.multi(${instructions.joinToString{ pp(it) }})"
        is Instruction.Prod -> "Instruction.Prod(${pp(instruction)})"

        is Trigger.OnGain -> "OnGain(${pp(expression)})"
        is Trigger.OnRemove -> "OnRemove(${pp(expression)})"
        is Trigger.Prod -> "Trigger.Prod(${pp(trigger)})"
        is Effect -> "Effect(${pp(trigger)}, ${pp(instruction)})"

        is Spend -> "Spend(${pp(qe.type)}${qe.scalar.pre(", ")}"
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
