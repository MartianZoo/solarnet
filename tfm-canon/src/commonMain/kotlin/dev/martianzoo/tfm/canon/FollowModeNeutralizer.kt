package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.Compact
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.tfm.canon.TfmClasses.PROJECT_CARD

/**
 * Retains generic card locations in follow mode while lowering references that still need faces.
 */
internal object FollowModeNeutralizer : TransformHandler {
  private val CARD_BACK = cn("CardBack")
  private val EVENT_PILE = cn("EventPile")
  private val HAND = cn("Hand")
  private val PLAYED_EVENT = cn("PlayedEvent")
  private val REVEALED = cn("Revealed")
  private val SELECTING = cn("Selecting")

  internal fun neutralize(source: ClassDeclaration): ClassDeclaration {
    val transformedEffects = source.effects.map(::transformEffect)
    return source.copy(
        executableEffects =
            transformedEffects.takeUnless { it == source.authoredEffectsWithActions },
        properties =
            source.properties.mapValues { (_, value) ->
              when (value) {
                is MetricValue -> MetricValue(transformMetric(value.value))
                is RequirementValue -> RequirementValue(transformRequirement(value.value))
                else -> value
              }
            },
    )
  }

  override fun transform(inner: PetNode): PetNode =
      when (inner) {
        is InstructionTree -> transformCards(inner)
        is Metric -> cardReferenceNeutralizer.transformMetric(inner)
        is Requirement -> cardReferenceNeutralizer.transformRequirement(inner)
        else -> malformed(inner)
      }

  internal fun transformEffect(source: dev.martianzoo.pets.ast.Effect) =
      transformer.transformEffect(source)

  internal fun transformMetric(source: Metric): Metric = transformer.transformMetric(source)

  internal fun transformRequirement(source: Requirement): Requirement =
      transformer.transformRequirement(source)

  private fun transformCards(source: InstructionTree): InstructionTree {
    val operation = CardOperation.decode(source)
    val transformed = cardReferenceNeutralizer.transformInstructionTree(source)
    return when (operation) {
      is CardOperation.SelectAndKeep, is CardOperation.SelectAndPlay ->
          withTemporaryLocation(SELECTING, transformed, close = true)
      is CardOperation.SelectAndPurchase, is CardOperation.RevealAndPurchase ->
          withTemporaryLocation(SELECTING, transformed, close = false)
      is CardOperation.RevealAndTest ->
          withTemporaryLocation(REVEALED, transformed, close = true)
      is CardOperation.RevealAndRestore ->
          Or.createTree(
              listOf(withTemporaryLocation(REVEALED, transformed, close = true), NoOp)
          )
      else -> transformed
    }
  }

  private fun withTemporaryLocation(
      location: ClassName,
      body: InstructionTree,
      close: Boolean,
  ): InstructionTree {
    val scopedBody =
        if (close) closeAfter(body, Remove.remove(location.expression, intensity = null)) else body
    return Then.createTree(listOf(Gain.gain(location.expression, intensity = null), scopedBody))
  }

  private fun closeAfter(body: InstructionTree, close: InstructionTree): InstructionTree =
      when (body) {
        is InstructionGroup ->
            InstructionGroup.createTree(
                body.instructions.dropLast(1) +
                    Then.createTree(listOf(body.instructions.last(), close))
            )
        is Then -> Then.createTree(body.instructions + close)
        else -> Then.createTree(listOf(body, close))
      }

  private fun Expression.isProjectCardAt(location: ClassName): Boolean =
      className == PROJECT_CARD && arguments.any { it.className == location }

  private fun Expression.isGenericCardBack(): Boolean =
      className in setOf(CARD_BACK, PROJECT_CARD, cn("CorporationCard"), cn("PreludeCard"))

  private fun malformed(source: PetNode): Nothing =
      throw PetSyntaxException(
          "Unsupported ${CardOperation.TRANSFORM_KIND} card operation: $source"
      )

  private val cardReferenceNeutralizer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode =
            when {
              node is Transmute &&
                  node.gaining.isProjectCardAt(HAND) &&
                  node.removing.isProjectCardAt(EVENT_PILE) ->
                  node.copy(fromEx = Full(PROJECT_CARD.expression, PLAYED_EVENT.expression))
              node is Transmute -> {
                val transformed = transformChildren(node) as Transmute
                if (node.gaining.isGenericCardBack() && node.gaining.refinement != null) {
                  transformed.copy(
                      fromEx =
                          (transformed.fromEx as? Compact)?.copy(refinement = null)
                              ?: transformed.fromEx,
                      intensity = OPTIONAL,
                  )
                } else {
                  transformed
                }
              }
              node is Expression && node.className == CARD_BACK && node.hasArea(EVENT_PILE) ->
                  node.withoutArea(PLAYED_EVENT, EVENT_PILE)
              node is Expression && node.isGenericCardBack() -> node.copy(refinement = null)
              else -> transformChildren(node)
            }

        private fun Expression.hasArea(area: ClassName): Boolean =
            arguments.count { it.className == area } == 1

        private fun Expression.withoutArea(result: ClassName, area: ClassName): Expression =
            copy(
                className = result,
                arguments = arguments.filterNot { it.className == area },
                argumentsSpecified = arguments.size > 1,
            )
      }

  private val transformer = TransformHandler.dispatcher(mapOf(CardOperation.TRANSFORM_KIND to this))
}
