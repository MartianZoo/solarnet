package dev.martianzoo.tfm.language

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Vocabulary.Companion.defaultEnglishDisplayName
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishTest {
  private val english = English(TerraformingMarsDescribers.descriptions)
  private val cardsByClassName = Canon.cardDefinitions.associateBy { it.className }
  private val goals = EnglishCardTextData.parse(readEnglishCardText("english-card-text-goals.tsv"))
  private val current =
      EnglishCardTextData.parse(readEnglishCardText("english-card-text-current.tsv"))

  // This characterization is deliberately the sole wording test for every canonical derivation
  // shape. The separate goals file is reviewed target text, not an answer source or test oracle.
  @Test
  fun allCardTextMatchesCurrentSnapshot() {
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
  fun describesStandalonePetsElements() {
    english.describe(parse<Effect>("End: VictoryPoint / Animal<This>")) shouldBe
        "1 VP per animal on this card."
    english.describe(parse<Effect>("End: VictoryPoint / Animal<This, Owner>")) shouldBe
        "1 VP per animal on this card."
    english.describe(parse<Effect>("CityTile<MarsArea, Anyone>: Steel")) shouldBe
        "When any city tile is placed on Mars, gain 1 steel."
    english.describe(parse<Effect>("PlantTag<CardFront<Anyone>, Anyone>: Steel")) shouldBe
        "When any plant tag is played, gain 1 steel."
    english.describe(listOf(parse<Action>("4 Energy -> 2 Steel, OxygenStep"))) shouldBe
        "Spend 4 energy to gain 2 steel and raise oxygen 1 step."
    english.describe(listOf(parse<Action>("Animal<This, Owner> -> Steel"))) shouldBe
        "Remove 1 animal from this card to gain 1 steel."
    english.describe(parse<InstructionTree>("2 Plant, TemperatureStep")) shouldBe
        "Gain 2 plants. Raise temperature 1 step."
    english.describe(parse<InstructionTree>("Animal<Owner, This>?")) shouldBe
        "Add up to 1 animal to this card."
    english.describe(
        parse<InstructionTree>("3 Megacredit<Anyone> FROM Megacredit."),
    ) shouldBe "Pay 3 M€ to any player, or as much as possible."
    english.describe(parse<Requirement>("MAX 6 OxygenStep")) shouldBe
        "Requires that oxygen is 6% or lower."
    english.describe(
        parse<Effect>("Invoice<ConvertPlantsSA, First>:: -Owed<Class<Plant>>")
    ) shouldBe "When you convert plants to greenery, pay 1 plant less."
    english.describe(parse<Effect>("Billing<PlayCards>:: -2 Owed<>")) shouldBe
        "When you play a card, pay 2 M€ less."
    english.describe(
        parse<Effect>("CardInvoice<Class<CardFront>(HAS requirement)>:: -2 Owed<>")
    ) shouldBe "When you play a card with a requirement, pay 2 M€ less."
    english.describe(parse<Effect>("BuyCard:: 2 Owed<>")) shouldBe
        "When you buy a card, pay 2 M€ extra."
    english.describe(parse<Effect>("UseAction<This, First>:: Accept<Class<Titanium>>")) shouldBe
        "When you pay for this action, titanium may be used."
    english.describe(parse<Effect>("PlayTag<Class<PlanetTag>>:: -2 Owed<>")) shouldBe
        "When you play a planet tag, pay 2 M€ less."
    english.describe(parse<Effect>("End: VictoryPoint / Cathedral<Anyone>")) shouldBe
        "1 VP per ANY cathedral."

    val animalCard =
        CardDefinition(
            CardData(
                name = "AnimalHolder",
                deck = "PROJECT",
                projectKind = "ACTIVE",
                resourceType = "Animal",
            )
        )
    english.describe(parse<InstructionTree>("Animal"), animalCard) shouldBe
        "Add 1 animal to any card."
  }

  @Test
  fun cardWithoutTopElementsHasEmptyTopText() {
    val requirementOnly =
        CardDefinition(
            CardData(
                name = "RequirementOnly",
                deck = "PROJECT",
                projectKind = "AUTOMATED",
                requirement = "OxygenStep",
            )
        )

    english.topText(requirementOnly) shouldBe ""
  }

  @Test
  fun interpretsPlayerOwnedTypesInCardOwnershipContext() {
    english.describe(parse<InstructionTree>("2 Megacredit / Colony")) shouldBe
        "Gain 2 M€ for each colony you own."
    english.describe(parse<InstructionTree>("2 Megacredit / Colony<Anyone>")) shouldBe
        "Gain 2 M€ for each colony."
  }

  @Test
  fun cardWithoutBottomElementsHasEmptyBottomText() {
    val actionOnly =
        CardDefinition(
            CardData(
                name = "ActionOnly",
                deck = "PROJECT",
                projectKind = "ACTIVE",
                actions = listOf("-> ProjectCard"),
            )
        )

    english.bottomText(actionOnly) shouldBe ""
  }

  @Test
  fun retainsUnsupportedPetsAlongsideRenderedInstructions() {
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
  fun usesDefaultNounForAClassWithoutRegisteredEnglishFacts() {
    val heat =
        TerraformingMarsDescribers.descriptions.keys.single { it.className.toString() == "Heat" }
    val sparseEnglish = English(TerraformingMarsDescribers.descriptions - heat)

    sparseEnglish.describe(parse<InstructionTree>("2 Heat")) shouldBe "Gain 2 heat."
  }
}
