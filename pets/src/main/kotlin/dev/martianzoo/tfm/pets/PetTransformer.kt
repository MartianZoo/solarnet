package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.From
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScalarAndType
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.util.toSetStrict

public abstract class PetTransformer {

  companion object {
    public fun <P : PetNode> P.transform(v: PetTransformer): P = v.doTransform(this)
    public fun <P : PetNode> Iterable<P>.transform(v: PetTransformer): List<P> = map(v::doTransform)
    public fun <P : PetNode> Set<P>.transform(v: PetTransformer): Set<P> =
        (this as Iterable<P>).transform(v).toSetStrict()
  }

  protected abstract fun <P : PetNode> doTransform(node: P): P

  protected fun <P : PetNode> defaultTransform(node: P): P {
    return (node as PetNode).run {
      val rewritten =
          when (this) {
            is ClassName -> this
            is TypeExpr -> TypeExpr(x(root), x(args), x(refinement), link)
            is ScalarAndType -> ScalarAndType(scalar, x(typeExpr))
            is Requirement ->
                when (this) {
                  is Requirement.Min -> Requirement.Min(x(sat))
                  is Requirement.Max -> Requirement.Max(x(sat))
                  is Requirement.Exact -> Requirement.Exact(x(sat))
                  is Requirement.Or -> Requirement.Or(x(requirements))
                  is Requirement.And -> Requirement.And(x(requirements))
                  is Requirement.Transform -> Requirement.Transform(x(requirement), transform)
                }
            is Instruction ->
                when (this) {
                  is Instruction.Gain -> Instruction.Gain(x(sat), intensity)
                  is Instruction.Remove -> Instruction.Remove(x(sat), intensity)
                  is Instruction.Per -> Instruction.Per(x(instruction), x(sat))
                  is Instruction.Gated -> Instruction.Gated(x(gate), x(instruction))
                  is Instruction.Transmute -> Instruction.Transmute(x(from), scalar)
                  is Instruction.Custom -> Instruction.Custom(functionName, x(arguments))
                  is Instruction.Then -> Instruction.Then(x(instructions))
                  is Instruction.Or -> Instruction.Or(x(instructions))
                  is Instruction.Multi -> Instruction.Multi(x(instructions))
                  is Instruction.Transform -> Instruction.Transform(x(instruction), transform)
                }
            is From ->
                when (this) {
                  is From.SimpleFrom -> From.SimpleFrom(x(toType), x(fromType))
                  is From.ComplexFrom -> From.ComplexFrom(x(className), x(arguments), x(refinement))
                  is From.TypeAsFrom -> From.TypeAsFrom(x(typeExpr))
                }
            is Effect -> Effect(x(trigger), x(instruction), automatic)
            is Trigger ->
                when (this) {
                  is Trigger.OnGain -> Trigger.OnGain(x(typeExpr))
                  is Trigger.OnRemove -> Trigger.OnRemove(x(typeExpr))
                  is Trigger.Transform -> Trigger.Transform(x(trigger), transform)
                }
            is Action -> Action(x(cost), x(instruction))
            is Cost ->
                when (this) {
                  is Cost.Spend -> Cost.Spend(x(sat))
                  is Cost.Per -> Cost.Per(x(cost), x(sat))
                  is Cost.Or -> Cost.Or(x(costs))
                  is Cost.Multi -> Cost.Multi(x(costs))
                  is Cost.Transform -> Cost.Transform(x(cost), transform)
                }
          }
      @Suppress("UNCHECKED_CAST")
      rewritten as P
    }
  }

  protected fun <P : PetNode?> x(node: P): P {
    @Suppress("UNCHECKED_CAST") return node?.let(::doTransform) as P
  }

  protected fun <P : PetNode> x(nodes: Iterable<P>): List<P> = nodes.map(::x)
  protected fun <P : PetNode> x(nodes: Set<P>): Set<P> = x(nodes as Iterable<P>).toSetStrict()
}
