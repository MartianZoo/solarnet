package dev.martianzoo.tfm.canon

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.GameWriter
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.PROD
import dev.martianzoo.tfm.api.SpecialClassNames.RAW
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.raw
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.pets.ast.TransformNode
import dev.martianzoo.util.Grid

internal val allCustomInstructions =
    setOf(
        ForceLoad,
        CreateAdjacencies,
        BeginPlayCard,
        GetVpsFrom,
        CopyPrelude,
        GainLowestProduction,
        CopyProductionBox,
    )

private object ForceLoad : CustomInstruction("forceLoad") { // TODO include @ ?
  override fun execute(
      game: GameReader,
      writer: GameWriter,
      arguments: List<Type>,
      multiplier: Int,
  ) {
    // This one legitimately doesn't have to do anything!
  }
}

// MarsArea has `Tile<This>:: @createAdjacencies(This)`
private object CreateAdjacencies : CustomInstruction("createAdjacencies") {
  override fun translate(game: GameReader, arguments: List<Type>): Instruction {
    val areaName: ClassName = arguments.single().className
    val grid: Grid<AreaDefinition> = game.setup.map.areas
    val area: AreaDefinition = grid.first { it.className == areaName }
    val neighborAreas: List<AreaDefinition> = neighborsInHexGrid(grid, area.row, area.column)

    val newTile: Expression = tileOn(area, game)!! // creating it is what got us here

    val nbrs: List<Expression> =
        neighborAreas.map { cn("Neighbor").addArgs(newTile, it.className.expr) }
    val adjs =
        neighborAreas
            .mapNotNull { tileOn(it, game) }
            .flatMap {
              listOf(
                  cn("ForwardAdjacency").addArgs(it, newTile),
                  cn("BackwardAdjacency").addArgs(newTile, it))
            }
    return Multi.create((nbrs + adjs).map { Gain(scaledEx(1, it), MANDATORY) })
  }
  private fun tileOn(area: AreaDefinition, game: GameReader): Expression? {
    val tileType: Type = game.resolve(cn("Tile").addArgs(area.className)) // TODO
    return game.getComponents(tileType).singleOrNull()?.expressionFull // TODO
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
    val card: CardDefinition = game.setup.authority.card(cardName)

    // TODO this is super bogus
    val reqt = card.requirement?.let { TransformNode.unwrap(it, RAW) }

    if (reqt?.let(game::evaluate) == false) throw UserException.requirementNotMet(reqt)

    val playTagSignals =
        card.tags.entries.map { (tagName: ClassName, ct: Int) ->
          Gain(scaledEx(ct, cn("PlayTag").addArgs(tagName.classExpression())), MANDATORY)
        }

    val instructions =
        if (card.cost > 0) {
          val instr = Gain(scaledEx(card.cost, cn("Owed").expr), MANDATORY)
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
    require(arguments.size == 2)
    val classExpr = arguments.first().expression
    require(classExpr.className == CLASS)
    val cardName = classExpr.arguments.single().className
    val card = game.setup.authority.card(cardName)
    return Multi.create(
        card.effects
            .map { TransformNode.unwrap(it, RAW) }
            .filter { it.trigger == OnGainOf.create(cn("End").expr) }
            .map { it.instruction })
        .raw()
  }
}

// For Double Down
private object CopyPrelude : CustomInstruction("copyPrelude") {
  override fun execute(
      game: GameReader,
      writer: GameWriter,
      arguments: List<Type>,
      multiplier: Int,
  ) {
    TODO()
  }
}

// For Robinson Industries
private object GainLowestProduction : CustomInstruction("gainLowestProduction") {
  override fun translate(game: GameReader, arguments: List<Type>): Instruction {
    val player = arguments.single()
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game, player.expression)
    val lowest: Int = prods.values.min()
    val keys: Set<ClassName> = prods.filterValues { it == lowest }.keys
    val or = Instruction.Or.create(keys.map { Gain(scaledEx(1, it.expr), MANDATORY) })
    return TransformNode.wrap(or, PROD).raw()
  }
}

// For Robotic Workforce
private object CopyProductionBox : CustomInstruction("copyProductionBox") {
  override fun translate(game: GameReader, arguments: List<Type>): Instruction {
    val def = game.setup.authority.card(arguments.single().className)
    val nodes: List<Transform> = def.immediate?.descendantsOfType() ?: listOf()
    val matches = nodes.filter { it.transformKind == PROD }

    when (matches.size) {
      1 -> return matches.first()
      0 -> throw RuntimeException("There is no immediate PROD box on ${def.className}")
      else ->
        throw RuntimeException(
            "The immediate instructions on ${def.className} " +
                "have multiple PROD boxes, which should never happen") // TODO validate sooner?
    }
  }
}
