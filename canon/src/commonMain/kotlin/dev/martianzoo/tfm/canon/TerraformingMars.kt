@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.CustomMetric
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.Type
import dev.martianzoo.data.Player
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost.Spend
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
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
import dev.martianzoo.util.Grid

internal val baseCustomClasses: Set<CustomClass> =
    setOf(
        TerraformingMars.CreateAdjacencies,
        TerraformingMars.CheckCardDeck,
        TerraformingMars.CheckCardRequirement,
        TerraformingMars.HandleCardCost,
        TerraformingMars.GetEventVps,
        TerraformingMars.PassLeft,
        TerraformingMars.MarsRow,
        TerraformingMars.CardCost,
        TerraformingMars.CardRequirement,
        TerraformingMars.ClassCardRequirement,
        TerraformingMars.StandardProjectCost,
        TerraformingMars.MapBonus,
        TerraformingMars.DistinctTagType,
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

  internal object DistinctTagType : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int = distinctClasses(game, type, cn("Tag"))
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
    override fun translate(reader: GameReader, owner: Type, cardFrontClassType: Type): Instruction {
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

  private fun cardFromClassType(cardClassType: Type, reader: GameReader): CardDefinition {
    require(cardClassType.className == CLASS)
    val cardName = cardClassType.expression.arguments.single().className
    return reader.tfmRuleset.card(cardName)
  }

  private fun card(type: HasClassName, reader: GameReader): CardDefinition =
      reader.tfmRuleset.card(type.className)
}

internal fun distinctClasses(
    game: GameReader,
    metricType: Type,
    componentClass: ClassName,
): Int {
  val owner = metricType.expressionFull.arguments.single()
  return game
      .getComponents(game.resolve(componentClass.of(owner)))
      .map { it.className }
      .toSet()
      .size
}
