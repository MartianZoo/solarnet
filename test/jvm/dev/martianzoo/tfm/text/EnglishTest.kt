package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Vocabulary.Companion.defaultEnglishDisplayName
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.Class
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)
  private val cardsByClassName = Canon.cards.associateBy { it.className }
  private val goals = EnglishCardTextData.parse(readEnglishCardText("english-card-text-goals.tsv"))
  private val current =
      EnglishCardTextData.parse(readEnglishCardText("english-card-text-current.tsv"))

  // This characterization is deliberately the sole wording test for every canonical derivation
  // shape. The separate goals file is reviewed target text, not an answer source or test oracle.
  @Test
  internal fun allCardTextMatchesCurrentSnapshot() {
    goals.keys shouldBe cardsByClassName.keys
    current.keys shouldBe cardsByClassName.keys
    current.forEach { (cardFront, expected) ->
      withClue(cardFront.toString()) {
        val card = requireNotNull(cardsByClassName[cardFront])
        val rendering = english.renderCard(card)
        expected.englishName shouldBe
            (goals[cardFront]?.englishName ?: defaultEnglishDisplayName(cardFront))
        rendering.top shouldBe expected.top
        rendering.bottom shouldBe expected.bottom
        countRenderedPetsFallbacks(rendering.top) +
            countRenderedPetsFallbacks(rendering.bottom) shouldBe rendering.unresolved.size
      }
    }
  }

  @Test
  internal fun describesStandalonePetsElements() {
    english.describe(parse<Effect>("End: VictoryPoint / Animal<This>")) shouldBe
        "1 VP per animal on this card."
    english.describe(parse<Effect>("End: VictoryPoint / Animal<This, Owner>")) shouldBe
        "1 VP per animal on this card."
    english.describe(parse<Effect>("CityTile<MarsArea, Anyone>: Steel")) shouldBe
        "When any city tile is placed on Mars, gain 1 steel."
    english.describe(parse<Effect>("PlantTag<CardFront<Anyone>, Anyone>: Steel")) shouldBe
        "When any plant tag is played, gain 1 steel."
    english.describe(parse<Effect>("TerraformRating: 2 MC")) shouldBe
        "When you raise your terraform rating 1 step, gain 2 M€."
    english.describe(parse<Effect>("CardFront(HAS MAX 0 Tag): 4 MC")) shouldBe
        "When you play a card with no tags, gain 4 M€."
    english.describe(parse<Effect>("CardFront(HAS =1 Tag): MC")) shouldBe
        "When you play a card with exactly 1 tag, gain 1 M€."
    english.describe(parse<Effect>("CardFront(HAS 2 Tag): Science<This>")) shouldBe
        "When you play a card with 2 or more tags, add 1 science resource to this card."
    english.describe(
        parse<Effect>("AnimalTag<CardFront<Anyone>, Anyone>: -2 Heat<Anyone>?")
    ) shouldBe "When any animal tag is played, you may remove up to 2 heat from that player."
    english.describe(listOf(parse<Action>("4 Energy -> 2 Steel, OxygenStep"))) shouldBe
        "Pay 4 energy to gain 2 steel and raise oxygen 1 step."
    english.describe(listOf(parse<Action>("Animal<This, Owner> -> Steel"))) shouldBe
        "Remove 1 animal from this card to gain 1 steel."
    english.describe(listOf(parse<Action>("MC -> Animal<This>?"))) shouldBe "[MC -> Animal<This>?]."
    english.describe(parse<InstructionTree>("2 Plant, TemperatureStep")) shouldBe
        "Gain 2 plants. Raise temperature 1 step."
    english.describe(parse<InstructionTree>("-3 MC THEN TemperatureStep")) shouldBe
        "Pay 3 M€ to raise temperature 1 step."
    english.describe(parse<InstructionTree>("-2 Plant")) shouldBe "Remove 2 plants."
    english.describe(parse<InstructionTree>("Animal<Owner, This>?")) shouldBe
        "You may add up to 1 animal to this card."
    english.describe(
        parse<InstructionTree>("3 MC<Anyone> FROM MC."),
    ) shouldBe "Pay 3 M€ to any player, or as much as possible."
    english.describe(parse<Requirement>("MAX 6 OxygenStep")) shouldBe
        "Requires that oxygen is 6% or lower."
    english.describe(
        parse<Effect>("Invoice<ConvertPlants, Action1>:: -Owed<Class<Plant>>")
    ) shouldBe "When you convert plants to greenery, pay 1 plant less."
    english.describe(parse<Effect>("Billing<CardPlay>:: -2 Owed<>")) shouldBe
        "When you play a card, pay 2 M€ less."
    english.describe(
        parse<Effect>("CardInvoice<Class<CardFront>(HAS requirement)>:: -2 Owed<>")
    ) shouldBe "When you play a card with a requirement, pay 2 M€ less."
    english.describe(parse<Effect>("BuyCard:: 2 Owed<>")) shouldBe
        "When you buy a card, pay 2 M€ extra."
    english.describe(
        parse<Effect>("UseAction<This, Action1>:: Accepting<Class<Titanium>>")
    ) shouldBe "When you pay for this action, titanium may be used."
    english.describe(parse<Effect>("PlayTag<Class<PlanetaryTag>>:: -2 Owed<>")) shouldBe
        "When you play a planetary tag, pay 2 M€ less."
    english.describe(parse<Effect>("PlayTag<Class<EarthTag>>:: -2 Owed<>")) shouldBe
        "When you play an Earth tag, pay 2 M€ less."
    english.describe(parse<InstructionTree>("ProjectCard")) shouldBe "Draw 1 card."
    english.describe(parse<InstructionTree>("OceanTile")) shouldBe "Place 1 ocean tile."
    english.describe(parse<InstructionTree>("CityTile")) shouldBe "Place a city tile."
    english.describe(parse<Requirement>("ScienceTag")) shouldBe "Requires a science tag."
    english.describe(parse<Requirement>("Colony")) shouldBe "Requires that you have a colony."
    english.describe(parse<Requirement>("VenusTag, EarthTag, JovianTag")) shouldBe
        "Requires a Venus tag, an Earth tag, and a Jovian tag."
    english.describe(parse<Requirement>("VenusTag, PlantTag")) shouldBe
        "Requires a Venus tag and a plant tag."
    english.describe(parse<Effect>("End: VictoryPoint / Cathedral<Anyone>")) shouldBe
        "1 VP per any cathedral."

    english.describe(parse<InstructionTree>("Animal")) shouldBe "Add 1 animal to any card."
    english.describe(parse<InstructionTree>("MAX 0 Plant: Steel")) shouldBe
        "If you have no plants, gain 1 steel."
    english.describe(parse<InstructionTree>("OceanTile: Steel")) shouldBe
        "If there is 1 ocean tile, gain 1 steel."
    english.describe(parse<InstructionTree>("2 OceanTile: Steel")) shouldBe
        "If there are 2 ocean tiles, gain 1 steel."
    english.describe(
        parse<InstructionTree>(
            "CARDS[ProjectCard<Revealed> THEN " +
                "((ProjectCard<Revealed>(HAS MAX 0 Tag): Steel) OR Ok)]"
        )
    ) shouldBe "Reveal 1 project card. If it has no tags, gain 1 steel."
  }

  @Test
  internal fun describesSelectedCardProcedures() {
    english.describe(
        parse<InstructionTree>("CopyProductionBox<CardFront(HAS BuildingTag)>")
    ) shouldBe "Copy the immediate production box of a building card."
  }

  @Test
  internal fun describesPositionedComponentConversions() {
    english.describe(
        parse<InstructionTree>("CityTile<LandArea> FROM GreeneryTile<LandArea>")
    ) shouldBe "Change a greenery tile on any land area into a city tile on that land area."
  }

  @Test
  internal fun describesActionUseSignalsAsCommands() {
    english.describe(parse<InstructionTree>("UseAction<ActionCard(HAS ActionUsedMarker)>")) shouldBe
        "Use an action from an action card that has an action-used marker."
  }

  @Test
  internal fun describesCompletedGlobalParameterMetrics() {
    english.describe(parse<InstructionTree>("3 MC / GpComplete")) shouldBe
        "Gain 3 M€ per completed global parameter."
  }

  @Test
  internal fun placesConditionalSelfEffectsWithImmediateText() {
    val card =
        syntheticCard(
            """
            CLASS ConditionalImmediate : AutomatedCard<Class<ProjectCard>> {
              cost = 0
              This IF SoloMode: PROD[2 MC]
            }
            """
        )

    english.topText(card) shouldBe ""
    english.bottomText(card) shouldBe "If this is a solo game, increase your M€ production 2 steps."
  }

  @Test
  internal fun describesBehaviorBearingSubclassesThroughTheirBaseClass() {
    val flexibility =
        syntheticCard(
            """
            CLASS FlexibleNextCard : AutomatedCard<Class<ProjectCard>> {
              cost = 0
              This: NextCardEffect { CheckRequirement:: -2 Required<Class<GlobalParameter>>. }
            }
            """
        )
    val discount =
        syntheticCard(
            """
            CLASS DiscountNextCard : AutomatedCard<Class<ProjectCard>> {
              cost = 0
              This: NextCardEffect { Billing<CardPlay>:: -8 Owed<> }
            }
            """
        )

    English(flexibility.classTable, TerraformingMarsDescribers.descriptions)
        .bottomText(flexibility) shouldBe
        "You may treat the global parameter requirement of the next card you play this generation as if it is 2 steps lower or higher."
    English(discount.classTable, TerraformingMarsDescribers.descriptions)
        .bottomText(discount) shouldBe "The next card you play this generation costs 8 M€ less."
  }

  @Test
  internal fun realizesLaterTypeVariableUsesAsAntecedents() {
    english.describe(parse<Effect>("PROD[StandardResource]: StandardResource")) shouldBe
        "When you increase one of your productions 1 step, gain that resource."
    english.describe(listOf(parse<Action>("PROD[StandardResource] -> 4 StandardResource"))) shouldBe
        "Decrease one of your productions 1 step to gain 4 of that resource."
    english.describe(listOf(parse<Action>("PROD[StandardResource] -> 4 MC"))) shouldBe
        "Decrease one of your productions 1 step to gain 4 M€."
    english.describe(parse<InstructionTree>("StandardResource THEN StandardResource")) shouldBe
        "Gain a standard resource, then gain that resource."
    english.describe(parse<Effect>("Trade<ColonyTile>: ColonyProduction<ColonyTile>?")) shouldBe
        "When you trade, you may raise that colony tile track 1 step."

    val unintroduced =
        syntheticCard(
            """
            CLASS Unintroduced<StandardResource> : ActiveCard<Class<ProjectCard>> {
              cost = 0
              BuyCard: StandardResource
            }
            """
        )
    english.topText(unintroduced) shouldBe "Effect: When you buy a card, gain a standard resource."
  }

  @Test
  internal fun compactAndExpandedTransmutationsRenderIdentically() {
    english.describe(parse<InstructionTree>("2 Steel<Owner FROM Anyone>?")) shouldBe
        english.describe(parse<InstructionTree>("2 Steel<Owner> FROM Steel<Anyone>?"))
  }

  @Test
  internal fun integratesPaymentPermissionIntoItsActionCost() {
    val card =
        syntheticCard(
            """
            CLASS TitaniumAction : ActionCard, ActiveCard<Class<ProjectCard>>, ResourceCard<Class<Asteroid>> {
              cost = 0
              UseAction<This, Action1>:: Accepting<Class<Titanium>>
              12 MC -> OceanTile
              Asteroid<This> -> VenusStep
            }
            """
        )

    english.topText(card) shouldBe
        "Action: Pay 12 M€ (titanium may be used) to place 1 ocean tile, or remove 1 asteroid from this card to raise Venus 1 step."
  }

  @Test
  internal fun cardWithoutTopElementsHasEmptyTopText() {
    val requirementOnly =
        syntheticCard(
            "CLASS RequirementOnly : AutomatedCard<Class<ProjectCard>> { cost = 0; requirement = HAS \"OxygenStep\" }"
        )

    english.topText(requirementOnly) shouldBe ""
  }

  @Test
  internal fun interpretsPlayerOwnedTypesInCardOwnershipContext() {
    english.describe(parse<InstructionTree>("2 MC / Colony")) shouldBe
        "Gain 2 M€ per colony you own."
    english.describe(parse<InstructionTree>("2 MC / Colony<Anyone>")) shouldBe
        "Gain 2 M€ per any colony."
    english.describe(parse<InstructionTree>("2 MC / CityTile<Anyone>")) shouldBe
        "Gain 2 M€ per any city tile."
  }

  @Test
  internal fun describesSpatialPlacementRequirements() {
    val phrases =
        mapOf(
            "HAS 2 Neighbor<CityTile<Anyone>>" to "next to any two city tiles",
            "HAS 3 Neighbor" to "next to any three tiles",
            "HAS MAX 0 Neighbor" to "next to no other tile",
            "HAS MAX 0 Neighbor<CityTile<>>" to "next to no city tile you own",
            "HAS MAX 0 Neighbor<CityTile<Anyone>>" to "next to no city tile",
            "HAS MAX 0 Neighbor<OceanTile>" to "next to no ocean tile",
            "HAS Neighbor<CityTile<>>" to "next to a city tile you own",
            "HAS Neighbor<CityTile<Anyone>>" to "next to any city tile",
            "HAS Neighbor<GreeneryTile<Anyone>>" to "next to any greenery tile",
            "HAS Neighbor<OceanTile>" to "next to an ocean tile",
            "HAS Neighbor<OwnedTile<Anyone>>" to "next to a tile anyone owns",
            "HAS Neighbor<OwnedTile>" to "next to a tile you own",
            "HAS Neighbor<SpecialTile<Anyone>>" to "next to any special tile",
        )

    phrases.forEach { (requirement, phrase) ->
      english.describe(parse<InstructionTree>("CityTile<LandArea($requirement)>")) shouldBe
          "Place a city tile on a land area $phrase."
    }
  }

  @Test
  internal fun cardWithoutBottomElementsHasEmptyBottomText() {
    val actionOnly =
        syntheticCard(
            "CLASS ActionOnly : ActionCard, ActiveCard<Class<ProjectCard>> { cost = 0; -> ProjectCard }"
        )

    english.bottomText(actionOnly) shouldBe ""
  }

  @Test
  internal fun omitsUnconditionalFixedScoresFromCardText() {
    val fixedScore =
        syntheticCard(
            "CLASS FixedScore : AutomatedCard<Class<ProjectCard>> { cost = 0; End: 2 VictoryPoint }"
        )
    val metricScore =
        syntheticCard(
            "CLASS MetricScore : AutomatedCard<Class<ProjectCard>> { cost = 0; End: VictoryPoint / Colony<Anyone> }"
        )

    english.bottomText(fixedScore) shouldBe ""
    english.bottomText(metricScore) shouldBe "1 VP per any colony."
  }

  @Test
  internal fun retainsUnsupportedPetsAlongsideRenderedInstructions() {
    english.describe(parse<InstructionTree>("2 Steel, 3 VictoryPoint")) shouldBe
        "Gain 2 steel. [3 VictoryPoint]."

    val rendering =
        renderInstructionTree(
            parse("2 Steel, 3 VictoryPoint"),
            Describers(Canon.classTable, TerraformingMarsDescribers.descriptions),
        )
    rendering.unresolved.map { it.node.toString() to it.reason } shouldBe
        listOf("3 VictoryPoint" to RefusalReason.UNKNOWN_CHANGE_FRAME)
  }

  @Test
  internal fun retainsUnsupportedPetsWithinPaymentResults() {
    val instruction = parse<InstructionTree>("-2 Steel THEN 3 VictoryPoint")
    val rendering =
        renderInstructionTree(
            instruction,
            Describers(Canon.classTable, TerraformingMarsDescribers.descriptions),
        )

    rendering.value shouldBe "Pay 2 steel to [3 VictoryPoint]."
    rendering.unresolved.map { it.node.toString() to it.reason } shouldBe
        listOf("3 VictoryPoint" to RefusalReason.UNKNOWN_CHANGE_FRAME)
  }

  @Test
  internal fun tracksUnsupportedMetricsWithinRenderedScores() {
    val effect =
        parse<Effect>("End: VictoryPoint / Adjacency<CityTile(HAS CapitalMarker), OceanTile>")
    val rendering =
        renderEffect(
            effect,
            Describers(Canon.classTable, TerraformingMarsDescribers.descriptions),
        )

    rendering.value shouldBe "1 VP per [Adjacency<CityTile(HAS CapitalMarker), OceanTile>]."
    rendering.unresolved.map { it.node.toString() to it.reason } shouldBe
        listOf(
            "Adjacency<CityTile(HAS CapitalMarker), OceanTile>" to RefusalReason.UNSUPPORTED_METRIC
        )
  }

  @Test
  internal fun tracksUnsupportedInstructionsWithinTriggeredEffects() {
    val effect = parse<Effect>("Trade<ColonyTile>:: TradeBarrier<ColonyTile>")
    val rendering =
        renderEffect(
            effect,
            Describers(Canon.classTable, TerraformingMarsDescribers.descriptions),
        )

    rendering.value shouldBe "When you trade, [TradeBarrier<ColonyTile>]."
    rendering.unresolved.map { it.node.toString() to it.reason } shouldBe
        listOf("TradeBarrier<ColonyTile>" to RefusalReason.UNKNOWN_CHANGE_FRAME)
  }

  @Test
  internal fun usesDefaultNounForAClassWithoutRegisteredEnglishFacts() {
    TerraformingMarsDescribers.descriptions.keys.none { it.toString() == "Heat" } shouldBe true
    val sparseEnglish = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

    sparseEnglish.describe(parse<InstructionTree>("2 Heat")) shouldBe "Gain 2 heat."
  }

  @Test
  internal fun usesTheSuppliedClassTableForExpansionComponents() {
    val declarations = parseClasses("CLASS Fanium : StandardResource").toSet()
    val additions =
        object : TfmCatalog() {
          override val explicitClassDeclarations: Set<ClassDeclaration> = declarations
        }
    val expandedCatalog = TfmCatalog.compose(Canon, additions)
    val expandedEnglish =
        English(expandedCatalog.classTable, TerraformingMarsDescribers.descriptions)

    expandedEnglish.describe(parse<InstructionTree>("2 Fanium")) shouldBe "Gain 2 fanium."
  }

  @Test
  internal fun usesSuppliedPaymentKnowledgeForExpansionResources() {
    val declarations =
        parseClasses(
                """
                CLASS Fanium : StandardResource
                CLASS FaniumConverter : ActiveCard<Class<ProjectCard>> {
                  cost = 0
                  This:: GrantedResourceValue<Class<Fanium>, This>
                  PlayTag<Class<BuildingTag>>:: Accepting<Class<Fanium>>
                }
                """
                    .trimIndent()
            )
            .toSet()
    val additions =
        object : TfmCatalog() {
          override val explicitClassDeclarations: Set<ClassDeclaration> = declarations
        }
    val expandedCatalog = TfmCatalog.compose(Canon, additions)
    val expandedEnglish =
        English(
            expandedCatalog.classTable,
            TerraformingMarsDescribers.descriptions +
                (cn("Fanium") to ComponentDescriber(basePaymentValue = true)),
        )

    expandedEnglish.topText(expandedCatalog.card(cn("FaniumConverter"))) shouldBe
        "Effect: When you play a building tag, fanium may be used. " +
            "Each fanium you pay is worth 1 M€ extra."
  }

  private fun syntheticCard(source: String): Class {
    val declarations = parseClasses(source.trimIndent()).toSet()
    val additions =
        object : TfmCatalog() {
          override val explicitClassDeclarations: Set<ClassDeclaration> = declarations
        }
    val catalog = TfmCatalog.compose(Canon, additions)
    val cardFront = catalog.classTable.getClass(cn("CardFront"))
    return declarations
        .map { catalog.classTable.getClass(it.className) }
        .single { it.isSubtypeOf(cardFront) }
  }
}
