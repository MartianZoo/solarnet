@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.CustomMetric
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.data.Player
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost.Spend
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.api.ApiUtils.getPlayerOwner
import dev.martianzoo.tfm.api.ApiUtils.mapDefinition
import dev.martianzoo.tfm.api.tfmRuleset
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.data.TfmClasses.TILE
import dev.martianzoo.types.Type
import dev.martianzoo.util.Grid

internal val baseCustomClasses: Set<CustomClass> =
    setOf(
        TerraformingMars.CreateAdjacencies,
        TerraformingMars.CheckCardDeck,
        TerraformingMars.CheckCardRequirement,
        TerraformingMars.HandleCardCost,
        TerraformingMars.GetEventVps,
        TerraformingMars.PassLeft,
        TerraformingMars.AssignAwardPlaces,
        TerraformingMars.MultiplayerVictoryCheck,
        TerraformingMars.MarsRow,
        TerraformingMars.CardCost,
        TerraformingMars.CitationsIgnoringRemoves,
        TerraformingMars.CardRequirement,
        TerraformingMars.ClassCardRequirement,
        TerraformingMars.StandardProjectCost,
        TerraformingMars.MapBonus,
    )

/** Namespace for the core game's custom Pets implementations. */
internal object TerraformingMars {
  internal object MarsRow : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val areaName = type.expressionFull.arguments.single().className
      return mapDefinition(game).areas.single { it.className == areaName }.row
    }
  }

  internal object CardCost : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int =
        card(type.expressionFull.arguments.single(), game).cost
  }

  internal object CitationsIgnoringRemoves : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val (cardExpression, targetExpression) = type.expressionFull.arguments
      if (game.countComponent(game.resolve(cardExpression)) == 0) return 0
      val effects = card(cardExpression, game).effects
      val target = targetExpression.arguments.single().className
      return effects.sumOf { it.citationsOutsideRemoval(target) }
    }

    private fun Effect.citationsOutsideRemoval(target: ClassName): Int {
      var count = 0
      visitDescendants { node ->
        when {
          node is Instruction.Change -> {
            count +=
                node.gaining?.descendantsOfType<Expression>()?.count {
                  it.className == target
                } ?: 0
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

  internal object CardRequirement : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int =
        if (card(type.expressionFull.arguments.single(), game).requirement == null) 0 else 1
  }

  internal object ClassCardRequirement : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val cardClass = type.expressionFull.arguments.single().arguments.single()
      return if (card(cardClass, game).requirement == null) 0 else 1
    }
  }

  internal object StandardProjectCost : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val projectName = type.expressionFull.arguments.single().className
      val action = parse<Action>(game.tfmRuleset.action(projectName).actions.single())
      return ((action.cost as Spend).scaledEx.scalar as ActualScalar).value
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

  internal object CreateAdjacencies : CustomClass() {
    override fun translate(reader: GameReader, areaType: Type): Instruction {
      val grid: Grid<AreaDefinition> = mapDefinition(reader).areas
      val area = grid.firstOrNull { it.className == areaType.className } ?: error(areaType)
      val neighborAreas: List<AreaDefinition> = grid.hexNeighbors(area.row, area.column)

      fun tileOn(area: AreaDefinition): Expression? {
        val tileType: Type = reader.resolve(TILE.of(area.className))
        return reader.getComponents(tileType).singleOrNull()?.expression
      }

      val newTile: Expression = tileOn(area)!!
      val neighbors = neighborAreas.map { cn("Neighbor").of(newTile, it.className.expression) }
      val adjacencies =
          neighborAreas.mapNotNull(::tileOn).flatMap {
            listOf(
                cn("ForwardAdjacency").of(it, newTile),
                cn("BackwardAdjacency").of(newTile, it),
            )
          }
      return Then.create((neighbors + adjacencies).map { gain(scaledEx(1, it)) })
    }
  }

  internal object CheckCardDeck : CustomClass() {
    override fun translate(
        reader: GameReader,
        cardBackClassType: Type,
        cardFrontClassType: Type,
    ): Instruction {
      val deck = cardFromClassType(cardFrontClassType, reader).deck
      return if (cardBackClassType.expression.arguments.single().className == deck?.className) {
        NoOp
      } else {
        parse("DIE!")
      }
    }
  }

  internal object CheckCardRequirement : CustomClass() {
    override fun translate(reader: GameReader, owner: Type, cardClassType: Type) =
        Gated.create(cardFromClassType(cardClassType, reader).requirement, NoOp)
  }

  internal object HandleCardCost : CustomClass() {
    override fun translate(
        reader: GameReader,
        owner: Type,
        cardFrontClassType: Type,
    ): Instruction {
      val card = cardFromClassType(cardFrontClassType, reader)
      if (card.cost == 0) return NoOp

      val playTagSignals =
          card.tags.entries.map { (tagName, count) ->
            gain(scaledEx(count, cn("PlayTag").of(tagName.classExpression())))
          }
      val instructions = listOf(gain(scaledEx(card.cost, cn("Owed")))) + playTagSignals
      return Then.create(instructions)
    }
  }

  internal object GetEventVps : CustomClass() {
    override fun translate(reader: GameReader, ignoredOwner: Type, classType: Type): Instruction {
      val effects = cardFromClassType(classType, reader).effects
      return Multi.create(effects.filter { it.trigger == end }.map { it.instruction })
    }

    private val end: Trigger = parse("End")
  }

  internal object PassLeft : CustomClass() {
    override fun translate(reader: GameReader, component: Type): Instruction {
      val currentOwner: Player = getPlayerOwner(reader, component)
      val current = currentOwner.toString().removePrefix("Player").toInt()
      val playerCount: Int = reader.count(parse<Metric>("Player"))
      if (playerCount == 1) return NoOp

      val next = current % playerCount + 1
      val fromExpression = component.className.of(component.expressionFull.arguments)
      val toExpression =
          component.className.of(
              component.expressionFull.arguments.map {
                if (it == currentOwner.expression) cn("Player$next").expression else it
              }
          )
      return Transmute(
          FromExpression(toExpression, fromExpression),
          ActualScalar(reader.countComponent(component)),
      )
    }
  }

  internal object AssignAwardPlaces : CustomClass() {
    override fun translate(reader: GameReader, awardType: Type): Instruction {
      val players = reader.getComponents("Player").elements
      val measuredType =
          reader.resolve(cn("AwardMeasured").of(cn("Player").expression, awardType.expression))
      if (reader.count(measuredType) < players.size) return NoOp

      val scores = players.associateWith {
        reader.count(reader.resolve(tally(it, awardType)))
      }
      val firstScore = scores.values.maxOrNull() ?: return NoOp

      val first = scores.filterValues { it == firstScore }.keys
      val winners = first.map { cn("FirstPlace").of(it.expression, awardType.expression) }
      val placements =
          if (players.size < 3 || first.size > 1) {
            winners
          } else {
            val secondScore = scores.filterKeys { it !in first }.values.maxOrNull() ?: 0
            val runnersUp =
                scores
                    .filter { (player, score) ->
                      player !in first && score == secondScore
                    }
                    .keys
                    .map { cn("SecondPlace").of(it.expression, awardType.expression) }
            winners + runnersUp
          }
      return Then.create(placements.map { gain(scaledEx(1, it)) })
    }
  }

  internal object MultiplayerVictoryCheck : CustomClass() {
    override fun translate(reader: GameReader): Instruction {
      val players = reader.getComponents("Player").elements
      val victoryPoints = players.associateWith {
        reader.count(reader.resolve(cn("VictoryPoint").of(it.expression)))
      }
      val mostVictoryPoints = victoryPoints.values.maxOrNull() ?: return NoOp
      val leaders = victoryPoints.filterValues { it == mostVictoryPoints }.keys
      val megacredits = leaders.associateWith {
        reader.count(reader.resolve(cn("Megacredit").of(it.expression)))
      }
      val mostMegacredits = megacredits.values.maxOrNull() ?: return NoOp
      val winners = megacredits.filterValues { it == mostMegacredits }.keys
      return Then.create(winners.map { gain(scaledEx(1, cn("Victory").of(it.expression))) })
    }
  }

  private fun tally(player: HasClassName, awardType: Type): Expression =
      cn("AwardTally").of(player.className.expression, awardType.expression)

  private fun cardFromClassType(cardClassType: Type, reader: GameReader): CardDefinition {
    require(cardClassType.className == CLASS)
    val cardName = cardClassType.expression.arguments.single().className
    return reader.tfmRuleset.card(cardName)
  }

  private fun card(type: HasClassName, reader: GameReader): CardDefinition =
      reader.tfmRuleset.card(type.className)
}
