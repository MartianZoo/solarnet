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
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar

/**
 * Various functions for transforming Pets syntax trees. Many more interesting transformers require
 * a class table, and therefore are found in the `engine` module's `Transformers` class.
 */
public object Transforming {
  // TODO: Move Terraforming Mars payment lowering into tfm-canon.
  private val standardResourceClasses: Set<ClassName> =
      setOf("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat").mapTo(linkedSetOf(), ::cn)

  /**
   * Replaces each occurrence of the special `This` expression with [contextType], replacing
   * `Class<This>` appropriately as well. An explicitly specialized `This<Foo>` keeps its authored
   * arguments and specializes the concrete context class, becoming (for example) `Bar<Foo>`.
   */
  public fun replaceThisExpressionsWith(contextType: Expression): PetTransformer =
      chain(
          replacer(THIS.classExpression(), contextType.className.classExpression()),
          object : PetTransformer() {
            override fun transformNode(node: PetNode): PetNode {
              if (node == THIS.expression) return contextType
              val transformed = transformChildren(node)
              return if (transformed is Expression && transformed.className == THIS) {
                transformed.copy(className = contextType.className)
              } else {
                transformed
              }
            }
          },
      )

  /** Replaces each occurrence of the contextual `Owner` placeholder with [owner]. */
  @Suppress("ComplexCondition") // TODO fix
  public fun replaceOwnerWith(owner: HasClassName): PetTransformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
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

  /** Replaces every authored X scalar with [value], retaining written coefficients. */
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
        if (spend.scaledEx.expression.className == cn("Megacredit")) ""
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

  public fun actionSelectors(actions: Collection<Action>): Set<ClassName> =
      actions.indices.mapTo(linkedSetOf()) { actionSelector(it + 1) }

  private fun actionSelector(index1Ref: Int): ClassName =
      listOf(cn("First"), cn("Second"), cn("Third")).getOrNull(index1Ref - 1)
          ?: throw IllegalArgumentException("A component can offer only three actions: $index1Ref")

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
