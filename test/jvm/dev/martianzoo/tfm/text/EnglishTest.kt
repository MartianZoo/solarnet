package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.Class
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.fake.FakeCanon
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishTest {
  private val catalog = TfmCatalog.compose(Canon, FakeCanon)
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)
  private val publishedEnglish =
      English(catalog.classTable, TerraformingMarsDescribers.descriptions)
  private val cardsByClassName = Canon.cards.associateBy { it.className }
  private val published =
      EnglishCardTextData.parse(readEnglishCardText("english-published-wording-evidence.tsv"))
  private val corrected =
      EnglishCardTextData.parse(readEnglishCardText("english-corrected-wording-evidence.tsv"))
  private val goals = EnglishCardTextData.parse(readEnglishCardText("english-card-text-goals.tsv"))
  private val current =
      EnglishCardTextData.parse(readEnglishCardText("english-card-text-current.tsv"))

  // This characterization is deliberately the sole wording test for every published card
  // shape. The separate goals file is reviewed target text, not an answer source or test oracle.
  @Test
  internal fun allCardTextMatchesCurrentSnapshot() {
    goals.keys shouldBe cardsByClassName.keys
    current.keys.toList() shouldBe published.keys.toList()
    corrected.keys.toList() shouldBe published.keys.toList()
    current.forEach { (cardFront, expected) ->
      withClue(cardFront.toString()) {
        val card = catalog.classTable.getClass(cardFront)
        val rendering = publishedEnglish.renderCard(card)
        expected.englishName shouldBe published.getValue(cardFront).englishName
        expected.englishName shouldBe corrected.getValue(cardFront).englishName
        expected.englishName.contains("Fake", ignoreCase = true) shouldBe false
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
    english.describe(
        parse<Effect>("Placement<@MarsArea>: MC / Neighbor<OceanTile, @MarsArea>")
    ) shouldBe "When you place a tile on Mars, gain 1 M€ per ocean tile next to that area."
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
        "Spend 4 energy to gain 2 steel and raise oxygen 1 step."
    english.describe(listOf(parse<Action>("Animal<This, Owner> -> Steel"))) shouldBe
        "Spend 1 animal from this card to gain 1 steel."
    english.describe(listOf(parse<Action>("X Floater<This> -> X StandardResource"))) shouldBe
        "Spend 1 or more floaters from this card to gain the same number of one standard resource."
    english.describe(listOf(parse<Action>("X ProjectCard -> 2X MC"))) shouldBe
        "Discard 1 or more cards to gain twice that amount of M€."
    english.describe(listOf(parse<Action>("MC -> Animal<This>?"))) shouldBe "[MC -> Animal<This>?]."
    english.describe(parse<InstructionTree>("2 Plant, TemperatureStep")) shouldBe
        "Gain 2 plants. Raise temperature 1 step."
    english.describe(parse<InstructionTree>("3 Microbe, 2 Animal")) shouldBe
        "Add 3 microbes to any card. Add 2 animals to any card."
    english.describe(parse<InstructionTree>("PROD[-Energy, 2 MC]")) shouldBe
        "Decrease your energy production 1 step and increase your M€ production 2 steps."
    english.describe(parse<InstructionTree>("PROD[Plant, Energy]")) shouldBe
        "Increase your plant production and your energy production 1 step each."
    english.describe(parse<InstructionTree>("PROD[2 Plant, 2 Energy, 3 Heat]")) shouldBe
        "Increase your plant production 2 steps, your energy production 2 steps, and your heat production 3 steps."
    english.describe(parse<InstructionTree>("PROD[-MC, Plant, Energy, Heat]")) shouldBe
        "Decrease your M€ production 1 step and increase your plant production, your energy production, and your heat production 1 step each."
    english.describe(parse<InstructionTree>("MC? / ProjectCard")) shouldBe
        "You may gain up to 1 M€ per card in hand."
    english.describe(parse<InstructionTree>("-4 MC.")) shouldBe
        "Remove 4 M€, or as much as possible."
    english.describe(parse<InstructionTree>("Plant / (VenusTag OR PlantTag OR Colony)")) shouldBe
        "Gain 1 plant per Venus tag you have, plant tag you have, or colony you own."
    english.describe(parse<InstructionTree>("PROD[1 MC / EarthTag MAX VenusTag]")) shouldBe
        "Increase your M€ production 1 step per pair of Earth and Venus tags you have."
    english.describe(parse<InstructionTree>("-3 MC THEN TemperatureStep")) shouldBe
        "Pay 3 M€ to raise temperature 1 step."
    english.describe(parse<InstructionTree>("-2 Plant")) shouldBe "Remove 2 plants."
    english.describe(parse<InstructionTree>("Animal<Owner, This>?")) shouldBe
        "You may add up to 1 animal to this card."
    english.describe(
        parse<InstructionTree>("3 MC<Anyone> FROM MC."),
    ) shouldBe "Pay 3 M€ to any player, or as much as possible."
    english.describe(parse<Requirement>("MAX 6 OxygenStep")) shouldBe "Requires 6% oxygen or less."
    english.describe(
        parse<Effect>("ActionBilling<ConvertPlantsAction, Action1>:: -Owed<Class<Plant>>")
    ) shouldBe "When you convert plants to greenery, you pay 1 plant less."
    english.describe(parse<Effect>("Billing<CardPlay>:: -2 Owed<>")) shouldBe
        "When you play a card, you pay 2 M€ less for it."
    english.describe(
        parse<Effect>("CardBilling<Class<CardFront>(HAS requirement)>:: -2 Owed<>")
    ) shouldBe "When you play a card with a requirement, you pay 2 M€ less for it."
    english.describe(parse<Effect>("PayingFor<Owner, Class<ProjectCard>>:: 2 Owed<>")) shouldBe
        "When you buy a card, pay 2 M€ extra."
    english.describe(
        parse<Effect>("UseAction<This, Action1>:: Accepting<Class<Titanium>>")
    ) shouldBe "When you pay for this action, titanium may be used."
    english.describe(parse<Effect>("PayingFor<Owner, Class<PlanetaryTag>>:: -2 Owed<>")) shouldBe
        "When you play a planetary tag, you pay 2 M€ less for it."
    english.describe(parse<Effect>("PayingFor<Owner, Class<EarthTag>>:: -2 Owed<>")) shouldBe
        "When you play an Earth tag, you pay 2 M€ less for it."
    english.describe(parse<InstructionTree>("ProjectCard")) shouldBe "Draw 1 card."
    english.describe(parse<InstructionTree>("OceanTile")) shouldBe "Place an ocean tile."
    english.describe(parse<InstructionTree>("CityTile")) shouldBe "Place a city tile."
    english.describe(parse<InstructionTree>("Community<LandArea(HAS MAX 0 Occupant)>")) shouldBe
        "Place a community marker on a land area with no occupant."
    english.describe(
        parse<InstructionTree>(
            "Community<LandArea(HAS MAX 0 Occupant, HAS Neighbor<OwnedOccupant>)>"
        )
    ) shouldBe
        "Place a community marker on a land area with no occupant next to a tile or community you own."
    english.describe(parse<InstructionTree>("EACH Player { ProjectCard }")) shouldBe
        "Have each player draw 1 card."
    english.describe(
        parse<InstructionTree>("EACH Player(HAS StartToken) { ChooseOceanArea }")
    ) shouldBe "[EACH Player(HAS StartToken) { ChooseOceanArea }]."
    english.describe(
        parse<InstructionTree>("EACH Player(NOT Owner) { PROD[-2 MC] BY Owner }")
    ) shouldBe "Each other player decreases their own M€ production 2 steps."
    english.describe(parse<InstructionTree>("WorldGovernmentTerraforming")) shouldBe
        "Raise 1 global parameter without gaining terraform rating or other bonuses."
    english.describe(
        parse<InstructionTree>("EACH Player(HAS MAX 0 This<Anyone>) { -5 MC., PROD[-1 MC] }")
    ) shouldBe "Remove 5 M€ from each opponent and decrease their M€ production 1 step."
    english.describe(parse<Requirement>("ScienceTag")) shouldBe "Requires a science tag."
    english.describe(parse<Requirement>("4 BioTag")) shouldBe "Requires 4 bio tags."
    english.describe(parse<Requirement>("2 EarthTag, 2 VenusTag, 2 JovianTag")) shouldBe
        "Requires 2 Earth tags, 2 Venus tags, and 2 Jovian tags."
    english.describe(parse<Requirement>("8 Class<Tag>(HAS Tag<Owner>)")) shouldBe
        "Requires 8 different tags."
    english.describe(parse<Requirement>("3 CityTile")) shouldBe
        "Requires that you have 3 city tiles."
    english.describe(parse<Requirement>("2 CityTile<Anyone>")) shouldBe
        "Requires 2 city tiles in play."
    english.describe(parse<Requirement>("5 Floater")) shouldBe "Requires that you have 5 floaters."
    english.describe(parse<Requirement>("PROD[Titanium]")) shouldBe
        "Requires that you have titanium production."
    english.describe(parse<Requirement>("25 TerraformRating")) shouldBe
        "Requires that you have 25 terraform rating."
    english.describe(parse<Requirement>("CityTile, Colony")) shouldBe
        "Requires that you have a city tile and a colony."
    english.describe(parse<Requirement>("Adjacency<CityTile, OceanTile>")) shouldBe
        "Requires that you have a city tile next to an ocean tile."
    english.describe(parse<Requirement>("VenusTag, EarthTag, JovianTag")) shouldBe
        "Requires a Venus tag, an Earth tag, and a Jovian tag."
    english.describe(parse<Requirement>("VenusTag, PlantTag")) shouldBe
        "Requires a Venus tag and a plant tag."
    english.describe(parse<Effect>("End: VictoryPoint / Cathedral")) shouldBe
        "1 VP per cathedral in play."
    english.describe(parse<Effect>("-Community: 3 MC")) shouldBe
        "When you remove a community marker, gain 3 M€."
    english.describe(parse<InstructionTree>("Animal")) shouldBe "Add 1 animal to any card."
    english.describe(parse<InstructionTree>("MAX 0 Plant: Steel")) shouldBe
        "If you have no plants, gain 1 steel."
    english.describe(parse<InstructionTree>("OceanTile: Steel")) shouldBe
        "If there is 1 ocean tile, gain 1 steel."
    english.describe(parse<InstructionTree>("2 OceanTile: Steel")) shouldBe
        "If there are 2 ocean tiles, gain 1 steel."
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
    english.describe(
        parse<InstructionTree>("UseAction<ActionCard(HAS ActionUsedMarker, NOT Viron)>")
    ) shouldBe "Use an action from an action card that has an action-used marker and is not Viron."
  }

  @Test
  internal fun hidesPreludeBookkeepingBehindItsPrintedRule() {
    english.bottomText(requireNotNull(cardsByClassName[cn("IndustrialComplex")])) shouldBe
        "Remove 18 M€. Increase each of your productions below 1 to 1."
    english.topText(requireNotNull(cardsByClassName[cn("SuitableInfrastructure")])) shouldBe
        "Effect: Once per action you take, gain 2 M€ if you increase any production."
    english.bottomText(requireNotNull(cardsByClassName[cn("SuitableInfrastructure")])) shouldBe
        "Gain 5 steel."
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
            CLASS ConditionalImmediate : AutomatedCard {
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
            CLASS FlexibleNextCard : AutomatedCard {
              cost = 0
              This: NextCardEffect { CheckRequirement:: -2 Required<Class<GlobalParameter>>. }
            }
            """
        )
    val discount =
        syntheticCard(
            """
            CLASS DiscountNextCard : AutomatedCard {
              cost = 0
              This: NextCardEffect { Billing<CardPlay>:: -8 Owed<> }
            }
            """
        )

    English(flexibility.classTable, TerraformingMarsDescribers.descriptions)
        .bottomText(flexibility) shouldBe
        "You may treat the global parameter requirement of the next card you play this generation as if it is 2 steps lower or higher."
    English(discount.classTable, TerraformingMarsDescribers.descriptions)
        .bottomText(discount) shouldBe
        "When you play the next card this generation, you pay 8 M€ less for it."
  }

  @Test
  internal fun realizesLaterTypeVariableUsesAsAntecedents() {
    english.describe(parse<Effect>("PROD[@StandardResource]: @StandardResource")) shouldBe
        "When you increase one of your productions 1 step, gain that resource."
    english.describe(
        listOf(parse<Action>("PROD[@StandardResource] -> 4 @StandardResource"))
    ) shouldBe "Decrease one of your productions 1 step to gain 4 of that resource."
    english.describe(listOf(parse<Action>("PROD[StandardResource] -> 4 MC"))) shouldBe
        "Decrease one of your productions 1 step to gain 4 M€."
    english.describe(parse<InstructionTree>("@StandardResource THEN @StandardResource")) shouldBe
        "Gain 1 standard resource, then gain that resource."
    english.describe(parse<Effect>("Trade<@ColonyTile>: ColonyProduction<@ColonyTile>?")) shouldBe
        "When you trade, you may raise that colony tile track 1 step."

    val unintroduced =
        syntheticCard(
            """
            CLASS Unintroduced<StandardResource> : ActiveCard {
              cost = 0
              PayingFor<Owner, Class<ProjectCard>>: StandardResource
            }
            """
        )
    english.topText(unintroduced) shouldBe "Effect: When you buy a card, gain 1 standard resource."
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
            CLASS TitaniumAction : ActionCard, ActiveCard, ResourceCard<Class<Asteroid>> {
              cost = 0
              UseAction<This, Action1>:: Accepting<Class<Titanium>>
              12 MC -> OceanTile
              Asteroid<This> -> VenusStep
            }
            """
        )

    english.topText(card) shouldBe
        "Action: Spend 12 M€ (titanium may be used) to place an ocean tile, or spend 1 asteroid from this card to raise Venus 1 step."
  }

  @Test
  internal fun cardWithoutTopElementsHasEmptyTopText() {
    val requirementOnly =
        syntheticCard(
            "CLASS RequirementOnly : AutomatedCard { cost = 0; requirement = HAS \"OxygenStep\" }"
        )

    english.topText(requirementOnly) shouldBe ""
  }

  @Test
  internal fun interpretsPlayerOwnedTypesInCardOwnershipContext() {
    english.describe(parse<InstructionTree>("2 MC / Colony")) shouldBe
        "Gain 2 M€ per colony you own."
    english.describe(parse<InstructionTree>("2 MC / Colony<Anyone>")) shouldBe
        "Gain 2 M€ per colony in play."
    english.describe(parse<InstructionTree>("2 MC / CityTile<Anyone>")) shouldBe
        "Gain 2 M€ per city tile in play."
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
        syntheticCard("CLASS ActionOnly : ActionCard, ActiveCard { cost = 0; -> ProjectCard }")

    english.bottomText(actionOnly) shouldBe ""
  }

  @Test
  internal fun omitsUnconditionalFixedScoresFromCardText() {
    val fixedScore =
        syntheticCard("CLASS FixedScore : AutomatedCard { cost = 0; End: 2 VictoryPoint }")
    val metricScore =
        syntheticCard(
            "CLASS MetricScore : AutomatedCard { cost = 0; End: VictoryPoint / Colony<Anyone> }"
        )

    english.bottomText(fixedScore) shouldBe ""
    english.bottomText(metricScore) shouldBe "1 VP per colony in play."
  }

  @Test
  internal fun callsOutTheEnteringCardOnlyWhereItParticipatesImmediately() {
    val card =
        syntheticCard(
            """
            CLASS SelfCounting : ActiveCard {
              cost = 0
              This:: JovianTag<This>
              This: TerraformRating / JovianTag
              -> MC / JovianTag
              JovianTag: 2 MC
              End: VictoryPoint / JovianTag
            }
            """
        )

    english.bottomText(card) shouldBe
        "Raise your terraform rating 1 step per Jovian tag you have (including this). " +
            "1 VP per Jovian tag you have."
    english.topText(card) shouldBe
        "Action: Gain 1 M€ per Jovian tag you have. / " +
            "Effect: When you play a Jovian tag (including this), gain 2 M€."

    val noTagCard =
        syntheticCard(
            """
            CLASS SelfFiltering : AutomatedCard {
              cost = 0
              This: PROD[MC / CardFront(HAS MAX 0 Tag)]
            }
            """
        )
    english.bottomText(noTagCard) shouldBe
        "Increase your M€ production 1 step per card with no tags (including this)."

    val outsideCountedClass =
        syntheticCard(
            """
            CLASS OutsideCountedClass : AutomatedCard {
              cost = 0
              This:: ScienceTag<This>
            }
            """
        )
    Describers(Canon.classTable, TerraformingMarsDescribers.descriptions)
        .forCard(outsideCountedClass, null)
        .whileEnteringCard()
        .enteringCardCounts(parse<Expression>("ActiveCard(HAS ScienceTag)")) shouldBe false
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
    rendering.unresolved().map { it.node.toString() to it.reason } shouldBe
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

    rendering.linearize() shouldBe "Pay 2 steel to [3 VictoryPoint]."
    rendering.unresolved().map { it.node.toString() to it.reason } shouldBe
        listOf("3 VictoryPoint" to RefusalReason.UNKNOWN_CHANGE_FRAME)
  }

  @Test
  internal fun neverDropsUnsupportedExpressionRefinements() {
    val metric = "CityTile<Anyone, MarsArea(HAS Neighbor<OceanTile>, NOT NoctisArea)>"

    english.describe(parse<InstructionTree>("MC / $metric")) shouldBe "[MC / $metric]."
  }

  @Test
  internal fun tracksAStandaloneUnsupportedTriggeredInstruction() {
    val effect = parse<Effect>("Trade<ColonyTile>:: TradeBarrier<ColonyTile>")
    val rendering =
        renderEffect(
            effect,
            Describers(Canon.classTable, TerraformingMarsDescribers.descriptions),
        )

    rendering.linearize() shouldBe "When you trade, [TradeBarrier<ColonyTile>]."
    rendering.unresolved().map { it.node.toString() to it.reason } shouldBe
        listOf("TradeBarrier<ColonyTile>" to RefusalReason.UNKNOWN_CHANGE_FRAME)
  }

  @Test
  internal fun refusesACompoundTriggeredInstructionWithUnsupportedPartsAsAUnit() {
    val effect =
        parse<Effect>(
            "Trade<ColonyTile>: ColonyProduction<ColonyTile>? THEN -TradeBarrier<ColonyTile>"
        )
    val rendering =
        renderEffect(
            effect,
            Describers(Canon.classTable, TerraformingMarsDescribers.descriptions),
        )

    rendering.linearize() shouldBe
        "[Trade<ColonyTile>: ColonyProduction<ColonyTile>? THEN -TradeBarrier<ColonyTile>]."
    rendering.unresolved().map { it.node.toString() to it.reason } shouldBe
        listOf(effect.toString() to RefusalReason.UNSUPPORTED_EFFECT_TRIGGER)
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
                CLASS FaniumConverter : ActiveCard {
                  cost = 0
                  This:: GrantedResourceValue<Class<Fanium>, This>
                  PayingFor<Owner, Class<BuildingTag>>:: Accepting<Class<Fanium>>
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
