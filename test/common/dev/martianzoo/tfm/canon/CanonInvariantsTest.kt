package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Transform as InstructionTransform
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Class
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Canon invariants checked during the build, not universal Terraforming Mars rules. A failure names
 * cards whose addition or rewrite requires revisiting the invariant and its consumers. See
 * docs/agents/GAME_HACKS.md for the corresponding composition hazards.
 */
internal class CanonInvariantsTest {
  private val table = Canon.classTable
  private val cards = Canon.cards

  // GAME_HACKS 3: stored MC production includes the production-floor offset.
  @Test
  internal fun perMoneyProductionAccountsForItsOffset() {
    val dispatcher =
        TransformHandler.dispatcher(
            Canon.transformHandlerFactories.mapValues { (_, factory) -> factory(table) }
        )
    val offenders =
        Canon.explicitClassDeclarations.flatMap { declaration ->
          declaration.allNodes.flatMap { authored ->
            val lowered = dispatcher.transformWithoutKindCheck(authored)
            val metrics =
                lowered.descendantsOfType<Instruction.Per>().map { it.metric } +
                    lowered.descendantsOfType<Action.Cost.Per>().map { it.metric }
            metrics
                .filter { unadjustedMoneyProduction(it, table).isNotEmpty() }
                .map { "${declaration.className}: / $it" }
          }
        }
    assertTrue(
        offenders.isEmpty(),
        "Per-money-production arithmetic needs its matching ProdOffset:\n${offenders.distinct().joinToString("\n")}",
    )
  }

  @Test
  internal fun cardCategoriesMatchTheirTagsAndPersistentBehavior() {
    validateCardClassification(Canon)
  }

  @Test
  internal fun animalHoldersHaveAnAnimalTag() {
    checkCards("Animal holders need an AnimalTag", holders("Animal")) { hasTag(it, "AnimalTag") }
  }

  @Test
  internal fun resourceHoldersWithAnimalTagsHoldAnimals() {
    checkCards(
        "Animal-tagged resource holders must hold Animal",
        resourceCards().filter { hasTag(it, "AnimalTag") },
    ) {
      cardResourceType(it) == cn("Animal")
    }
  }

  @Test
  internal fun microbeHoldersHaveAMicrobeTag() {
    checkCards("Microbe holders need a MicrobeTag", holders("Microbe")) { hasTag(it, "MicrobeTag") }
  }

  @Test
  internal fun resourceCardsIdentifyAConcreteCardResource() {
    checkCards("ResourceCard needs an unambiguous concrete CardResource", resourceCards()) { card ->
      cardResourceType(card)?.let(table::getClass)?.let { resource ->
        !resource.abstract && resource.isSubtypeOf(table.getClass(cn("CardResource")))
      } == true
    }
  }

  @Test
  internal fun resourceHoldingProjectsAreActiveCards() {
    checkCards(
        "Resource-holding projects must be ActiveCard",
        resourceCards().filter { cardBack(it)?.className == cn("ProjectCard") },
    ) {
      it.isSubtypeOf(table.getClass(cn("ActiveCard")))
    }
  }

  @Test
  internal fun actionCardRoleMatchesAuthoredActions() {
    checkCards("ActionCard must agree with authored actions", cards) {
      it.isSubtypeOf(table.getClass(cn("ActionCard"))) == cardActions(it).isNotEmpty()
    }
  }

  @Test
  internal fun eventCardsHaveNoActionsOrResourceStorage() {
    checkCards("Events leave play and cannot retain actions or resource storage", eventCards()) {
      cardActions(it).isEmpty() && cardResourceType(it) == null
    }
  }

  // GAME_HACKS 12: CopyProductionBox extracts a subtree and discards its ancestors.
  @Test
  internal fun buildingCardsHaveAtMostASingleImmediateProductionBox() {
    checkCards("CopyProductionBox cannot copy multiple PROD blocks", buildingCards()) {
      cardProductionBoxes(it).size <= 1
    }
  }

  @Test
  internal fun copiedProductionBoxesDoNotReferToTheOriginalCardAsThis() {
    checkCards("CopyProductionBox does not bind This to the original card", buildingCards()) { card
      ->
      cardProductionBoxes(card).all { THIS !in it.descendantsOfType<ClassName>() }
    }
  }

  @Test
  internal fun copiedProductionBoxesDoNotLoseConditionsChoicesOrBindings() {
    checkCards(
        "CopyProductionBox would lose a surrounding condition, choice, multiplier, actor, or EACH; put it inside PROD",
        buildingCards(),
    ) { card ->
      // THEN is intentional for Mining Area/Rights: copying repeats production after the tile
      // already exists. Conditions, choices, scaling and bindings must travel with the box.
      var valid = true
      cardImmediate(card)?.visitDescendants { node ->
        if (node is InstructionTransform && node.transformKind == TfmClasses.PROD) {
          false
        } else {
          if (
              (node is Instruction.Gated ||
                  node is Instruction.Or ||
                  node is Instruction.Per ||
                  node is Instruction.By ||
                  node is Instruction.Each) && productionBoxes(node).isNotEmpty()
          ) {
            valid = false
          }
          true
        }
      }
      valid
    }
  }

