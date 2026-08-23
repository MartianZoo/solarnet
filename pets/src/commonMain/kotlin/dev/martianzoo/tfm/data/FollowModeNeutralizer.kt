package dev.martianzoo.tfm.data

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.data.TfmClasses.PROJECT_CARD

/** Removes real-card source knowledge while follow mode still delegates hidden card outcomes. */
internal object FollowModeNeutralizer : PetTransformer() {
  private val BUY_CARD = cn("BuyCard")
  private val BUY_CARDS = cn("BuyCards")
  private val BUY_SELECTED_CARDS = cn("BuySelectedCards")
  private val EVENT_PILE = cn("EventPile")
  private val PLAYED_EVENT = cn("PlayedEvent")

  internal fun neutralize(source: ClassDeclaration): ClassDeclaration =
      source.copy(effects = source.effects.map(::transformEffect))

  override fun transformNode(node: PetNode): PetNode =
      when {
        node is Transform && node.transformKind == CARDS -> neutralizeCards(node.instruction)
        else -> transformChildren(node)
      }

  private fun neutralizeCards(source: InstructionTree): InstructionTree =
      when (source) {
        is Gain -> neutralizeSearch(source)
        is InstructionGroup -> neutralizeSelection(source)
        is Then ->
            when {
              source.first.gainingAt(REVEALED) -> neutralizeRevealedOperation(source)
              else -> malformed(source)
            }
        is Transmute -> neutralizeEventRecovery(source)
        else -> malformed(source)
      }

  private fun neutralizeSearch(source: Gain): InstructionTree {
    if (source.gaining.className != PROJECT_CARD || source.gaining.arguments.any())
        malformed(source)
    return Gain(scaledEx(PROJECT_CARD.expression, source.count), source.intensity)
  }

  private fun neutralizeSelection(source: InstructionGroup): InstructionTree {
    if (source.instructions.size != 2) malformed(source)
    val offered =
        source.instructions.filterIsInstance<Gain>().singleOrNull {
          it.gaining.isUnfilteredProjectCardAt(SELECTING)
        } ?: malformed(source)
    val result = source.instructions.singleOrNull { it !== offered } ?: malformed(source)

    return when (result) {
      is Transmute -> neutralizeRetainedCards(result)
      is Then -> neutralizePurchase(result)
      else -> malformed(source)
    }
  }

  private fun neutralizeRetainedCards(source: Transmute): InstructionTree {
    if (
        source.gaining != PROJECT_CARD.expression ||
            !source.removing.isProjectCardAt(SELECTING) ||
            source.removing.refinement != null
    ) {
      malformed(source)
    }
    return Gain(scaledEx(PROJECT_CARD.expression, source.count), source.intensity)
  }

  private fun neutralizePurchase(source: Then): InstructionTree {
    if (
        source.instructions.size != 2 ||
            !source.first.removingOptionallyAt(SELECTING) ||
            !source.instructions.last().isMandatoryGainOf(BUY_SELECTED_CARDS)
    ) {
      malformed(source)
    }
    return Gain(scaledEx(BUY_CARD.expression, 1), OPTIONAL)
  }

  private fun neutralizeRevealedOperation(source: Then): InstructionTree =
      if (source.instructions.last().isMandatoryGainOf(BUY_CARDS)) {
        neutralizeRevealedPurchase(source)
      } else {
        neutralizeReveal(source)
      }

  private fun neutralizeRevealedPurchase(source: Then): InstructionTree {
    if (source.instructions.size != 3) malformed(source)
    val revealed = source.first as? Gain ?: malformed(source)
    val retained = source.instructions[1] as? Transmute ?: malformed(source)
    if (
        retained.gaining.className != PROJECT_CARD ||
            retained.gaining.arguments.any() ||
            retained.gaining.refinement == null ||
            !retained.removing.isProjectCardAt(REVEALED) ||
            retained.removing.refinement != null ||
            retained.intensity != AMAP ||
            !source.instructions.last().isMandatoryGainOf(BUY_CARDS)
    ) {
      malformed(source)
    }
    val count = (revealed.count as? ActualScalar)?.value ?: malformed(source)
    return InstructionGroup(List(count) { followModeBuyOrFreeCard() })
  }

  private fun neutralizeReveal(source: Then): InstructionTree {
    if (source.instructions.size != 2) malformed(source)
    val revealed = source.first as? Gain ?: malformed(source)
    if (revealed.gaining.refinement != null) malformed(source)
    val choices = source.instructions.last() as? Or ?: malformed(source)
    if (choices.instructions.count { it == NoOp } != 1) malformed(source)
    val gated = choices.instructions.filterIsInstance<Gated>().singleOrNull() ?: malformed(source)
    if (
        gated.gate.descendantsOfType<Expression>().none {
          it.isProjectCardAt(REVEALED) && it.refinement != null
        }
    ) {
      malformed(source)
    }
    val outcome = gated.inner as? Gain ?: malformed(source)
    if (outcome.intensity != null && outcome.intensity != MANDATORY) malformed(source)
    return Gain(outcome.scaledEx, OPTIONAL)
  }

  private fun neutralizeEventRecovery(source: Transmute): InstructionTree {
    if (source.gaining != PROJECT_CARD.expression || !source.removing.isProjectCardAt(EVENT_PILE)) {
      malformed(source)
    }
    return source.copy(
        fromEx =
            source.fromEx.copy(
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

  private fun InstructionTree.isMandatoryGainOf(className: ClassName): Boolean =
      this is Gain &&
          gaining == className.expression &&
          (intensity == null || intensity == MANDATORY)

  private fun Instruction.gainingAt(area: ClassName): Boolean =
      this is Gain && gaining.isUnfilteredProjectCardAt(area)

  private fun InstructionTree.removingOptionallyAt(area: ClassName): Boolean =
      this is Remove && removing.isProjectCardAt(area) && intensity == OPTIONAL

  private fun Expression.isProjectCardAt(area: ClassName): Boolean =
      className == PROJECT_CARD && arguments.singleOrNull()?.className == area

  private fun Expression.isUnfilteredProjectCardAt(area: ClassName): Boolean =
      isProjectCardAt(area) && refinement == null

  private fun malformed(source: InstructionTree): Nothing =
      throw PetSyntaxException("Unsupported $CARDS card operation: $source")

  private const val CARDS = "CARDS"
  private val SELECTING = cn("Selecting")
  private val REVEALED = cn("Revealed")
}
