package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.state.GameConfig
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class CardTrackingCriteriaTest : CardTrackingFullGameTest() {
  override val config =
      GameConfig(
          "PreludeExpansion, VenusNextExpansion, Prelude2CardPack, PromoCardPack, TurmoilExpansion",
          "Player1",
      )
  internal override val producesReplayRecording = false

  @Test
  internal fun filteredDrawAcceptsThePrintedTag() {
    search("TagFilter<Class<PlantTag>>", AdaptedLichen)
    assertCardTrackingComplete()
  }

  @Test
  internal fun filteredDrawRejectsTheWrongTag() {
    search("TagFilter<Class<PlantTag>>", AcquiredCompany)
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "AcquiredCompany does not satisfy"
  }

  @Test
  internal fun filteredDrawChecksEveryCardInTheBatch() {
    search("TagFilter<Class<PlantTag>>", AdaptedLichen, AcquiredCompany)
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "AcquiredCompany does not satisfy"
  }

  @Test
  internal fun filteredDrawRequiresAKnownCardDefinition() {
    search("TagFilter<Class<PlantTag>>", *unknownProjectCards(1))
    shouldThrow<IllegalArgumentException> { assertCardTrackingComplete() }.message shouldContain
        "No card named UnknownCard"
  }

  @Test
  internal fun taglessDrawAcceptsACardWithNoPrintedTags() {
    search("NoTagsFilter", MicroMills)
    assertCardTrackingComplete()
  }

  @Test
  internal fun eventIconIsAPrintedTag() {
    search("NoTagsFilter", MiningExpedition)
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "MiningExpedition does not satisfy"
  }

  @Test
  internal fun referenceDrawAcceptsAFloaterAction() {
    search("ReferenceFilter<Class<Floater>>", Dirigibles)
    assertCardTrackingComplete()
  }

  @Test
  internal fun referenceDrawRejectsAnUnrelatedCard() {
    search("ReferenceFilter<Class<Floater>>", VenusGovernor)
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "VenusGovernor does not satisfy"
  }

  @Test
  internal fun referenceDrawReadsThePrintedRequirement() {
    search("ReferenceFilter<Class<PartyRequirement>>", SupportedResearch)
    assertCardTrackingComplete()
  }

  @Test
  internal fun venusSelectionChecksTheFreeCardButAllowsAnOrdinaryPurchase() {
    p1.runOperation("2 ProjectCard<Selecting>, 3 MC")
    p1.runOperation("TakeSelectedCard<TagFilter<Class<VenusTag>>>") { p1.draw(VenusGovernor) }
    p1.runOperation("BuySelectedCards") {
      p1.pay(3)
      p1.draw(AcquiredCompany)
    }
    assertCardTrackingComplete()
  }

  @Test
  internal fun venusSelectionRejectsAFreeCardWithoutTheTag() {
    p1.runOperation("ProjectCard<Selecting>")
    p1.runOperation("TakeSelectedCard<TagFilter<Class<VenusTag>>>") { p1.draw(AcquiredCompany) }
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "AcquiredCompany does not satisfy"
  }

  @Test
  internal fun successfulSearchForLifeRequiresTheFlippedIdentity() {
    flip(SearchForLife, searchForLifeClaim)
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "requires the flipped card's identity"
  }

  @Test
  internal fun successfulSearchForLifeAcceptsAMicrobeCard() {
    nameFlippedCard(flip(SearchForLife, searchForLifeClaim), Tardigrades)
    assertCardTrackingComplete()
  }

  @Test
  internal fun successfulSearchForLifeRejectsANonMicrobeCard() {
    nameFlippedCard(flip(SearchForLife, searchForLifeClaim), AdaptedLichen)
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "AdaptedLichen does not satisfy"
  }

  @Test
  internal fun failedSearchForLifeMayLeaveTheCardUnknown() {
    flip(SearchForLife)
    assertCardTrackingComplete()
  }

  @Test
  internal fun namedFailedSearchForLifeChecksTheAbsenceOfTheTag() {
    nameFlippedCard(flip(SearchForLife), AdaptedLichen)
    assertCardTrackingComplete()
  }

  @Test
  internal fun namedFailedSearchForLifeRejectsAMatchingCard() {
    nameFlippedCard(flip(SearchForLife), Tardigrades)
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "Tardigrades satisfies"
  }

  @Test
  internal fun successfulAsteroidDeflectionRequiresTheFlippedIdentity() {
    flip(AsteroidDeflectionSystem, asteroidDeflectionClaim)
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "requires the flipped card's identity"
  }

  @Test
  internal fun successfulAsteroidDeflectionAcceptsASpaceCard() {
    nameFlippedCard(flip(AsteroidDeflectionSystem, asteroidDeflectionClaim), AsteroidMining)
    assertCardTrackingComplete()
  }

  @Test
  internal fun successfulAsteroidDeflectionRejectsANonSpaceCard() {
    nameFlippedCard(flip(AsteroidDeflectionSystem, asteroidDeflectionClaim), AdaptedLichen)
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "AdaptedLichen does not satisfy"
  }

  @Test
  internal fun namingALaterFlipDoesNotJustifyAnEarlierClaim() {
    flip(SearchForLife, searchForLifeClaim)
    admin.runOperation("Generation")
    p1.runOperation("MC")
    nameFlippedCard(p1.cardAction1(SearchForLife) { doTask(searchForLifeClaim) }, Tardigrades)

    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }.message shouldContain
        "requires the flipped card's identity"
  }

  @Test
  internal fun failedAsteroidDeflectionMayLeaveTheCardUnknown() {
    flip(AsteroidDeflectionSystem)
    assertCardTrackingComplete()
  }

  private val searchForLifeClaim = "ClaimCardReward<TagFilter<Class<MicrobeTag>>, SearchForLife>"
  private val asteroidDeflectionClaim =
      "ClaimCardReward<TagFilter<Class<SpaceTag>>, AsteroidDeflectionSystem>"

  private fun search(filter: String, vararg cards: ClassName) {
    p1.runOperation("${cards.size} SearchForCard<$filter>") { p1.draw(*cards) }
  }

  private fun flip(card: ClassName, choice: String = "Ok"): TaskResult {
    admin.runOperation("Generation")
    admin.phase("Action")
    p1.runOperation("PROD[Energy]")
    p1.runOperation("$card, MC")
    return p1.cardAction1(card) {
      doTask(choice)
    }
  }
}
