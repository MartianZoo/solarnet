@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon.bundles.TerraformingMars

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.Type
import dev.martianzoo.data.Player
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.FromExpression.ComplexFrom
import dev.martianzoo.pets.ast.FromExpression.ExpressionAsFrom
import dev.martianzoo.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.pets.ast.Instruction
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
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.data.TfmClasses.TILE
import dev.martianzoo.util.Grid

internal object CreateAdjacencies : CustomClass("CreateAdjacencies") {
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

internal object CheckCardDeck : CustomClass("CheckCardDeck") {
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

internal object CheckCardRequirement : CustomClass("CheckCardRequirement") {
  override fun translate(reader: GameReader, owner: Type, cardClassType: Type) =
      Gated.create(cardFromClassType(cardClassType, reader).requirement, NoOp)
}

internal object HandleCardCost : CustomClass("HandleCardCost") {
  override fun translate(reader: GameReader, owner: Type, cardFrontClassType: Type): Instruction {
    val card = cardFromClassType(cardFrontClassType, reader)
    if (card.cost == 0) return NoOp

    val playTagSignals =
        card.tags.entries.map { (tagName, count) ->
          gain(scaledEx(count, cn("PlayTag").of(tagName.classExpression())))
        }
    val instructions =
        if (card.cost > 0) listOf(gain(scaledEx(card.cost, cn("Owed")))) + playTagSignals
        else playTagSignals
    return Then.create(instructions)
  }
}

internal object GetEventVps : CustomClass("GetEventVps") {
  override fun translate(reader: GameReader, ignoredOwner: Type, classType: Type): Instruction {
    val effects = cardFromClassType(classType, reader).effects
    return Multi.create(effects.filter { it.trigger == end }.map { it.instruction })
  }

  private val end: Trigger = parse("End")
}

internal object PassLeft : CustomClass("PassLeft") {
  override fun translate(reader: GameReader, component: Type): Instruction {
    val currentOwner: Player = getPlayerOwner(reader, component)
    val current = currentOwner.toString().removePrefix("Player").toInt()
    val playerCount: Int = reader.count(parse<Metric>("Player"))
    if (playerCount == 1) return NoOp

    val next = current % playerCount + 1
    val arguments: List<FromExpression> =
        component.expressionFull.arguments.map {
          if (it == currentOwner.expression) {
            SimpleFrom(cn("Player$next").expression, currentOwner.expression)
          } else {
            ExpressionAsFrom(it)
          }
        }
    return Transmute(ComplexFrom(component.className, arguments), ActualScalar(1))
  }
}

private fun cardFromClassType(cardClassType: Type, reader: GameReader): CardDefinition {
  require(cardClassType.className == CLASS)
  val cardName = cardClassType.expression.arguments.single().className
  return (reader.ruleset as TfmRuleset).card(cardName)
}
