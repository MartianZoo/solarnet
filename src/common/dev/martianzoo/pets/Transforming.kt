package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.api.SystemClasses.OWNER
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.api.SystemClasses.USE_ACTION
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar

/**
 * Small context-free functions for transforming Pets syntax trees. [PetElaborator] owns the
 * Class-table-dependent transformation packages.
 */
public object Transforming {
  // TODO: Move Terraforming Mars payment lowering into tfm-canon.
  private val standardResourceClasses: Set<ClassName> =
      setOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat").mapTo(linkedSetOf(), ::cn)

  /**
   * Replaces each occurrence of the special `This` expression with [contextType], replacing
   * `Class<This>` with the class literal for the context's class as well. An explicitly specialized
   * `This<Foo>` keeps its authored arguments and adopts the context's class, becoming (for example)
   * `Bar<Foo>`. This is
   * [rule L12-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration).
   */
  public fun replaceThisExpressionsWith(contextType: Expression): PetTransformer =
      chain(
          replacer(THIS.classExpression(), contextType.className.classExpression()),
          object : PetTransformer() {
            override fun transformNode(node: PetNode): PetNode {
              if (node is Expression && node.isBare(THIS)) return contextType
              val transformed = transformChildren(node)
              return if (transformed is Expression && transformed.className == THIS) {
                transformed.copy(className = contextType.className)
              } else {
                transformed
              }
            }
          },
      )

  /**
   * Replaces each occurrence of the contextual `Owner` placeholder with [owner], except inside any
   * subtree [shielded] accepts. An Owner-selecting fanout shields its body because the selection
   * supplies the owner there instead, so an ordinary owned body reads on a card exactly as it does
   * anywhere else ([rules
   * L3-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-expressions)
   * and
   * [L12-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-elaboration)).
   * `Anyone` is an ordinary class and stands for itself.
   */
  public fun replaceOwnerWith(
      owner: HasClassName,
      shielded: (PetNode) -> Boolean = { false },
  ): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (shielded(node)) {
            // Only the body is shielded. A selector still names components in the enclosing
            // context, so `EACH ProjectCard<Owner>` means the cards this component's owner holds.
            return if (node is Each) node.copy(selector = transformExpression(node.selector))
            else node
          }
          if (
              node is Expression &&
                  node.className == OWNER &&
                  node.arguments.isEmpty() &&
                  node.refinement == null
          ) {
            return node.copy(className = owner.className)
          }
          return transformChildren(node)
        }
      }

  /**
   * Replaces every authored X scalar with [value], retaining written coefficients: `X Plant THEN 2X
   * Heat` bound to 3 becomes `3 Plant THEN 6 Heat`, since `X` takes one value everywhere it appears
   * ([rule
   * L7-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   */
  public fun bindXTo(value: Int): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode =
            if (node is Scalar) node.bindX(value) else transformChildren(node)
      }

  internal fun actionToEffect(action: Action, index1Ref: Int): Effect {
    val whichAction = actionSelector(index1Ref)
    val instruction = action.toInstruction()
    val trigger = OnGainOf.create(USE_ACTION.of(THIS, whichAction))
    return Effect(trigger, instruction, automatic = false)
  }

  /**
   * Lowers each of [actions] to the effect keyed by its position on the class: the nth action is
   * triggered by `UseAction<This, ActionN>` ([rule
   * L9-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-actions)).
   *
   * The standard-resource cost rewrite that rides along here is `ACTIONS.md`'s subject, not this
   * module's; see the TODO above.
   */
  public fun actionListToEffects(actions: Collection<Action>): List<Effect> =
      actions.withIndex().flatMap { (index0Ref, action) ->
        actionToEffects(action, index1Ref = index0Ref + 1)
      }

  private fun actionToEffects(action: Action, index1Ref: Int): List<Effect> {
    val (spend, metric) =
        when (val cost = action.cost) {
          is Action.Cost.Spend -> cost to null
          is Action.Cost.Per -> (cost.cost as? Action.Cost.Spend)?.let { it to cost.metric }
          else -> null
        } ?: return listOf(actionToEffect(action, index1Ref))
    if (spend.scaledEx.expression.className !in standardResourceClasses) {
      return listOf(actionToEffect(action, index1Ref))
    }

    val selector = actionSelector(index1Ref)
    val metricText =
        when (metric) {
          is Property -> if (metric.receiver == null) "This.$metric" else "$metric"
          else -> "$metric"
        }
    val owed =
        "${spend.scaledEx.scalar} Owed<Class<${spend.scaledEx.expression}>>" +
            if (metric == null) "" else " / $metricText"
    val invoiceResource =
        if (spend.scaledEx.expression.className == cn("MC")) ""
        else ", Class<${spend.scaledEx.expression}>"
    if (spend.scaledEx.scalar is XScalar) {
      return listOf(
          parse(
              "UseAction<This, $selector>: $owed THEN " +
                  "Invoice<This, $selector$invoiceResource> THEN " +
                  "MAX 0 Invoice: (${action.instruction})"
          )
      )
    }

    return listOf(
        parse(
            "UseAction<This, $selector>: $owed THEN " + "Invoice<This, $selector$invoiceResource>"
        ),
        parse("-Invoice<This, $selector>: " + action.instruction),
    )
  }

  /** The position markers `Action1`..`ActionN` keying [actions] to their effects. */
  public fun actionSelectors(actions: Collection<Action>): Set<ClassName> =
      actions.indices.mapTo(linkedSetOf()) { actionSelector(it + 1) }

  // Rule L9-4: a class may offer at most three actions.
  private fun actionSelector(index1Ref: Int): ClassName =
      listOf(cn("Action1"), cn("Action2"), cn("Action3")).getOrNull(index1Ref - 1)
          ?: throw IllegalArgumentException("A component can offer only three actions: $index1Ref")

  /**
   * Returns the effect `This: instruction`, which is how a card's "do this now" section becomes an
   * ordinary rule ([rule
   * L9-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#9-actions)).
   * An immediate `Ok` produces no effect at all, so this returns null for one.
   */
  public fun immediateToEffect(
      instruction: InstructionTree,
      effectIsAutomatic: Boolean = false,
  ): Effect? {
    val syntaxTree =
        if (instruction is InstructionGroup) {
          InstructionGroup.createTree(instruction.instructions)
        } else {
          instruction
        }
    return if (syntaxTree == NoOp) {
      null
    } else {
      Effect(WhenGain, syntaxTree, effectIsAutomatic)
    }
  }
}
