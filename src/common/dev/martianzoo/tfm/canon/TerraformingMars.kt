@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.Compact
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.FromExpression.Unchanged
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform as InstructionTransform
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.pets.ast.Requirement.Exact
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.ApiUtils.getOwner
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.TfmClasses.PROD
import dev.martianzoo.tfm.canon.TfmClasses.SUCCESSOR
import kotlin.math.abs

private val terraformingMarsCustomClasses: Set<CustomClass> =
    setOf(
        TerraformingMars.Neighbor,
        TerraformingMars.AdjustGpRequirement,
        TerraformingMars.HandleCardTags,
        TerraformingMars.ScoreEventVps,
        TerraformingMars.PassLeft,
        TerraformingMars.NonNegativeIconsOf,
        TerraformingMars.PlacementBonus,
        TerraformingMars.CopyProductionBox,
    )

internal val terraformingMarsBundle: StandardFormBundle =
    StandardFormBundle(
        "TerraformingMars",
        terraformingMarsCustomClasses,
        additionalResourceDirectories =
            setOf(
                "bundles/CorporateEraExpansion",
            ),
    )

/** Namespace for the core game's custom Pets implementations. */
private object TerraformingMars {
  internal object CopyProductionBox : CustomClass() {
    override fun translate(reader: GameReader, owner: Type, cardType: Type): Instruction {
      val card = reader.tfmCatalog.card(cardType.className)
      val immediate =
          cardImmediate(card)
              ?: throw NarrowingException("card ${card.className} has no immediate instruction")
      val matches =
          immediate.descendantsOfType<InstructionTransform>().filter { it.transformKind == PROD }

      return when (matches.size) {
        0 -> throw NarrowingException("must choose a card that has an immediate PROD box")
        1 -> matches.first()
        else -> error("Card ${card.className} is malformed, has ${matches.size} PROD blocks")
      }
    }
  }

