package dev.martianzoo.tfm.testlib

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.From
import dev.martianzoo.tfm.pets.ast.From.ComplexFrom
import dev.martianzoo.tfm.pets.ast.From.ExpressionAsFrom
import dev.martianzoo.tfm.pets.ast.From.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledExpression
import dev.martianzoo.util.iff
import dev.martianzoo.util.pre
import dev.martianzoo.util.wrap

internal object PetToKotlin {
  private fun <T : PetNode?> T.pre(prefix: String): String = pre(prefix, PetToKotlin::p2k)

  private fun <T : PetNode?> Iterable<T>.join(separator: CharSequence = ", "): String {
    return joinToString(separator) { p2k(it) }
  }

  fun p2k(n: PetNode?): String {
    n.apply {
      return when (this) {
        null -> "null"
        is ClassName -> "cn(\"$this\")"
        is Expression -> {
          p2k(className) +
              (if (arguments.none()) ".type" else ".addArgs(${arguments.join()})") +
              refinement?.let(::p2k).wrap(".refine(", ")")
        }

        is ScaledExpression -> {
          if (scalar == 1) {
            "scaledEx(${p2k(expression)})"
          } else {
            "scaledEx($scalar${p2k(expression).pre(", ")})"
          }
        }

        is Metric -> {
          when (this) {
            is Metric.Count -> "Count(${p2k(scaledEx)})"
            is Metric.Max -> "Metric.Max(${p2k(metric)}, $maximum)"
          }
        }

        is Requirement -> {
          when (this) {
            is Requirement.Min -> "Min(${p2k(scaledEx)})"
            is Requirement.Max -> "Requirement.Max(${p2k(scaledEx)})"
            is Requirement.Exact -> "Exact(${p2k(scaledEx)})"
            is Requirement.Or -> "Requirement.Or(${requirements.join()})"
            is Requirement.And -> "Requirement.And(${requirements.join()})"
            is Requirement.Transform ->
              "Requirement.Transform(${p2k(requirement)}, \"$transformKind\")"
          }
        }
        is Instruction -> {
          when (this) {
            is Gain -> "Gain(${p2k(scaledEx)}${intensity.pre(", ")})"
            is Remove -> "Remove(${p2k(scaledEx)}${intensity.pre(", ")})"
            is Instruction.Per -> "Instruction.Per(${p2k(instruction)}, ${p2k(metric)})"
            is Gated -> "Gated(${p2k(gate)}, ${p2k(instruction)})"
            is Transmute -> "Transmute(${p2k(from)}, $count${intensity.pre(", ")})"
            is Custom ->
                "Instruction.Custom(\"$functionName\"" +
                    "${arguments.joinToString("") { ", ${p2k(it)}" }})"
            is Then -> "Then(${instructions.join()})"
            is Instruction.Or -> "Instruction.Or(${instructions.join()})"
            is Instruction.Multi -> "Instruction.Multi(${instructions.join()})"
            is Instruction.Transform ->
                "Instruction.Transform(${p2k(instruction)}, \"$transformKind\")"
          }
        }
        is From -> {
          when (this) {
            is ComplexFrom ->
                "ComplexFrom(cn(\"$className\")," +
                    " listOf(${arguments.join()})${refinement.pre(", ")})"
            is SimpleFrom -> "SimpleFrom(${p2k(toExpression)}, ${p2k(fromExpression)})"
            is ExpressionAsFrom -> "ExpressionAsFrom(${p2k(expression)})"
          }
        }
        is Trigger -> {
          when (this) {
            is Trigger.WhenGain -> "WhenGain"
            is Trigger.WhenRemove -> "WhenRemove"
            is Trigger.OnGainOf -> "OnGainOf(${p2k(expression)})"
            is Trigger.OnRemoveOf -> "OnRemoveOf(${p2k(expression)})"
            is Trigger.ByTrigger -> "ByTrigger(${p2k(inner)}, ${p2k(by)})"
            is Trigger.Transform -> "Trigger.Transform(${p2k(trigger)}, \"$transformKind\")"
          }
        }
        is Effect -> "Effect(${p2k(trigger)}, ${p2k(instruction)}${", true".iff(automatic)})"
        is Cost -> {
          when (this) {
            is Cost.Spend -> "Spend(${p2k(scaledEx)})"
            is Cost.Per -> "Cost.Per(${p2k(cost)}, ${p2k(metric)})"
            is Cost.Or -> "Cost.Or(${costs.join()})"
            is Cost.Multi -> "Cost.Multi(${costs.join()})"
            is Cost.Transform -> "Cost.Transform(${p2k(cost)}, \"$transformKind\")"
          }
        }
        is Action -> "Action(${p2k(cost)}, ${p2k(instruction)})"
      }
    }
  }
}
