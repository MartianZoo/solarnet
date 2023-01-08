package dev.martianzoo.tfm.testlib

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.FromExpression.ComplexFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.TypeInFrom
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.pre

internal object ToKotlin {
  fun <T : PetsNode?> T.pre(prefix: String): String = pre(prefix, ToKotlin::p2k)

  fun <T : PetsNode?> Iterable<T>.join(separator: CharSequence = ", "): String {
    return joinToString(separator) { p2k(it) }
  }

  fun p2k(n: PetsNode?): String {
    n.apply {
      return when (this) {
        null -> "null"

        is GenericTypeExpression -> "te(\"$className\"" +
            specs.joinOrEmpty(", ", prefix=", listOf(", suffix=")") { p2k(it) } +
            "${refinement.pre(", " + if (specs.isEmpty()) "requirement=" else "")})"
        is QuantifiedExpression -> "QuantifiedExpression(${p2k(expression)}${scalar.pre(", ")})"

        is Requirement.Min -> "Min(${p2k(qe.expression)}${qe.scalar.pre(", ")})"
        is Requirement.Max -> "Max(${p2k(qe.expression)}, ${qe.scalar})"
        is Requirement.Exact -> "Exact(${p2k(qe.expression)}, ${qe.scalar})"
        is Requirement.Or -> "Requirement.Or(${requirements.join()})"
        is Requirement.And -> "Requirement.And(${requirements.join()})"
        is Requirement.Prod -> "Requirement.Prod(${p2k(requirement)})"

        is Gain -> "Gain(${p2k(qe.expression)}${qe.scalar.pre(", ")}${intensity.pre(if (qe.scalar != null) ", " else ", intensity=")})"
        is Remove -> "Remove(${p2k(qe.expression)}${qe.scalar.pre(", ")}${intensity.pre(", ")})"
        is Instruction.Per -> "Instruction.Per(${p2k(instruction)}, ${p2k(qe)})"
        is Gated -> "Gated(${p2k(requirement)}, ${p2k(instruction)})"
        is Transmute -> "Transmute(${p2k(fromExpression)}${scalar.pre(", ")}${intensity.pre(", ")})"
        is ComplexFrom -> "ComplexFrom(\"$className\", listOf(${specializations.join()})${refinement.pre(", ")}"
        is SimpleFrom -> "SimpleFrom(${p2k(toType)}, ${p2k(fromType)})"
        is TypeInFrom -> "TypeInFrom(${p2k(type)})"
        is Custom -> "Instruction.Custom(\"$functionName\"${arguments.joinToString("") {", ${p2k(it)}"}})"
        is Then -> "Then(${instructions.join()})"
        is Instruction.Or -> "Instruction.Or(${instructions.join()})"
        is Instruction.Multi -> "Instruction.Multi(${instructions.join()})"
        is Instruction.Prod -> "Instruction.Prod(${p2k(instruction)})"

        is Trigger.OnGain -> "OnGain(${p2k(expression)})"
        is Trigger.OnRemove -> "OnRemove(${p2k(expression)})"
        is Trigger.Prod -> "Trigger.Prod(${p2k(trigger)})"
        is Effect -> "Effect(${p2k(trigger)}, ${p2k(instruction)}, $automatic)"

        is Cost.Spend -> "Spend(${p2k(qe.expression)}${qe.scalar.pre(", ")}"
        is Cost.Per -> "Cost.Per(${p2k(cost)}, ${p2k(qe)})"
        is Cost.Or -> "Cost.Or(${costs.join()})"
        is Cost.Multi -> "Cost.Multi(${costs.join()})"
        is Cost.Prod -> "Cost.Prod(${p2k(cost)})"
        is Action -> "Action(${p2k(cost)}, ${p2k(instruction)})"

        // I can't figure out wtf is missing... and all the classes are sealed
        else -> error("")
      }
    }
  }

}
