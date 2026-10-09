@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.engine

import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Transform as InstructionTransform
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
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.CustomClass
import dev.martianzoo.state.CustomInstruction
import dev.martianzoo.state.CustomMetric
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.toComponent
import dev.martianzoo.tfm.canon.TfmClasses.PROD
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.cardEffects
import dev.martianzoo.tfm.canon.cardImmediate
import dev.martianzoo.tfm.canon.cardTags
import dev.martianzoo.tfm.state.ApiUtils.mapDefinition
import dev.martianzoo.tfm.state.tfmCatalog
import kotlin.math.abs

/** Terraforming Mars runtime entry point and its complete Kotlin implementation set. */
public object TfmEngine {
  private val preludeCard = cn("PreludeCard")
  private val projectCard = cn("ProjectCard")

  /** Starts a live game with every Terraforming Mars custom implementation available. */
  public fun newGame(premise: GamePremise): World = Engine.newGame(premise, customClasses)

  /** Kotlin implementations known to this Terraforming Mars engine. */
  public val customClasses: Set<CustomClass> =
      setOf(
          CopyPrelude,
          CopyProductionBox,
          GainsOf,
          GpRequirementShortfall,
          Neighbor,
          NonNegativeIconsOf,
          PartyDistance,
          PartyRequirement,
          PlacementBonus,
          PlayerDistance,
          PriceAspectCount,
          RepeatPlacementBonus,
          ScoreEventVps,
          TileInLargestGroup,
      )

  private object CopyPrelude : CustomInstruction() {
    override fun translate(reader: GameReader, owner: Type, cardType: Type): InstructionTree {
      val card = reader.tfmCatalog.card(cardType.className)
      if (cardBack(card)?.className != preludeCard) {
        throw NarrowingException("Card ${card.className} is not a prelude card")
      }
      if (card.className == cn("DoubleDown")) {
        throw NarrowingException("Cute, but Double Down can't copy itself")
      }
      val immediate = cardImmediate(card) ?: return NoOp
      return PetElaborator(reader.classTable)
          .specializeEffect(
              card.defaultType,
              card.defaultType,
              Effect(WhenGain, immediate),
              cn("DoubleDown").expression,
          )
          .instruction
    }
  }

  private object CopyProductionBox : CustomInstruction() {
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

