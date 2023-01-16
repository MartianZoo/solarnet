package dev.martianzoo.tfm.testlib

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.ClassName
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
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScalarAndType
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.pre

internal object PetToKotlin {
  fun <T : PetNode?> T.pre(prefix: String): String = pre(prefix, PetToKotlin::p2k)

  fun <T : PetNode?> Iterable<T>.join(separator: CharSequence = ", "): String {
    return joinToString(separator) { p2k(it) }
  }

  fun p2k(n: PetNode?): String {
    n.apply {
      return when (this) {
        null -> "null"

        is ClassName -> "cn($string)"
        is ClassLiteral -> "${p2k(className)}.literal"
        is GenericTypeExpression -> {
          var s = p2k(root)
          if (args.any()) {
            s += ".addArgs(${args.join()})"
          }
          if (refinement != null) {
            s += ".refine(${p2k(refinement)})"
          }
          s
        }

        is ScalarAndType -> "sat($scalar, ${p2k(type)})"

        is Requirement.Min -> "Min(${p2k(sat)})"
        is Requirement.Max -> "Max(${p2k(sat)})"
        is Requirement.Exact -> "Exact(${p2k(sat)})"
        is Requirement.Or -> "Requirement.Or(${requirements.join()})"
        is Requirement.And -> "Requirement.And(${requirements.join()})"
        is Requirement.Transform -> "Requirement.Transform(${p2k(requirement)}, \"$transform\")"

        is Gain -> "Gain(${p2k(sat)}, $intensity)"
        is Remove -> "Remove(${p2k(sat)}, $intensity)"
        is Instruction.Per -> "Instruction.Per(${p2k(instruction)}, ${p2k(sat)})"
        is Gated -> "Gated(${p2k(requirement)}, ${p2k(instruction)})"
        is Transmute -> "Transmute(${p2k(fromExpression)}${scalar.pre(", ")}${intensity.pre(", ")})"
        is ComplexFrom -> "ComplexFrom(c(\"$className\"), " + "listOf(${args.join()})${
          refinement.pre(", ")
        }"

        is SimpleFrom -> "SimpleFrom(${p2k(toType)}, ${p2k(fromType)})"
        is TypeInFrom -> "TypeInFrom(${p2k(type)})"
        is Custom -> "Instruction.Custom(\"$functionName\"" + "${arguments.joinToString("") { ", ${p2k(it)}" }})"
        is Then -> "Then(${instructions.join()})"
        is Instruction.Or -> "Instruction.Or(${instructions.join()})"
        is Instruction.Multi -> "Instruction.Multi(${instructions.join()})"
        is Instruction.Transform -> "Instruction.Transform(${p2k(instruction)}, \"$transform\")"

        is Trigger.OnGain -> "OnGain(${p2k(expression)})"
        is Trigger.OnRemove -> "OnRemove(${p2k(expression)})"
        is Trigger.Transform -> "Trigger.Transform(${p2k(trigger)}, \"$transform\")"
        is Effect -> "Effect(${p2k(trigger)}, ${p2k(instruction)}, $automatic)"

        is Cost.Spend -> "Spend(${p2k(sat)})"
        is Cost.Per -> "Cost.Per(${p2k(cost)}, ${p2k(sat)})"
        is Cost.Or -> "Cost.Or(${costs.join()})"
        is Cost.Multi -> "Cost.Multi(${costs.join()})"
        is Cost.Transform -> "Cost.Transform(${p2k(cost)}, \"$transform\")"
        is Action -> "Action(${p2k(cost)}, ${p2k(instruction)})"

        // I can't figure out wtf is missing... and all the classes are sealed
        else -> error("")
      }
    }
  }
}
