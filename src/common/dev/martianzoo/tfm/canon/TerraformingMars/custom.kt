@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon.terraformingmars

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Transform as InstructionTransform
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.pets.ast.Requirement.Exact
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.CustomClass
import dev.martianzoo.state.CustomInstruction
import dev.martianzoo.state.CustomMetric
import dev.martianzoo.state.GameReader
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.TfmClasses.PROD
import dev.martianzoo.tfm.canon.TfmClasses.PROJECT_CARD
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.cardEffects
import dev.martianzoo.tfm.canon.cardImmediate
import dev.martianzoo.tfm.canon.cardTags
import dev.martianzoo.tfm.canon.tfmCatalog
import kotlin.math.abs

private val copyProductionBox =
    object : CustomInstruction("CopyProductionBox") {
      override fun translate(reader: GameReader, owner: Type, cardType: Type): Instruction {
        val card = reader.tfmCatalog.card(cardType.className)
        val immediate =
            cardImmediate(card)
                ?: throw NarrowingException("card ${card.className} has no immediate instruction")
        val matches =
            immediate.descendantsOfType<InstructionTransform>().filter { it.transformKind == PROD }

        if (
            immediate.descendantsOfType<Instruction.Each>().any { each ->
              each.descendantsOfType<InstructionTransform>().any { it.transformKind == PROD }
            }
        ) {
          throw ExpressionException(
              "Card ${card.className} has PROD inside EACH; move EACH inside PROD so copying preserves its bindings"
          )
        }

        return when (matches.size) {
          0 -> throw NarrowingException("must choose a card that has an immediate PROD box")
          1 -> matches.first()
          else -> error("Card ${card.className} is malformed, has ${matches.size} PROD blocks")
        }
      }
    }

private val nonNegativeIconsOf =
    object : CustomMetric("NonNegativeIconsOf") {
      override fun count(game: GameReader, type: Type): Int {
        val (cardType, targetClassType) = type.typeDependencies.map { it.boundType }
        val effects = cardEffects(card(cardType, game))
        val target = requireNotNull(targetClassType.representedClass).className
        return effects.sumOf { it.citationsOutsideRemoval(target) }
      }

      private fun Effect.citationsOutsideRemoval(target: ClassName): Int {
        var count = 0
        visitDescendants { node ->
          when {
            node is Instruction.Change -> {
              count +=
                  node.gaining?.descendantsOfType<Expression>()?.count { it.className == target }
                      ?: 0
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

private val placementBonus =
    object : CustomMetric("PlacementBonus") {
      override fun countAbstract(game: GameReader, type: Type): Int? {
        val arguments = type.typeDependencies.map { it.boundType }
        val area = arguments.single { it.className != CLASS }
        if (area.abstract) return null
        val resource = requireNotNull(arguments.single { it.className == CLASS }.representedClass)
        val bonus = mapDefinition(game).areas.single { it.className == area.className }.bonus
        return bonus?.descendantsOfType<Gain>()?.sumOf {
          if (game.classTable.getClass(it.gaining.className).isSubtypeOf(resource))
              (it.count as ActualScalar).value
          else 0
        } ?: 0
      }

      override fun count(game: GameReader, type: Type): Int {
        val arguments = type.typeDependencies.map { it.boundType }
        val resourceName =
            requireNotNull(arguments.single { it.className == CLASS }.representedClass).className
        val areaName = arguments.single { it.className != CLASS }.className
        val bonus = mapDefinition(game).areas.single { it.className == areaName }.bonus ?: return 0
        return bonus.descendantsOfType<Gain>().sumOf {
          if (it.gaining.className == resourceName) (it.count as ActualScalar).value else 0
        }
      }
    }

private val neighbor =
    object : CustomMetric("Neighbor") {
      override fun countAbstract(game: GameReader, type: Type): Int {
        val (tile, target) = type.typeDependencies.map { it.boundType }
        val targets = game.getComponents(target).elements
        val areas = mapDefinition(game).areas
        return targets.sumOf { targetArea ->
          val row = targetArea.getNumberPropertyValue("row")
          val column = targetArea.getNumberPropertyValue("column")
          areas.hexNeighbors(row, column).sumOf { sourceArea ->
            val sourceType = game.resolve(sourceArea.className.expression)
            game.getDependents(sourceType).count { it.narrows(tile, game) }
          }
        }
      }

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

private val gpRequirementShortfall =
    object : CustomMetric("GpRequirementShortfall") {
      override fun count(game: GameReader, type: Type): Int {
        val (cardClassType, parameterClassType) = type.typeDependencies.map { it.boundType }
        val requirement =
            representedType(cardClassType, game).getRequirementPropertyValue("requirement")
                as? Counting ?: return 0
        val counted = requirement.metric as? Metric.Count ?: return 0
        if (game.resolve(counted.expression).rootClass != parameterClassType.representedClass) {
          return 0
        }

        val actual = game.count(counted)
        return when (requirement) {
          is Min -> (requirement.target - actual).coerceAtLeast(0)
          is Max -> (actual - requirement.target).coerceAtLeast(0)
          is Exact -> abs(actual - requirement.target)
        }
      }
    }

private val priceAspectCount =
    object : CustomMetric("PriceAspectCount") {
      override val requiredClassNames: Set<ClassName> = setOf(PROJECT_CARD)

      override fun count(game: GameReader, type: Type): Int {
        val (cardClassType, aspectClassType) = type.typeDependencies.map { it.boundType }
        val card = cardFromClassType(cardClassType, game)
        if (cardBack(card)?.isSubtypeOf(card.classTable.getClass(PROJECT_CARD)) != true) return 0
        val aspect = requireNotNull(aspectClassType.representedClass)
        return if (aspect.className == card.className) 1 else cardTags(card).count(aspect.className)
      }
    }

private val scoreEventVps =
    object : CustomInstruction("ScoreEventVps") {
      override fun translate(
          reader: GameReader,
          ignoredOwningType: Type,
          classType: Type,
      ): InstructionTree {
        val effects = cardEffects(cardFromClassType(classType, reader))
        return InstructionGroup.of(effects.filter { it.trigger == end }.map { it.instruction })
      }

      private val end: Trigger = parse("End")
    }

private fun cardFromClassType(cardClassType: Type, reader: GameReader): Class {
  return reader.tfmCatalog.card(representedType(cardClassType, reader).className)
}

private fun representedType(classType: Type, reader: GameReader): Type {
  require(classType.className == CLASS)
  return reader.resolve(requireNotNull(classType.representedClass).className.expression)
}

private fun card(type: HasClassName, reader: GameReader): Class =
    reader.tfmCatalog.card(type.className)

internal val customClasses: Set<CustomClass> =
    setOf(
        neighbor,
        gpRequirementShortfall,
        priceAspectCount,
        scoreEventVps,
        nonNegativeIconsOf,
        placementBonus,
        copyProductionBox,
    )
