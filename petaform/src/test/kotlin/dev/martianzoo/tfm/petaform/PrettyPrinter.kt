package dev.martianzoo.tfm.petaform

import dev.martianzoo.tfm.petaform.Effect.Trigger

object PrettyPrinter {
  fun pp(instr: String): String {
    return pp(PetaformParser.parse<Instruction>(instr))
  }

  fun pp(n: PetaformNode?): String {
    n.apply {
      return when (this) {
        null -> "null"

        is TypeExpression -> "TypeExpression(\"$className\", listOf(${specializations.joinToString{pp(it)}})${if (predicate != null) ", " + pp(predicate) else ""})"
        is QuantifiedExpression -> "QuantifiedExpression(${pp(typeExpression)}, $scalar)"

        is Predicate.Or -> "Predicate.or(${predicates.joinToString{pp(it)}})"
        is Predicate.And -> "Predicate.and(${predicates.joinToString{pp(it)}})"
        is Predicate.Min -> "Min(${pp(qe.typeExpression)}, ${qe.scalar})"
        is Predicate.Max -> "Max(${pp(qe.typeExpression)}, ${qe.scalar})"
        is Predicate.Exact -> "Exact(${pp(qe.typeExpression)}, ${qe.scalar})"
        is Predicate.Prod -> "Predicate.Prod(${pp(predicate)})"

        is Instruction.Gain -> "Gain(${pp(qe.typeExpression)}, ${qe.scalar}, $intensity)"
        is Instruction.Remove -> "Remove(${pp(qe.typeExpression)}, ${qe.scalar}, $intensity)"
        is Instruction.Gated -> "Gated(${pp(predicate)}, ${pp(instruction)})"
        is Instruction.Then -> "Instruction.then(${instructions.joinToString{pp(it)}})"
        is Instruction.Or -> "Instruction.or(${instructions.joinToString{pp(it)}})"
        is Instruction.Multi -> "Instruction.multi(${instructions.joinToString{pp(it)}})"
        is Instruction.Transmute -> "Transmute(${pp(trans)}, $scalar, $intensity)"
        is Instruction.FromIsBelow -> "FromIsBelow(\"$className\", listOf(${specializations.joinToString{pp(it)}})${if (predicate != null) ", " + pp(predicate) else ""})"
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

        is Action.Cost.Spend -> "Spend(qe.typeExpression, qe.scalar)"
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
