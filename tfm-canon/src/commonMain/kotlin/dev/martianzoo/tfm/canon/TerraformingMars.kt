@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.DIE
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
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.pets.ast.Requirement.Exact
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.Grid
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.canon.TfmClasses.PROD
import dev.martianzoo.tfm.canon.TfmClasses.TILE
import dev.martianzoo.tfm.engine.TfmApiUtils.getPlayerOwner

internal val terraformingMarsBundle: StandardFormBundle by lazy {
  StandardFormBundle(
      "TerraformingMars",
      terraformingMarsCustomClasses,
      routines = terraformingMarsRoutines,
      additionalResourceDirectories =
          setOf(
              "bundles/CorporateEraExpansion",
              "bundles/TharsisMap",
          ),
  )
}

private val terraformingMarsCustomClasses: Set<CustomClass> =
    setOf(
        TerraformingMars.CreateAdjacencies,
        TerraformingMars.CreateMapAreas,
        TerraformingMars.CheckCardDeck,
        TerraformingMars.HandlePossibleGpRequirement,
        TerraformingMars.HandleCardTags,
        TerraformingMars.GetEventVps,
        TerraformingMars.PassLeft,
        TerraformingMars.AssignAwardPlaces,
        TerraformingMars.MultiplayerVictoryCheck,
        TerraformingMars.CitationsIgnoringRemoves,
        TerraformingMars.MapBonus,
        TerraformingMars.CopyProductionBox,
    )

/** Namespace for the core game's custom Pets implementations. */
private object TerraformingMars {
  internal object CreateMapAreas : CustomClass() {
    override fun translate(reader: GameReader, mapType: Type): InstructionTree {
      val map = reader.tfmCatalog.marsMap(mapType.className)
      return Then.create(
          map.areas.mapNotNull { area ->
            gain(area.className.expression).takeIf {
              reader.countComponent(reader.resolve(area.className.expression)) == 0
            }
          }
      )
    }
  }

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

  internal object CitationsIgnoringRemoves : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val (cardExpression, targetExpression) = type.expressionFull.arguments
      if (game.countComponent(game.resolve(cardExpression)) == 0) return 0
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

  internal object MapBonus : CustomMetric() {
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

  private val NEIGHBOR = cn("Neighbor")
  private val FORWARD_ADJACENCY = cn("ForwardAdjacency")
  private val BACKWARD_ADJACENCY = cn("BackwardAdjacency")

  internal object CreateAdjacencies : CustomClass() {
    override val requiredClassNames: Set<ClassName> =
        setOf(NEIGHBOR, FORWARD_ADJACENCY, BACKWARD_ADJACENCY)

    override fun translate(reader: GameReader, areaType: Type): Instruction {
      val grid: Grid<AreaDefinition> = mapDefinition(reader).areas
      val row = areaType.getNumberPropertyValue("row")
      val column = areaType.getNumberPropertyValue("column")
      val area = grid[row, column]!!
      val neighborAreas: List<AreaDefinition> = grid.hexNeighbors(row, column)

      fun tileOn(area: AreaDefinition): Expression? {
        val tileType: Type = reader.resolve(TILE.of(area.className))
        return reader.getComponents(tileType).singleOrNull()?.expression
      }

      val newTile: Expression = tileOn(area)!!
      val neighbors = neighborAreas.map { NEIGHBOR.of(newTile, it.className.expression) }
      val adjacencies =
          neighborAreas.mapNotNull(::tileOn).flatMap {
            listOf(
                FORWARD_ADJACENCY.of(it, newTile),
                BACKWARD_ADJACENCY.of(newTile, it),
            )
          }
      return Then.create((neighbors + adjacencies).map(::gain))
    }
  }

  internal object CheckCardDeck : CustomClass() {
    override fun translate(
        reader: GameReader,
        cardBackClassType: Type,
        cardFrontClassType: Type,
    ): Instruction {
      val deck = cardBack(cardFromClassType(cardFrontClassType, reader))
      return if (representedType(cardBackClassType, reader).className == deck?.className) {
        NoOp
      } else {
        gain(DIE)
      }
    }
  }

  internal object HandlePossibleGpRequirement : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(REQUIRED, GLOBAL_PARAMETER)

    override fun translate(
        reader: GameReader,
        ignoredOwner: Type,
        cardClassType: Type,
    ): Instruction {
      val requirement =
          cardRequirement(representedType(cardClassType, reader)) ?: return FALLBACK_UNAVAILABLE
      return globalParameterShortfall(requirement, reader)?.let { (parameter, count) ->
        gain(REQUIRED.of(CLASS.of(parameter)), count)
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

  internal object GetEventVps : CustomClass() {
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
      val currentOwner: Player = getPlayerOwner(reader, component)
      val players = reader.actors.filterIsInstance<Player>()
      if (players.size == 1) return NoOp

      val current = players.indexOf(currentOwner)
      check(current >= 0) { "StartToken owner is not a seated Player: $currentOwner" }
      val nextOwner = players[(current + 1) % players.size]
      val arguments =
          component.expressionFull.arguments.map {
            if (it == currentOwner.expression) Full(nextOwner.expression, it) else Unchanged(it)
          }
      return Transmute(
          Compact(component.className, arguments),
          ActualScalar(reader.countComponent(component)),
      )
    }
  }

  private val AWARD_TALLY = cn("AwardTally")
  private val FIRST_PLACE = cn("FirstPlace")
  private val SECOND_PLACE = cn("SecondPlace")

  internal object AssignAwardPlaces : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(AWARD_TALLY, FIRST_PLACE, SECOND_PLACE)

    override fun translate(reader: GameReader, awardType: Type): Instruction {
      val players = reader.getComponents("Player").elements
      val scores = players.associateWith { reader.count(reader.resolve(tally(it, awardType))) }
      val firstScore = scores.values.maxOrNull() ?: return NoOp

      val first = scores.filterValues { it == firstScore }.keys
      val winners = first.map { FIRST_PLACE.of(it.expression, awardType.expression) }
      val placements =
          if (players.size < 3 || first.size > 1) {
            winners
          } else {
            val secondScore = scores.filterKeys { it !in first }.values.maxOrNull() ?: 0
            val runnersUp =
                scores
                    .filter { (player, score) -> player !in first && score == secondScore }
                    .keys
                    .map { SECOND_PLACE.of(it.expression, awardType.expression) }
            winners + runnersUp
          }
      return Then.create(placements.map(::gain))
    }
  }

  private val VICTORY = cn("Victory")

  internal object MultiplayerVictoryCheck : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(VICTORY)

    override fun translate(reader: GameReader): Instruction {
      val players = reader.getComponents("Player").elements
      val victoryPoints = players.associateWith {
        reader.count(reader.resolve(cn("VictoryPoint").of(it.expression)))
      }
      val mostVictoryPoints = victoryPoints.values.maxOrNull() ?: return NoOp
      val leaders = victoryPoints.filterValues { it == mostVictoryPoints }.keys
      val mc = leaders.associateWith {
        reader.count(reader.resolve(cn("MC").of(it.expression)))
      }
      val mostMC = mc.values.maxOrNull() ?: return NoOp
      val winners = mc.filterValues { it == mostMC }.keys
      return Then.create(winners.map { gain(VICTORY.of(it.expression)) })
    }
  }

  private fun tally(player: HasClassName, awardType: Type): Expression =
      AWARD_TALLY.of(player.className.expression, awardType.expression)

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
