package dev.martianzoo.tfm.cardgenerator

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.EffectTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.carddata.CardData
import dev.martianzoo.tfm.carddata.CardDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class GenerateCardPetsTest {
  @Test
  internal fun eventClassificationDoesNotDependOnTagOrder() {
    val data =
        CardDefinition(
            name = "EventFirst",
            deck = "ProjectCard",
            tags = listOf("EventTag", "SpaceTag"),
            immediate = "Plant",
        )
    val card = CardPetsGenerator.GeneratedCard(data).declaration
    assertEquals("EventCard", data.projectKind)
    assertEquals(listOf("EventCard"), card.supertypes.map { it.className.toString() })
    assertEquals(
        setOf(parse<Requirement>("=1 EventTag<This>"), parse<Requirement>("=1 SpaceTag<This>")),
        Requirement.split(card.invariants).toSet(),
    )
  }

  @Test
  internal fun duplicateEventTagsAreStillRejected() {
    assertFailsWith<IllegalArgumentException> {
      CardDefinition(
          name = "RepeatedEvent",
          deck = "ProjectCard",
          tags = listOf("EventTag", "EventTag"),
      )
    }
  }

  @Test
  internal fun ambiguousResourceStorageFailsInsteadOfSilentlyDroppingTheHolderRole() {
    val invalid =
        CardDefinition(
            name = "AmbiguousHolder",
            deck = "ProjectCard",
            actions = listOf("-> Animal<This>, Microbe<This>"),
            effects = listOf("End: VictoryPoint / Animal<This>, VictoryPoint / Microbe<This>"),
        )
    val failure =
        assertFailsWith<IllegalArgumentException> {
          CardPetsGenerator.GeneratedCard(invalid).render()
        }
    assertTrue(failure.message.orEmpty().contains("AmbiguousHolder has ambiguous resource storage"))
    assertTrue(failure.message.orEmpty().contains("Animal"))
    assertTrue(failure.message.orEmpty().contains("Microbe"))
  }

  @Test
  internal fun exactAttachmentsAndRepeatedTagsProduceOnlyInvariants() {
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
                "This: 2 Plant",
            )
            .map { parse<EffectTree>("OWN[$it]") },
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
    assertEquals(listOf(parse<EffectTree>("OWN[This: ProjectCard]")), card.authoredEffects)
  }

  @Test
  internal fun groupedDecksPreserveCardDefinitionsAndDeriveProjectKinds() {
    val cards = CardData.definitions("TerraformingMars").associateBy { it.name }

    assertEquals("CorporationCard", cards.getValue("CrediCor").deck)
    assertEquals(null, cards.getValue("CrediCor").projectKind)
    assertEquals("AutomatedCard", cards.getValue("DeepWellHeating").projectKind)
    assertEquals("ActiveCard", cards.getValue("ArcticAlgae").projectKind)
    assertEquals("EventCard", cards.getValue("ImportedHydrogen").projectKind)
    assertTrue("EventTag" in cards.getValue("ImportedHydrogen").tags)
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

  private fun assertInOrder(source: String, vararg fragments: String) {
    var previous = -1
    fragments.forEach { fragment ->
      val next = source.indexOf(fragment)
      assertTrue(next > previous, "Expected '$fragment' after index $previous in:\n$source")
      previous = next
    }
  }
}
