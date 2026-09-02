package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Vocabulary.Companion.defaultEnglishDisplayName
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.Class
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

internal class EnglishTest {
  private val english = English(TerraformingMarsDescribers.descriptions)
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
        expected.englishName shouldBe
            (goals[cardFront]?.englishName ?: defaultEnglishDisplayName(cardFront))
        english.topText(card) shouldBe expected.top
        english.bottomText(card) shouldBe expected.bottom
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
    english.describe(listOf(parse<Action>("4 Energy -> 2 Steel, OxygenStep"))) shouldBe
        "Pay 4 energy to gain 2 steel and raise oxygen 1 step."
    english.describe(listOf(parse<Action>("Animal<This, Owner> -> Steel"))) shouldBe
        "Remove 1 animal from this card to gain 1 steel."
    english.describe(listOf(parse<Action>("MC -> Animal<This>?"))) shouldBe "[MC -> Animal<This>?]."
    english.describe(parse<InstructionTree>("2 Plant, TemperatureStep")) shouldBe
        "Gain 2 plants. Raise temperature 1 step."
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
    english.describe(parse<Effect>("UseAction<This, Action1>:: Accept<Class<Titanium>>")) shouldBe
        "When you pay for this action, titanium may be used."
    english.describe(parse<Effect>("PlayTag<Class<PlanetaryTag>>:: -2 Owed<>")) shouldBe
        "When you play a planetary tag, pay 2 M€ less."
    english.describe(parse<Effect>("PlayTag<Class<EarthTag>>:: -2 Owed<>")) shouldBe
        "When you play an Earth tag, pay 2 M€ less."
    english.describe(parse<InstructionTree>("ProjectCard")) shouldBe "Draw 1 card."
    english.describe(parse<InstructionTree>("OceanTile")) shouldBe "Place 1 ocean tile."
    english.describe(parse<InstructionTree>("CityTile")) shouldBe "Place a city tile."
    english.describe(parse<Requirement>("ScienceTag")) shouldBe "Requires a science tag."
    english.describe(parse<Requirement>("VenusTag, EarthTag, JovianTag")) shouldBe
        "Requires a Venus tag, an Earth tag, and a Jovian tag."
    english.describe(parse<Effect>("End: VictoryPoint / Cathedral<Anyone>")) shouldBe
        "1 VP per ANY cathedral."

    english.describe(parse<InstructionTree>("Animal")) shouldBe "Add 1 animal to any card."
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
              UseAction<This, Action1>:: Accept<Class<Titanium>>
              12 MC -> OceanTile
              Asteroid<This> -> VenusStep
            }
            """
        )

    english.topText(card) shouldBe
        "Action: Pay 12 M€ (titanium may be used) to place 1 ocean tile, or remove 1 asteroid from this card to raise Venus 1 step."
  }

  @Test
  internal fun refusesTrackChoiceWhoseTypedBarrierSelectsAnotherTrack() {
    val card =
        syntheticCard(
            """
            CLASS MismatchedTradeBarrier : ActiveCard<Class<ProjectCard>> {
              cost = 0
              Trade<ColonyTile>:: TradeBarrier<Luna>
              Trade<ColonyTile>: ColonyProduction<ColonyTile>? THEN -TradeBarrier<Luna>
            }
            """
        )

    english.topText(card) shouldNotBe
        "Effect: When you trade, you may first increase that colony tile track 1 step."
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
        "Gain 2 M€ per colony."
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
  internal fun retainsUnsupportedPetsAlongsideRenderedInstructions() {
    english.describe(parse<InstructionTree>("2 Steel, 3 VictoryPoint")) shouldBe
        "Gain 2 steel. [3 VictoryPoint]."

    val rendering =
        renderInstructionTree(
            parse("2 Steel, 3 VictoryPoint"),
            Describers(TerraformingMarsDescribers.descriptions),
        )
    rendering.unresolved.map { it.node.toString() to it.reason } shouldBe
        listOf("3 VictoryPoint" to RefusalReason.UNKNOWN_CHANGE_FRAME)
  }

  @Test
  internal fun usesDefaultNounForAClassWithoutRegisteredEnglishFacts() {
    val heat =
        TerraformingMarsDescribers.descriptions.keys.single { it.className.toString() == "Heat" }
    val sparseEnglish = English(TerraformingMarsDescribers.descriptions - heat)

    sparseEnglish.describe(parse<InstructionTree>("2 Heat")) shouldBe "Gain 2 heat."
  }

  private fun syntheticCard(source: String): Class {
    val declarations = parseClasses(source.trimIndent()).toSet()
    val additions =
        object : TfmCatalog() {
          override val explicitClassDeclarations: Set<ClassDeclaration> = declarations
        }
    val catalog = TfmCatalog.compose(Canon, additions)
    return catalog.card(declarations.single().className)
  }
}