  // GAME_HACKS 13: CopyPrelude reads only nonautomatic, bare This effects.
  // Boom Town's automatic removal installs a persistent penalty; copying must not repeat it.
  // This checks direct gains, not benefits reached indirectly through support components.
  @Test
  internal fun preludeSelfGainBenefitsAreVisibleToCopyPrelude() {
    checkCards(
        "CopyPrelude ignores automatic gains or wrapped This effects",
        cards.filter { cardBack(it)?.className == cn("PreludeCard") },
    ) { card ->
      card.declaration.authoredEffects.all { effect ->
        Trigger.WhenGain !in effect.untransformed.trigger.descendantsOfType<Trigger>() ||
            (effect.untransformed.trigger == Trigger.WhenGain &&
                (!effect.untransformed.automatic ||
                    effect.untransformed.instruction.descendantsOfType<Instruction.Change>().all {
                      it.gaining == null
                    }))
      }
    }
  }

  // GAME_HACKS 17: GpRequirementShortfall understands a single counting requirement.
  @Test
  internal fun adjustableParameterRequirementsUseASingleCount() {
    val gp = table.getClass(cn("GlobalParameter"))
    checkCards(
        "GpRequirementShortfall cannot adjust a compound global-parameter requirement",
        cards,
    ) { card ->
      val requirement = cardRequirement(card)
      val counts = requirement?.descendantsOfType<Metric.Count>().orEmpty()
      val parameterCounts = counts.filter {
        table.getClass(it.expression.className).isSubtypeOf(gp)
      }
      parameterCounts.isEmpty() ||
          (requirement is Requirement.Counting &&
              requirement.metric == parameterCounts.singleOrNull())
    }
  }

  // GAME_HACKS 18: ScoreEventVps restores only effects whose trigger equals bare End.
  @Test
  internal fun eventScoringUsesBareEndTriggers() {
    val end = parse<Trigger>("End")
    checkCards(
        "ScoreEventVps ignores wrapped End triggers; put the condition inside End:",
        eventCards(),
    ) { card ->
      cardEffects(card).all { effect ->
        effect.untransformed.trigger.descendantsOfType<Trigger.OnGainOf>().none {
          it.expression.className == cn("End")
        } || effect.untransformed.trigger == end
      }
    }
  }

  @Test
  internal fun eventCardsDoNotNeedSubscriptionsAfterLeavingPlay() {
    checkCards(
        "Played events only retain End effects; other subscriptions disappear",
        eventCards(),
    ) { card ->
      cardEffects(card).all { effect ->
        effect.untransformed.trigger.descendantsOfType<Trigger.SubscribedTrigger>().all { trigger ->
          trigger is Trigger.OnGainOf && trigger.expression.className == cn("End")
        }
      }
    }
  }

  // GAME_HACKS 1 and 2: these shortcuts create heat that was never physically gained.
  @Test
  internal fun cardSubscriptionsDoNotObserveHeatGains() {
    val heat = table.getClass(cn("Heat"))
    val offenders = cards.flatMap { card ->
      card.declaration.authoredEffects
          .filter { effect ->
            var observesHeat = false
            effect.untransformed.trigger.visitDescendants { node ->
              if (
                  node is Trigger.OnGainOf &&
                      table.findClass(node.expression.className)?.let(heat::isSubtypeOf) == true
              ) {
                observesHeat = true
              }
              // PROD[Heat] observes production, not heat cubes.
              node !is Trigger.Transform
            }
            observesHeat
          }
          .map { "${card.className}: $it" }
    }
    assertTrue(
        offenders.isEmpty(),
        "Artificial heat would be observable by:\n${offenders.joinToString("\n")}",
    )
  }

  // GAME_HACKS 8 and 9: same-class transfers would repeat placement/building rewards.
  @Test
  internal fun authoredChangesDoNotTransferExistingTilesOrColonies() {
    val roots = listOf("Tile", "Colony").map { table.getClass(cn(it)) }
    val offenders =
        Canon.explicitClassDeclarations.flatMap { declaration ->
          declaration.allNodes
              .flatMap { it.descendantsOfType<Instruction.Transmute>() }
              .filter { change ->
                change.gaining.className == change.removing.className &&
                    table.findClass(change.gaining.className)?.let { klass ->
                      roots.any(klass::isSubtypeOf)
                    } == true
              }
              .map { "${declaration.className}: $it" }
        }
    assertTrue(
        offenders.isEmpty(),
        "Transfers would replay placement/building rewards:\n${offenders.joinToString("\n")}",
    )
  }

  private fun checkCards(reason: String, subjects: Collection<Class>, holds: (Class) -> Boolean) {
    assertTrue(subjects.isNotEmpty(), "No cards examined: $reason")
    val offenders = subjects.filterNot(holds).map { it.className }
    assertTrue(offenders.isEmpty(), "$reason:\n${offenders.joinToString("\n")}")
  }

  private fun hasTag(card: Class, tag: String): Boolean = cn(tag) in cardTags(card).elements

  private fun resourceCards(): List<Class> = cards.filter {
    it.isSubtypeOf(table.getClass(cn("ResourceCard")))
  }

  private fun holders(resource: String): List<Class> = cards.filter {
    cardResourceType(it) == cn(resource)
  }

  private fun eventCards(): List<Class> = cards.filter {
    it.isSubtypeOf(table.getClass(cn("EventCard")))
  }

  private fun buildingCards(): List<Class> = cards.filter { hasTag(it, "BuildingTag") }

  private fun productionBoxes(node: PetNode?): List<InstructionTransform> =
      node?.descendantsOfType<InstructionTransform>().orEmpty().filter {
        it.transformKind == TfmClasses.PROD
      }
}