  private object GainsOf : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val (subject, target) = type.typeDependencies.map { it.boundType }
      val definition = requireNotNull(subject.representedClass).declaration
      val targetClass = requireNotNull(target.representedClass)
      return definition.authoredEffects.sumOf { effect ->
        effect.instruction.descendantsOfType<Instruction.Change>().count { change ->
          val gained = change.gaining ?: return@count false
          game.classTable.getClass(gained.className).isSubtypeOf(targetClass)
        }
      }
    }
  }

  private object GpRequirementShortfall : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val (cardClassType, parameterClassType) = type.typeDependencies.map { it.boundType }
      val requirement =
          cardRequirement(representedType(cardClassType, game)) as? Counting ?: return 0
      val counted = requirement.metric as? Metric.Count ?: return 0
      if (game.resolve(counted.expression).rootClass != parameterClassType.representedClass)
          return 0

      val actual = game.count(counted)
      return when (requirement) {
        is Min -> (requirement.target - actual).coerceAtLeast(0)
        is Max -> (actual - requirement.target).coerceAtLeast(0)
        is Exact -> abs(actual - requirement.target)
      }
    }
  }

  private object Neighbor : CustomMetric() {
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

  private object NonNegativeIconsOf : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val (cardType, targetClassType) = type.typeDependencies.map { it.boundType }
      val effects = cardEffects(card(cardType, game))
      val target = requireNotNull(targetClassType.representedClass).className
      return effects.sumOf { citationsOutsideRemoval(it, target) }
    }
  }

  private object PartyDistance : CustomMetric() {
    private val afterParty = cn("AfterParty")

    override fun count(game: GameReader, type: Type): Int {
      val (source, target) = type.typeDependencies.map { it.boundType }
      if (source == target) return 0

      val relations = game.getComponents(game.resolve(afterParty.expression))
      val successors =
          relations.elements.associate { relation ->
            val (before, after) = relation.typeDependencies.map { it.boundType }
            before to after
          }
      require(successors.size == relations.size) { "AfterParty must have one successor per Party" }

      var current = source
      for (distance in 1..successors.size) {
        current = requireNotNull(successors[current]) { "$current has no AfterParty successor" }
        if (current == target) return distance
      }
      error("$target is not reachable from $source through AfterParty")
    }
  }

  private object PartyRequirement : CustomMetric() {
    private val party = cn("Party")
    private val partyDelegate = cn("PartyDelegate")
    private val ruling = cn("Ruling")

    override fun count(game: GameReader, type: Type): Int {
      val partyType = game.resolve(party.expression)
      val partyTypeArgument =
          type.typeDependencies.map { it.boundType }.single { it.narrows(partyType, game) }
      val player = checkNotNull(type.toComponent().owningPlayer)
      val isRuling = game.count(game.resolve(ruling.of(partyTypeArgument.expression))) == 1
      val delegates =
          game.count(
              game.resolve(partyDelegate.of(partyTypeArgument.expression, player.expression))
          )
      return if (isRuling || delegates >= 2) 1 else 0
    }
  }

  private object PlacementBonus : CustomMetric() {
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

  private object PlayerDistance : CustomMetric() {
    private val afterMe = cn("AfterMe")

    override fun count(game: GameReader, type: Type): Int {
      val (source, target) = type.typeDependencies.map { it.boundType }
      val relations = game.getComponents(game.resolve(afterMe.expression))
      val successors =
          relations.elements.associate { relation ->
            val (before, after) = relation.typeDependencies.map { it.boundType }
            before to after
          }
      require(successors.size == relations.size) { "AfterMe must have one successor per Player" }

      if (target !in successors) return successors.size
      if (source !in successors) return 0
      if (source == target) return 0
      var current = source
      for (distance in 1..successors.size) {
        current = requireNotNull(successors[current]) { "$current has no AfterMe successor" }
        if (current == target) return distance
      }
      error("$target is not reachable from $source through AfterMe")
    }
  }

  private object PriceAspectCount : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val (cardClassType, aspectClassType) = type.typeDependencies.map { it.boundType }
      val card = cardFromClassType(cardClassType, game)
      if (cardBack(card)?.isSubtypeOf(card.classTable.getClass(projectCard)) != true) return 0
      val aspect = requireNotNull(aspectClassType.representedClass)
      return if (aspect.className == card.className) 1 else cardTags(card).count(aspect.className)
    }
  }

  private object RepeatPlacementBonus : CustomInstruction() {
    override fun translate(game: GameReader, type0: Type, type1: Type): InstructionTree {
      val map = mapDefinition(game)
      val areaNames = map.areas.mapTo(hashSetOf()) { it.className }
      val area = listOf(type0, type1).single { it.className in areaNames }
      val bonus = map.areas.single { it.className == area.className }.bonus ?: return NoOp
      return InstructionGroup.createTree(bonus.instructions + bonus.instructions)
    }
  }

  private object ScoreEventVps : CustomInstruction() {
    private val end: Trigger = parse("End")

    override fun translate(
        reader: GameReader,
        ignoredOwningType: Type,
        classType: Type,
    ): InstructionTree {
      val effects = cardEffects(cardFromClassType(classType, reader))
      return InstructionGroup.of(effects.filter { it.trigger == end }.map { it.instruction })
    }
  }

  private object TileInLargestGroup : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val player = checkNotNull(type.toComponent().owningPlayer)
      val areas = mapDefinition(game).areas
      val areasByName = areas.associateBy { it.className }
      val ownedTiles = game.getComponents(game.resolve(cn("OwnedTile").of(player.expression)))
      val ownedAreas =
          ownedTiles.mapNotNullTo(linkedSetOf()) { tile ->
            tile.typeDependencies.firstNotNullOfOrNull { dependency ->
              areasByName[dependency.boundType.className]
            }
          }
      return areas.largestContiguousGroupSize(ownedAreas, { it.row }, { it.column })
    }
  }

  private fun citationsOutsideRemoval(effect: Effect, target: ClassName): Int {
    var count = 0
    effect.visitDescendants { node ->
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

  private fun cardFromClassType(cardClassType: Type, reader: GameReader): Class =
      reader.tfmCatalog.card(representedType(cardClassType, reader).className)

  private fun representedType(classType: Type, reader: GameReader): Type {
    require(classType.className == CLASS)
    return reader.resolve(requireNotNull(classType.representedClass).className.expression)
  }

  private fun card(type: HasClassName, reader: GameReader): Class =
      reader.tfmCatalog.card(type.className)

  private fun cardRequirement(cardType: Type): Requirement? =
      cardType.getRequirementPropertyValue("requirement")
}
