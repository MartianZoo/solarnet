@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SpecialClassNames.CLASS
import dev.martianzoo.api.SpecialClassNames.DIE
import dev.martianzoo.api.Type
import dev.martianzoo.tfm.api.ApiUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.ApiUtils.mapDefinition
import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.Deck.PRELUDE
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.data.TfmClassNames.MEGACREDIT
import dev.martianzoo.tfm.data.TfmClassNames.PROD
import dev.martianzoo.tfm.data.TfmClassNames.TILE
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.pets.ast.TransformNode
import dev.martianzoo.util.Grid

internal val canonCustomClasses =
    setOf(
        CreateAdjacencies,
        CheckCardDeck,
        CheckCardRequirement,
        HandleCardCost,
        GetEventVps,
        GainLowestProduction,
        CopyProductionBox,
        CopyPrelude,
    )

private object CreateAdjacencies : CustomClass("CreateAdjacencies") {
  override fun translate(reader: GameReader, areaType: Type): Instruction {
    val grid: Grid<AreaDefinition> = mapDefinition(reader).areas
    val area = grid.firstOrNull { it.className == areaType.className } ?: error(areaType)
    val neighborAreas: List<AreaDefinition> = neighborsInHexGrid(grid, area.row, area.column)
    val newTile: Expression = tileOn(area, reader)!! // creating it is what got us here

    val neighbors: List<Expression> =
        neighborAreas.map { cn("Neighbor").of(newTile, it.className.expression) }
    val adjacencies: List<Expression> =
        neighborAreas
            .mapNotNull { tileOn(it, reader) }
            .flatMap {
              listOf(
                  cn("ForwardAdjacency").of(it, newTile),
                  cn("BackwardAdjacency").of(newTile, it),
              )
            }
    // I don't think we care whether this returns Multi or Then
    return Then.create((neighbors + adjacencies).map { gain(scaledEx(1, it)) })
  }

  private fun tileOn(area: AreaDefinition, reader: GameReader): Expression? {
    val tileType: Type = reader.resolve(TILE.of(area.className))
    val tiles = reader.getComponents(tileType)
    return tiles.singleOrNull()?.expressionFull
  }

  fun <E> neighborsInHexGrid(grid: Grid<E>, r: Int, c: Int): List<E> {
    return listOfNotNull(
        grid[r - 1, c - 1],
        grid[r - 1, c + 0],
        grid[r + 0, c - 1],
        grid[r + 0, c + 1],
        grid[r + 1, c + 0],
        grid[r + 1, c + 1],
    )
  }
}

private object CheckCardDeck : CustomClass("CheckCardDeck") {
  override fun translate(
      reader: GameReader,
      cardBackClassType: Type,
      cardFrontClassType: Type
  ): Instruction {
    val deck = cardFromClassExpression(cardFrontClassType, reader).deck
    return if (cardBackClassType.expression.arguments.single().className == deck?.className) {
      NoOp
    } else {
      parse("$DIE!")
    }
  }
}

private object CheckCardRequirement : CustomClass("CheckCardRequirement") {
  override fun translate(reader: GameReader, owner: Type, cardClassType: Type): Instruction {
    val reqt = cardFromClassExpression(cardClassType, reader).requirement
    return Gated.create(reqt, NoOp)
  }
}

private object HandleCardCost : CustomClass("HandleCardCost") {
  override fun translate(
      reader: GameReader,
      owner: Type,
      cardFrontClassType: Type
  ): Instruction {
    val cardType: Expression = cardFrontClassType.expression.arguments.single()
    val card: CardDefinition = reader.authority.card(cardType.className)
    if (card.cost == 0) return NoOp

    val playTagSignals =
        card.tags.entries.map { (tagName: ClassName, ct: Int) ->
          gain(scaledEx(ct, cn("PlayTag").of(tagName.classExpression())))
        }

    val instructions =
        if (card.cost > 0) {
          val instr = gain(scaledEx(card.cost, cn("Owed")))
          listOf(instr) + playTagSignals
        } else {
          playTagSignals
        }
    return Then.create(instructions) // in case any PlayTag discounts are ::
  }
}

private fun cardFromClassExpression(cardClassType: Type, reader: GameReader): CardDefinition {
  val cardType: Expression = cardClassType.expression.arguments.single()
  val card: CardDefinition = reader.authority.card(cardType.className)
  return card
}

// For scoring event cards
private object GetEventVps : CustomClass("GetEventVps") {
  override fun translate(reader: GameReader, ignoredOwner: Type, classType: Type): Instruction {
    require(classType.className == CLASS)
    val cardName = classType.expression.arguments.single().className
    val card = reader.authority.card(cardName)
    val endFx = card.effects.filter { it.trigger == end }
    return Multi.create(endFx.map { it.instruction })
  }
  val end: Trigger = parse("End")
}

// For Robinson Industries
private object GainLowestProduction : CustomClass("GainLowestProduction") {
  override fun translate(reader: GameReader, owner: Type): Instruction {
    val lowest = lookUpProductionLevels(reader, owner.expression).values.min()

    val options =
        standardResourceNames(reader).mapNotNull {
          val target = if (it == MEGACREDIT) lowest + 5 else lowest
          if (target >= 0) parse<Instruction>("=$target $it: $it") else null
        }
    // A big and gross instruction, but preparing it prunes it down
    return TransformNode.wrap(Or.create(options), PROD)
  }
}

// For Robotic Workforce
private object CopyProductionBox : CustomClass("CopyProductionBox") {
  override fun translate(reader: GameReader, owner: Type, cardType: Type): Instruction {
    val card: CardDefinition = reader.authority.card(cardType.className)
    val immediate =
        card.immediate
            ?: throw NarrowingException("card ${card.className} has no immediate instruction")
    val matches = immediate.descendantsOfType<Transform>().filter { it.transformKind == PROD }

    when (matches.size) {
      0 -> throw NarrowingException("must choose a card that has an immediate PROD box")
      1 -> return matches.first()
      else -> error("Card ${card.className} is malformed, has ${matches.size} PROD blocks")
    }
  }
}

// For Double Down
private object CopyPrelude : CustomClass("CopyPrelude") {
  override fun translate(reader: GameReader, owner: Type, cardType: Type): Instruction {
    val card = reader.authority.card(cardType.className)
    if (card.deck != PRELUDE) {
      throw NarrowingException("Card ${card.className} is not a prelude card")
    }
    if (cardType.className == cn("DoubleDown")) { // TODO another way to get this behavior?
      throw NarrowingException("Cute. No, you can't copy Double Down itself")
    }
    return card.immediate ?: NoOp
  }
}
