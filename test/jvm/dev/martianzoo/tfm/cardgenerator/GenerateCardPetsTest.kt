package dev.martianzoo.tfm.cardgenerator

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.carddata.CardData
import dev.martianzoo.tfm.carddata.CardDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GenerateCardPetsTest {
  @Test
  internal fun exactAttachmentsAndRepeatedTagsShareCreationWithoutDuplicates() {
    val data =
        CardDefinition(
            name = "Example",
            deck = "ProjectCard",
            tags = listOf("MicrobeTag", "MicrobeTag", "BuildingTag"),
            invariants =
                linkedSetOf(
                    "=3 Grant<Class<Steel>, This>",
                    "=1 Grant<Class<Titanium>, This>",
                    "=2 MicrobeTag<This>",
                ),
            effects = listOf("This:: 5 MC"),
            immediate = "2 Plant",
        )
    val generated = CardPetsGenerator.GeneratedCard(data)
    val rendered = generated.render()
    val card = parseClasses(rendered).single()
    assertEquals(generated.declaration, card)
    assertEquals(1, rendered.lineSequence().count { it.trimStart().startsWith("HAS ") })

    assertEquals(
        (data.invariants + "=1 BuildingTag<This>").map { parse<Requirement>(it) }.toSet(),
        Requirement.split(card.invariants).toSet(),
    )
    assertEquals(
        listOf(
                "This:: 5 MC",
                "This:: 3 Grant<Class<Steel>, This>!, Grant<Class<Titanium>, This>!",
                "This:: 2 MicrobeTag<This>!, BuildingTag<This>!",
                "This: 2 Plant",
            )
            .map { parse<Effect>(it) },
        card.authoredEffects,
    )
  }

  @Test
  internal fun otherConstraintsDoNotCreateComponents() {
    val data =
        CardDefinition(
            name = "Example",
            invariants =
                linkedSetOf(
                    "MAX 1 Tile<This>",
                    "1 Marker<This>",
                    "=0 Forbidden<This>",
                    "=1 Phase",
                    "=1 This",
                    "=1 Class<This>",
                    "=1 Attached<Bridge<This>>",
                    "=1 Refined<This>(NOT Excluded)",
                ),
        )
    val card = parseClasses(CardPetsGenerator.GeneratedCard(data).render()).single()

    assertEquals(
        data.invariants.map { parse<Requirement>(it) }.toSet(),
        Requirement.split(card.invariants).toSet(),
    )
    assertTrue(card.authoredEffects.isEmpty())
  }

  @Test
  internal fun permanentAttachmentKeepsAProjectBlueWithoutHandwrittenEffects() {
    val data =
        CardDefinition(
            name = "Example",
            deck = "ProjectCard",
            invariants = setOf("=1 Grant<This>"),
        )

    assertEquals("ActiveCard", data.projectKind)
  }

  @Test
  internal fun generatedTagInvariantsDoNotMakeAnOrdinaryProjectBlue() {
    val data =
        CardDefinition(
            name = "Example",
            deck = "ProjectCard",
            tags = listOf("ScienceTag"),
            immediate = "ProjectCard",
        )
    val card = parseClasses(CardPetsGenerator.GeneratedCard(data).render()).single()

    assertEquals("AutomatedCard", data.projectKind)
    assertEquals(setOf(parse<Requirement>("=1 ScienceTag<This>")), card.invariants)
    assertEquals(parse<Effect>("This:: ScienceTag<This>!"), card.authoredEffects.first())
  }

  @Test
  internal fun groupedDecksPreserveCardDefinitionsAndDeriveProjectKinds() {
    val cards = CardData.definitions("TerraformingMars").associateBy { it.name }

    assertEquals("StandardCorporationCard", cards.getValue("CrediCor").deck)
    assertEquals(null, cards.getValue("CrediCor").projectKind)
    assertEquals("AutomatedCard", cards.getValue("DeepWellHeating").projectKind)
    assertEquals("ActiveCard", cards.getValue("ArcticAlgae").projectKind)
    assertEquals("EventCard", cards.getValue("ImportedHydrogen").projectKind)
    assertEquals("EventTag", cards.getValue("ImportedHydrogen").tags.last())
  }

  @Test
  internal fun cardEffectsHaveStableSetupOrdering() {
    val colonies = CardPetsGenerator.renderBundle("ColoniesExpansion")
    assertInOrder(
        colonies.cardDeclaration("StormcraftIncorporated"),
        "This:: JovianTag<This>",
        "This: 48 MC",
        "Billing<Class<Heat>>:: AcceptingFromCard<This>",
        "PayFromCard<This>:: -2 Owed<Class<Heat>>",
    )

    val promos = CardPetsGenerator.renderBundle("PromoCardPack")
    assertInOrder(
        promos.cardDeclaration("PharmacyUnion"),
        "This:: 54 MC",
        "This:: 2 MicrobeTag<This>",
        "This: SearchForCard<TagFilter<Class<ScienceTag>>>",
        "MicrobeTag<Anyone>:",
    )
  }

  @Test
  internal fun supportingDeclarationsFollowTheirCard() {
    val colonies = CardPetsGenerator.renderBundle("ColoniesExpansion")
    assertInOrder(
        colonies,
        "CLASS Aridor :",
        "CLASS AridorTagWatcher<Class<@Tag>> :",
        "CLASS Arklight :",
    )
  }

  @Test
  internal fun authoredSpecialCardsFollowGeneratedCardsInTheSameResource() {
    val cards = renderCardPets("TurmoilExpansion", "CLASS ExampleGlobalEvent : GlobalEvent\n")

    assertInOrder(cards, "CLASS AerialLenses :", "CLASS ExampleGlobalEvent : GlobalEvent")
    assertTrue(cards.endsWith('\n'))
  }

  private fun String.cardDeclaration(name: String): String =
      substringAfter("CLASS $name ").substringBefore("\n}")

  private fun assertInOrder(source: String, vararg fragments: String) {
    var previous = -1
    fragments.forEach { fragment ->
      val next = source.indexOf(fragment)
      assertTrue(next > previous, "Expected '$fragment' after index $previous in:\n$source")
      previous = next
    }
  }
}
