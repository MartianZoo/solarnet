package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.FromExpression.Compact
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.tfm.canon.TfmClasses.PROJECT_CARD

/** Lowers marked face-sensitive card procedures into identity-free follow-mode effects. */
internal object FollowModeNeutralizer : TransformHandler {
  internal fun neutralize(source: ClassDeclaration): ClassDeclaration {
    val transformedEffects = source.effects.map(transformer::transformEffect)
    return source.copy(
        executableEffects =
            transformedEffects.takeUnless { it == source.authoredEffectsWithActions },
    )
  }

  override fun transform(inner: PetNode): PetNode =
      when (inner) {
        is InstructionTree -> transformCards(inner)
        else -> malformed(inner)
      }

  private fun transformCards(source: InstructionTree): InstructionTree =
      when (val operation = CardOperation.decode(source)) {
        is CardOperation.Search ->
            operation.cards.copy(scaledEx = scaledEx(PROJECT_CARD, operation.cards.count))
        is CardOperation.RevealAndTest ->
            Then.createTree(
                listOf(operation.revealed, operation.outcome.copy(intensity = OPTIONAL))
            )
        is CardOperation.RevealAndPurchase ->
            replacer(operation.retained, operation.retained.withoutFilter())
                .transformInstructionTree(source)
      }

  private fun Transmute.withoutFilter(): Transmute =
      copy(
          fromEx =
              when (val source = fromEx) {
                is Compact -> source.copy(refinement = null)
                is Full -> source.copy(toExpression = source.toExpression.copy(refinement = null))
                is FromExpression.Unchanged -> malformed(this)
              },
          intensity = OPTIONAL,
      )

  private fun malformed(source: PetNode): Nothing =
      throw PetSyntaxException(
          "Unsupported ${CardOperation.TRANSFORM_KIND} card operation: $source"
      )

  private val transformer = TransformHandler.dispatcher(mapOf(CardOperation.TRANSFORM_KIND to this))
}
