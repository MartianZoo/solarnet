package dev.martianzoo.tfm.petaform

import dev.martianzoo.tfm.petaform.Effect.Trigger

object PrettyPrinter {
  fun pp(instr: String): String {
    return pp(PetaformParser.parse<Instruction>(instr))
  }

  fun <T : Any?> T.surround(prefix: String, suffix: String, fn: (T) -> String = { "$it" }) =
      if (this == null) "" else "$prefix${fn(this)}$suffix"

  fun <T : Any?> T.pre(prefix: String, fn: (T) -> String = { "$it" }) = surround(prefix, "", fn)
  fun <T : Any?> T.suf(suffix: String, fn: (T) -> String = { "$it" }) = surround("", suffix, fn)

  fun <T : PetaformNode?> T.pre(prefix: String): String = pre(prefix, ::pp)

  fun pp(n: PetaformNode?): String {
    n.apply {
      return when (this) {
        null -> "null"

        is TypeExpression -> "TypeExpression(\"$className\"" +
            specializations.joinToString(", ", ", listOf(", ")") { pp(it) } +
            "${predicate.pre(", " + if (specializations.isEmpty()) "predicate=" else "")})"
        is QuantifiedExpression -> "QuantifiedExpression(${pp(typeExpression)}${scalar.pre(", ")})"

        is Predicate.Or -> "Predicate.or(${predicates.joinToString{pp(it)}})"
        is Predicate.And -> "Predicate.and(${predicates.joinToString{pp(it)}})"
        is Predicate.Min -> "Min(${pp(qe.typeExpression)}${qe.scalar.pre(", ")})"
        is Predicate.Max -> "Max(${pp(qe.typeExpression)}, ${qe.scalar})"
        is Predicate.Exact -> "Exact(${pp(qe.typeExpression)}, ${qe.scalar})"
        is Predicate.Prod -> "Predicate.Prod(${pp(predicate)})"

        is Instruction.Gain -> "Gain(${pp(qe.typeExpression)}${qe.scalar.pre(", ")}${intensity.pre(if (qe.scalar != null) ", " else ", intensity=")})"
        is Instruction.Remove -> "Remove(${pp(qe.typeExpression)}${qe.scalar.pre(", ")}${intensity.pre(", ")})"
        is Instruction.Gated -> "Gated(${pp(predicate)}, ${pp(instruction)})"
        is Instruction.Then -> "Instruction.then(${instructions.joinToString{pp(it)}})"
        is Instruction.Or -> "Instruction.or(${instructions.joinToString{pp(it)}})"
        is Instruction.Multi -> "Instruction.multi(${instructions.joinToString{pp(it)}})"
        is Instruction.Transmute -> "Transmute(${pp(trans)}${scalar.pre(", ")}${intensity.pre(", ")})"
        is Instruction.FromIsBelow -> "FromIsBelow(\"$className\", listOf(${specializations.joinToString{pp(it)}})${predicate.pre(", ")}"
        is Instruction.FromIsRightHere -> "FromIsRightHere(${pp(to)}, ${pp(from)})"
        is Instruction.FromIsNowhere -> "FromIsNowhere(${pp(type)})"
        is Instruction.Per -> "Instruction.Per(${pp(instruction)}, ${pp(qe)})"
        is Instruction.Prod -> "Instruction.Prod(${pp(instruction)})"
        is Instruction.Custom -> "Instruction.Custom(\"$name\", listOf(${pp(arguments.joinToString())}))"

        is Trigger.OnGain -> "OnGain(${pp(expression)})"
        is Trigger.OnRemove -> "OnRemove(${pp(expression)})"
        is Trigger.Conditional -> "Conditional(${pp(trigger)}, ${pp(predicate)})"
        is Trigger.Now -> "Now(${pp(predicate)})"
        is Trigger.Prod -> "Trigger.Prod(${pp(trigger)})"
        is Effect -> "Effect(${pp(trigger)}, ${pp(instruction)})"

        is Action.Cost.Spend -> "Spend(${pp(qe.typeExpression)}${qe.scalar.pre(", ")}"
        is Action.Cost.Per -> "Cost.Per(${pp(cost)}, ${pp(qe)})"
        is Action.Cost.Or -> "Action.or(${costs.joinToString{pp(it)}})"
        is Action.Cost.Multi -> "Action.and(${costs.joinToString{pp(it)}})"
        is Action.Cost.Prod -> "Action.Prod(${pp(cost)})"
        is Action -> "Action(${pp(cost)}, ${pp(instruction)})"

        // I can't figure out wtf is missing.. and all the classes are sealed
        else -> error("")
      }
    }
  }

}
