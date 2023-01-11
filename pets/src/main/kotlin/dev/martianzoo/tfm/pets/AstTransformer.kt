package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.FromExpression
import dev.martianzoo.tfm.pets.ast.FromExpression.ComplexFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.TypeInFrom
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Script
import dev.martianzoo.tfm.pets.ast.Script.ScriptCommand
import dev.martianzoo.tfm.pets.ast.Script.ScriptCounter
import dev.martianzoo.tfm.pets.ast.Script.ScriptLine
import dev.martianzoo.tfm.pets.ast.Script.ScriptPragmaPlayer
import dev.martianzoo.tfm.pets.ast.Script.ScriptRequirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.toSetStrict

open class AstTransformer {
  fun <P : PetsNode> transform(nodes: List<P>) = nodes.map { transform(it) }
  fun <P : PetsNode> transform(nodes: Set<P>) = nodes.map { transform(it) }.toSetStrict()

  open fun <P : PetsNode?> transform(node: P): P {
    @Suppress("UNCHECKED_CAST")
    if (node == null) return null as P // TODO how'm I even getting away with this
    return (node as PetsNode).run {
      val rewritten = when (this) {
        is ClassName -> this

        is TypeExpression -> when (this) {
          is ClassLiteral -> ClassLiteral(x(className))
          is GenericTypeExpression -> GenericTypeExpression(x(className), x(specs), x(refinement))
        }

        is QuantifiedExpression -> QuantifiedExpression(x(expression), scalar)

        is Requirement -> when (this) {
          is Requirement.Min -> Requirement.Min(x(qe))
          is Requirement.Max -> Requirement.Max(x(qe))
          is Requirement.Exact -> Requirement.Exact(x(qe))
          is Requirement.Or -> Requirement.Or(x(requirements))
          is Requirement.And -> Requirement.And(x(requirements))
          is Requirement.Transform -> Requirement.Transform(x(requirement), transform)
        }

        is Instruction -> when (this) {
          is Instruction.Gain -> Instruction.Gain(x(qe), intensity)
          is Instruction.Remove -> Instruction.Remove(x(qe), intensity)
          is Instruction.Per -> Instruction.Per(x(instruction), x(qe))
          is Instruction.Gated -> Instruction.Gated(x(requirement), x(instruction))
          is Instruction.Transmute -> Instruction.Transmute(x(fromExpression), scalar)
          is Instruction.Custom -> Instruction.Custom(functionName, x(arguments))
          is Instruction.Then -> Instruction.Then(x(instructions))
          is Instruction.Or -> Instruction.Or(x(instructions))
          is Instruction.Multi -> Instruction.Multi(x(instructions))
          is Instruction.Transform -> Instruction.Transform(x(instruction), transform)
        }
        is FromExpression -> when (this) {
          is SimpleFrom -> SimpleFrom(x(toType), x(fromType))
          is ComplexFrom -> ComplexFrom(x(className), x(specializations), x(refinement))
          is TypeInFrom -> TypeInFrom(x(type))
        }

        is Effect -> Effect(x(trigger), x(instruction), automatic)
        is Trigger -> when (this) {
          is Trigger.OnGain -> Trigger.OnGain(x(expression))
          is Trigger.OnRemove -> Trigger.OnRemove(x(expression))
          is Trigger.Transform -> Trigger.Transform(x(trigger), transform)
        }

        is Action -> Action(x(cost), x(instruction))
        is Cost -> when (this) {
          is Cost.Spend -> Cost.Spend(x(qe))
          is Cost.Per -> Cost.Per(x(cost), x(qe))
          is Cost.Or -> Cost.Or(x(costs))
          is Cost.Multi -> Cost.Multi(x(costs))
          is Cost.Transform -> Cost.Transform(x(cost), transform)
        }

        is Script -> Script(x(lines))
        is ScriptLine -> when (this) {
          is ScriptCommand -> ScriptCommand(x(command), x(ownedBy))
          is ScriptCounter -> ScriptCounter(key, x(type))
          is ScriptRequirement -> ScriptRequirement(x(req))
          is ScriptPragmaPlayer -> ScriptPragmaPlayer(x(player))
        }
      }
      @Suppress("UNCHECKED_CAST")
      rewritten as P
    }
  }

  private fun <P : PetsNode?> x(node: P) = transform(node)
  private fun <P : PetsNode> x(nodes: List<P>) = transform(nodes)
  private fun <P : PetsNode> x(nodes: Set<P>) = transform(nodes)
}
