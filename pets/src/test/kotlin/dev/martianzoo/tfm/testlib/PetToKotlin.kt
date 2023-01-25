package dev.martianzoo.tfm.testlib

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.From.ComplexFrom
import dev.martianzoo.tfm.pets.ast.From.SimpleFrom
import dev.martianzoo.tfm.pets.ast.From.TypeAsFrom
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
import dev.martianzoo.tfm.pets.ast.TypeExpr.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpr.GenericTypeExpr
import dev.martianzoo.util.iff
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
        is ClassName -> "cn(\"$this\")"
        is ClassLiteral -> "${p2k(className)}.literal"
        is GenericTypeExpr -> {
          var s = p2k(root)
          if (args.any()) {
            s += ".addArgs(${args.join()})"
          } else {
            s += ".type"
          }
          if (refinement != null) {
            s += ".refine(${p2k(refinement)})"
          }
          s
        }

        is ScalarAndType -> {
          if (scalar == 1) {
            "sat(${p2k(typeExpr)})"
          } else {
            "sat($scalar${p2k(typeExpr).pre(", ")})"
          }
        }
        is Requirement.Min -> "Min(${p2k(sat)})"
        is Requirement.Max -> "Max(${p2k(sat)})"
        is Requirement.Exact -> "Exact(${p2k(sat)})"
        is Requirement.Or -> "Requirement.Or(${requirements.join()})"
        is Requirement.And -> "Requirement.And(${requirements.join()})"
        is Requirement.Transform -> "Requirement.Transform(${p2k(requirement)}, \"$transform\")"
        is Gain -> "Gain(${p2k(sat)}${intensity.pre(", ")})"
        is Remove -> "Remove(${p2k(sat)}${intensity.pre(", ")})"
        is Instruction.Per -> "Instruction.Per(${p2k(instruction)}, ${p2k(sat)})"
        is Gated -> "Gated(${p2k(gate)}, ${p2k(instruction)})"
        is Transmute -> "Transmute(${p2k(from)}, $count${intensity.pre(", ")})"
        is ComplexFrom ->
            "ComplexFrom(cn(\"$className\"), listOf(${arguments.join()})${refinement.pre(", ")})"

        is SimpleFrom -> "SimpleFrom(${p2k(toType)}, ${p2k(fromType)})"
        is TypeAsFrom -> "TypeAsFrom(${p2k(typeExpr)})"
        is Custom ->
          "Instruction.Custom(\"$functionName\"" +
              "${arguments.joinToString("") { ", ${p2k(it)}" }})"

        is Then -> "Then(${instructions.join()})"
        is Instruction.Or -> "Instruction.Or(${instructions.join()})"
        is Instruction.Multi -> "Instruction.Multi(${instructions.join()})"
        is Instruction.Transform -> "Instruction.Transform(${p2k(instruction)}, \"$transform\")"
        is Trigger.OnGain -> "OnGain(${p2k(typeExpr)})"
        is Trigger.OnRemove -> "OnRemove(${p2k(typeExpr)})"
        is Trigger.Transform -> "Trigger.Transform(${p2k(trigger)}, \"$transform\")"
        is Effect -> "Effect(${p2k(trigger)}, ${p2k(instruction)}${", true".iff(automatic)})"
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
