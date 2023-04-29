package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.ApiUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.ApiUtils.mapDefinition
import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.PROD
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.raw
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.unraw
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.pets.ast.TransformNode
import dev.martianzoo.util.Grid

internal val allCustomInstructions =
    setOf(
        ForceLoad,
        CreateAdjacencies,
        BeginPlayCard,
        GetVpsFrom,
        GainLowestProduction,
        CopyProductionBox,
    )

private object ForceLoad : CustomInstruction("forceLoad") {
  override fun translate(game: GameReader, arguments: List<Type>): Instruction {
    return NoOp
  }
}

// MarsArea has `Tile<This>:: @createAdjacencies(This)`
private object CreateAdjacencies : CustomInstruction("createAdjacencies") {
  override fun translate(game: GameReader, arguments: List<Type>): Instruction {
    val grid = mapDefinition(game).areas
    val areaName: ClassName = arguments.single().className
    val area: AreaDefinition = grid.firstOrNull { it.className == areaName } ?: error(areaName)
    val neighborAreas: List<AreaDefinition> = neighborsInHexGrid(grid, area.row, area.column)

    val newTile: Expression = tileOn(area, game)!! // creating it is what got us here

    val nbrs: List<Expression> =
        neighborAreas.map { cn("Neighbor").of(newTile, it.className.expression) }
    val adjs =
        neighborAreas
            .mapNotNull { tileOn(it, game) }
            .flatMap {
              listOf(
                  cn("ForwardAdjacency").of(it, newTile), cn("BackwardAdjacency").of(newTile, it))
            }
    return Multi.create((nbrs + adjs).map { gain(scaledEx(1, it)) })
  }

  private fun tileOn(area: AreaDefinition, game: GameReader): Expression? {
    val tileType: Type = game.resolve(cn("Tile").of(area.className))
    val tiles = game.getComponents(tileType)

    // TODO invariants should have already taken care of this
    if (tiles.size > 1) throw UserException("Two tiles on same area")
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

private object BeginPlayCard : CustomInstruction("beginPlayCard") {
  override fun translate(game: GameReader, arguments: List<Type>): Instruction {
    val cardType: Type = arguments.single()
    val cardName = cardType.expression.className
    val card: CardDefinition = game.authority.card(cardName)

    val reqt = card.requirement?.unraw()

    if (reqt?.let(game::evaluate) == false) throw UserException.requirementNotMet(reqt)

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
    return Multi.create(instructions)
  }
}

// For scoring event cards
private object GetVpsFrom : CustomInstruction("getVpsFrom") {
  override fun translate(game: GameReader, arguments: List<Type>): Instruction {
    val clazz = arguments.single()
    require(clazz.className == CLASS)
    val cardName = clazz.expression.arguments.single().className
    val card = game.authority.card(cardName)
    return NoOp // bug #6
    // return Multi.create(
    //         card.effects
    //             .map { TransformNode.unwrap(it, RAW) }
    //             .filter { it.trigger == OnGainOf.create(cn("End").expression) }
    //             .map { it.instruction })
    //     .raw()
  }
}

// For Robinson Industries
private object GainLowestProduction : CustomInstruction("gainLowestProduction") {
  override fun translate(game: GameReader, arguments: List<Type>): Instruction {
    val player = arguments.single()
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game, player.expression)
    val lowest: Int = prods.values.min()
    val keys: Set<ClassName> = prods.filterValues { it == lowest }.keys
    val or = Or.create(keys.map { gain(scaledEx(1, it)) })
    return TransformNode.wrap(or, PROD).raw()
  }
}

// For Robotic Workforce
private object CopyProductionBox : CustomInstruction("copyProductionBox") {
  override fun translate(game: GameReader, arguments: List<Type>): Instruction {
    val def = game.authority.card(arguments.single().className)
    val nodes: List<Transform> = def.immediate?.descendantsOfType() ?: listOf()
    val matches = nodes.filter { it.transformKind == PROD }

    when (matches.size) {
      1 -> return matches.first()
      0 -> throw RuntimeException("There is no immediate PROD box on ${def.className}")
      else ->
          throw RuntimeException(
              "The immediate instructions on ${def.className} " +
                  "have multiple PROD boxes, which should never happen")
    }
  }
}