  internal object NonNegativeIconsOf : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val (cardExpression, targetExpression) = type.expressionFull.arguments
      val effects = cardEffects(card(cardExpression, game))
      val target = targetExpression.arguments.single().className
      return effects.sumOf { it.citationsOutsideRemoval(target) }
    }

    private fun Effect.citationsOutsideRemoval(target: ClassName): Int {
      var count = 0
      visitDescendants { node ->
        when {
          node is Instruction.Change -> {
            count +=
                node.gaining?.descendantsOfType<Expression>()?.count { it.className == target } ?: 0
            false
          }
          node is Expression && node.className == target -> {
            count++
            true
          }
          else -> true
        }
      }
      return count
    }
  }

  internal object PlacementBonus : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val arguments = type.expressionFull.arguments
      val resourceName = arguments.single { it.className == CLASS }.arguments.single().className
      val areaName = arguments.single { it.className != CLASS }.className
      val bonus = mapDefinition(game).areas.single { it.className == areaName }.bonus ?: return 0
      return bonus.descendantsOfType<Gain>().sumOf {
        if (it.gaining.className == resourceName) (it.count as ActualScalar).value else 0
      }
    }
  }

  internal object Neighbor : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val (piece, target) = type.typeDependencies.map { it.boundType }
      val source =
          piece.typeDependencies
              .map { it.boundType }
              .singleOrNull {
                listOf("row", "column").all { property ->
                  PropertyName(property) in it.rootClass.properties
                }
              } ?: return 0
      val rowDelta = target.getNumberPropertyValue("row") - source.getNumberPropertyValue("row")
      val columnDelta =
          target.getNumberPropertyValue("column") - source.getNumberPropertyValue("column")
      if (abs(rowDelta) > 1 || abs(columnDelta) > 1) return 0
      return if (rowDelta + columnDelta == 0) 0 else 1
    }
  }

  internal object AdjustGpRequirement : CustomClass() {
    override val requiredClassNames: Set<ClassName> =
        setOf(REQUIRED, CHECK_REQUIREMENT, GLOBAL_PARAMETER)

    override fun translate(
        reader: GameReader,
        ignoredOwner: Type,
        cardClassType: Type,
    ): Instruction {
      val requirement =
          cardRequirement(representedType(cardClassType, reader)) ?: return FALLBACK_UNAVAILABLE
      return globalParameterShortfall(requirement, reader)?.let { (parameter, count) ->
        Then.create(
            listOf(
                gain(REQUIRED.of(CLASS.of(parameter)), count),
                gain(CHECK_REQUIREMENT.of(cardClassType.expression)),
            )
        )
      } ?: FALLBACK_UNAVAILABLE
    }

    private fun globalParameterShortfall(
        requirement: Requirement,
        reader: GameReader,
    ): Pair<Expression, Int>? {
      val counting = requirement as? Counting ?: return null
      val counted = counting.metric as? Metric.Count ?: return null
      val parameter = counted.expression
      val isGlobalParameter =
          reader.resolve(parameter).rootClass.allSuperclasses().any {
            it.className == GLOBAL_PARAMETER
          }
      if (!isGlobalParameter) return null

      val actual = reader.count(counting.metric)
      val shortfall =
          when (counting) {
            is Min -> counting.target - actual
            is Max -> actual - counting.target
            is Exact -> kotlin.math.abs(actual - counting.target)
          }
      return if (shortfall > 0) parameter to shortfall else null
    }

    private val FALLBACK_UNAVAILABLE: Instruction = Gated.create(parse<Requirement>("Die"), NoOp)
  }

  private val PLAY_TAG = cn("PlayTag")
  private val REQUIRED = cn("Required")
  private val CHECK_REQUIREMENT = cn("CheckRequirement")
  private val GLOBAL_PARAMETER = cn("GlobalParameter")

  internal object HandleCardTags : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(PLAY_TAG)

    override fun translate(
        reader: GameReader,
        owner: Type,
        cardFrontClassType: Type,
    ): Instruction {
      val card = cardFromClassType(cardFrontClassType, reader)
      return Then.create(
          cardTags(card).entries.map { (tagName, count) ->
            gain(PLAY_TAG.of(tagName.classExpression()), count)
          }
      )
    }
  }

  internal object ScoreEventVps : CustomClass() {
    override fun translate(
        reader: GameReader,
        ignoredOwner: Type,
        classType: Type,
    ): InstructionTree {
      val effects = cardEffects(cardFromClassType(classType, reader))
      return InstructionGroup.of(effects.filter { it.trigger == end }.map { it.instruction })
    }

    private val end: Trigger = parse("End")
  }

  internal object PassLeft : CustomClass() {
    override fun translate(reader: GameReader, component: Type): Instruction {
      val currentOwner = getOwner(reader, component).groundType
      val outgoing =
          reader.getComponents(reader.resolve(SUCCESSOR.expression)).single { relation ->
            relation.typeDependencies.single { it.key == Key(SUCCESSOR, 0) }.boundType ==
                currentOwner
          }
      val nextOwner = outgoing.typeDependencies.single { it.key == Key(SUCCESSOR, 1) }.boundType
      if (nextOwner == currentOwner) return NoOp

      val arguments =
          component.expressionFull.arguments.map {
            if (reader.resolve(it).groundType == currentOwner) Full(nextOwner.expression, it)
            else Unchanged(it)
          }
      return Transmute(
          Compact(component.className, arguments),
          ActualScalar(reader.countComponent(component)),
      )
    }
  }

  private fun cardFromClassType(cardClassType: Type, reader: GameReader): Class {
    return reader.tfmCatalog.card(representedType(cardClassType, reader).className)
  }

  private fun representedType(classType: Type, reader: GameReader): Type {
    require(classType.className == CLASS)
    return reader.resolve(classType.expressionFull.arguments.single())
  }

  private fun card(type: HasClassName, reader: GameReader): Class =
      reader.tfmCatalog.card(type.className)

  private fun cardRequirement(cardType: Type): Requirement? =
      cardType.getRequirementPropertyValue("requirement")
}
