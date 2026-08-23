package dev.martianzoo.tfm.data

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.data.CardOperation.RecoverEvents
import dev.martianzoo.tfm.data.CardOperation.RevealAndPurchase
import dev.martianzoo.tfm.data.CardOperation.RevealAndTest
import dev.martianzoo.tfm.data.CardOperation.Search
import dev.martianzoo.tfm.data.CardOperation.SelectAndKeep
import dev.martianzoo.tfm.data.CardOperation.SelectAndPurchase
import dev.martianzoo.tfm.data.TfmClasses.PROJECT_CARD

/** Removes real-card source knowledge while follow mode still delegates hidden card outcomes. */
internal object FollowModeNeutralizer : PetTransformer() {
  internal fun neutralize(source: ClassDeclaration): ClassDeclaration =
      source.copy(effects = source.effects.map(::transformEffect))

  override fun transformNode(node: PetNode): PetNode =
      when {
        node is Transform && node.transformKind == CardOperation.TRANSFORM_KIND ->
            neutralize(CardOperation.decode(node))
        else -> transformChildren(node)
      }

  private fun neutralize(operation: CardOperation): InstructionTree =
      when (operation) {
        is Search ->
            Gain(
                scaledEx(PROJECT_CARD.expression, operation.cards.count),
                operation.cards.intensity,
            )
        is SelectAndKeep ->
            Gain(
                scaledEx(PROJECT_CARD.expression, operation.retained.count),
                operation.retained.intensity,
            )
        is SelectAndPurchase -> Gain(scaledEx(BUY_CARD.expression, 1), OPTIONAL)
        is RevealAndPurchase -> {
          val count = (operation.revealed.count as ActualScalar).value
          InstructionGroup(List(count) { followModeBuyOrFreeCard() })
        }
        is RevealAndTest -> Gain(operation.outcome.scaledEx, OPTIONAL)
        is RecoverEvents ->
            operation.recovered.copy(
                fromEx =
                    operation.recovered.fromEx.copy(
                        toExpression = PROJECT_CARD.expression,
                        fromExpression = PLAYED_EVENT.expression,
                    )
            )
      }

  private fun followModeBuyOrFreeCard(): Instruction {
    val free = Gain(scaledEx(PROJECT_CARD.expression, 1), null)
    val buy = Gain(scaledEx(BUY_CARD.expression, 1), OPTIONAL)
    return Or.create(listOf(free, buy))
  }

  private val BUY_CARD = cn("BuyCard")
  private val PLAYED_EVENT = cn("PlayedEvent")
}
