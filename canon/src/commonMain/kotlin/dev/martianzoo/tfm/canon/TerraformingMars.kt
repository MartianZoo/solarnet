@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.CustomMetric
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.DIE
import dev.martianzoo.data.Player
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Transforming.replaceOwnerWith
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
import dev.martianzoo.pets.ast.Instruction.Transform as InstructionTransform
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.And
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.pets.ast.Requirement.Exact
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.Requirement.Or
import dev.martianzoo.pets.ast.Requirement.Transform as RequirementTransform
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.api.ApiUtils.getPlayerOwner
import dev.martianzoo.tfm.api.ApiUtils.mapDefinition
import dev.martianzoo.tfm.api.tfmAuthority
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.data.TfmClasses.PROD
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
        TerraformingMars.TallyAward,
        TerraformingMars.AssignAwardPlaces,
        TerraformingMars.MultiplayerVictoryCheck,
        TerraformingMars.MarsRow,
        TerraformingMars.CardCost,
        TerraformingMars.CitationsIgnoringRemoves,
        TerraformingMars.CardRequirement,
        TerraformingMars.ClassCardRequirement,
        TerraformingMars.StandardProjectCost,
        TerraformingMars.MapBonus,
        TerraformingMars.CopyProductionBox,
    )

/** Namespace for the core game's custom Pets implementations. */
internal object TerraformingMars {
  internal object CopyProductionBox : CustomClass() {
    override fun translate(reader: GameReader, owner: Type, cardType: Type): Instruction {
      val card: CardDefinition = reader.tfmAuthority.card(cardType.className)
      val immediate =
          card.immediate
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
      val action = parse<Action>(game.tfmAuthority.action(projectName).actions.single())
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

  private val NEIGHBOR = cn("Neighbor")
  private val FORWARD_ADJACENCY = cn("ForwardAdjacency")
  private val BACKWARD_ADJACENCY = cn("BackwardAdjacency")

  internal object CreateAdjacencies : CustomClass() {
    override val requiredClassNames: Set<ClassName> =
        setOf(NEIGHBOR, FORWARD_ADJACENCY, BACKWARD_ADJACENCY)

    override fun translate(reader: GameReader, areaType: Type): Instruction {
      val grid: Grid<AreaDefinition> = mapDefinition(reader).areas
      val area = grid.firstOrNull { it.className == areaType.className } ?: error(areaType)
      val neighborAreas: List<AreaDefinition> = grid.hexNeighbors(area.row, area.column)

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
      val deck = cardFromClassType(cardFrontClassType, reader).deck
      return if (cardBackClassType.expression.arguments.single().className == deck?.className) {
        NoOp
      } else {
        gain(DIE)
      }
    }
  }

  internal object CheckCardRequirement : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(REQUIRED, GLOBAL_PARAMETER)

    override fun translate(
        reader: GameReader,
        owner: Type,
        cardClassType: Type,
    ): Instruction {
      val requirement =
          cardFromClassType(cardClassType, reader).requirement?.let {
            replaceOwnerWith(Player(owner.className)).transform(it)
          } ?: return NoOp
      if (requirement.canEvaluateDirectly() && reader.has(requirement)) return NoOp

      return requirement.globalParameterShortfall(reader)?.let { (parameter, count) ->
        gain(REQUIRED.of(CLASS.of(parameter)), count)
      } ?: Gated.create(requirement, NoOp)
    }

    private fun Requirement.globalParameterShortfall(reader: GameReader): Pair<Expression, Int>? {
      val counting = this as? Counting ?: return null
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
      check(shortfall > 0)
      return parameter to shortfall
    }

    private fun Requirement.canEvaluateDirectly(): Boolean =
        when (this) {
          is Counting -> true
          is Or -> requirements.all { it.canEvaluateDirectly() }
          is And -> requirements.all { it.canEvaluateDirectly() }
          is RequirementTransform -> false
        }
  }

  private val OWED = cn("Owed")
  private val PLAY_TAG = cn("PlayTag")
  private val REQUIRED = cn("Required")
  private val GLOBAL_PARAMETER = cn("GlobalParameter")

  internal object HandleCardCost : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(OWED, PLAY_TAG)

    override fun translate(
        reader: GameReader,
        owner: Type,
        cardFrontClassType: Type,
    ): Instruction {
      val card = cardFromClassType(cardFrontClassType, reader)
      if (card.cost == 0) return NoOp

      val playTagSignals =
          card.tags.entries.map { (tagName, count) ->
            gain(PLAY_TAG.of(tagName.classExpression()), count)
          }
      val instructions = listOf(gain(OWED, card.cost)) + playTagSignals
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

  private val AWARD_TALLY = cn("AwardTally")
  private val FIRST_PLACE = cn("FirstPlace")
  private val SECOND_PLACE = cn("SecondPlace")

  internal object TallyAward : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(AWARD_TALLY)

    override fun translate(reader: GameReader, owner: Type, awardType: Type): Instruction {
      val metric = reader.tfmAuthority.award(awardType.className).metric
      return parse("AwardTally<${owner.className}, ${awardType.className}> / ($metric)")
    }
  }

  internal object AssignAwardPlaces : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(AWARD_TALLY, FIRST_PLACE, SECOND_PLACE)

    override fun translate(reader: GameReader, awardType: Type): Instruction {
      val players = reader.getComponents("Player").elements
      val scores = players.associateWith {
        reader.count(reader.resolve(tally(it, awardType)))
      }
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
                    .filter { (player, score) ->
                      player !in first && score == secondScore
                    }
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
      val megacredits = leaders.associateWith {
        reader.count(reader.resolve(cn("Megacredit").of(it.expression)))
      }
      val mostMegacredits = megacredits.values.maxOrNull() ?: return NoOp
      val winners = megacredits.filterValues { it == mostMegacredits }.keys
      return Then.create(winners.map { gain(VICTORY.of(it.expression)) })
    }
  }

  private fun tally(player: HasClassName, awardType: Type): Expression =
      AWARD_TALLY.of(player.className.expression, awardType.expression)

  private fun cardFromClassType(cardClassType: Type, reader: GameReader): CardDefinition {
    require(cardClassType.className == CLASS)
    val cardName = cardClassType.expression.arguments.single().className
    return reader.tfmAuthority.card(cardName)
  }

  private fun card(type: HasClassName, reader: GameReader): CardDefinition =
      reader.tfmAuthority.card(type.className)
}
