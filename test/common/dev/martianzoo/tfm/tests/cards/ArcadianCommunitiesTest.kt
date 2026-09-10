package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ArcadianCommunitiesTest : CardTest() {
  @Test
  internal fun `Starts with resources and a community on any empty land area`() {
    newGame(PromoCardPack)

    playCorporationWithoutStartingProjects(p1, ArcadianCommunities)
        .expect("40 MC, 10 Steel, RequiredAction")
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { doTask("Community<Tharsis_4_2>") }

    p1.assertCounts(40 to "MC", 10 to "Steel", 1 to "Community<Tharsis_4_2>")
  }

  @Test
  internal fun `Initial community requires an empty land area but no adjacency`() {
    newGame(PromoCardPack)
    p1.runOperation("CityTile<Tharsis_1_1>")
    p1.runOperation("Community<Tharsis_1_3>")
    playCorporationWithoutStartingProjects(p1, ArcadianCommunities)
    admin.phase("Action")

    p1.stdAction("DoRequiredActionsAction") {
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_1_1>") }
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_1_3>") }
      doTask("Community<Tharsis_9_7>")
    }

    p1.assertCounts(1 to "Community<Tharsis_9_7>")
  }

  @Test
  internal fun `Action places a community adjacent to an owned tile`() {
    newGame(PromoCardPack)
    playCorporationWithoutStartingProjects(p1, ArcadianCommunities)
    p1.runOperation("CityTile<Tharsis_1_1>")
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { doTask("Community<Tharsis_9_7>") }

    p1.cardAction1(ArcadianCommunities) {
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_4_2>") }
      doTask("Community<Tharsis_2_1>")
    }

    p1.assertCounts(1 to "Community<Tharsis_2_1>")
  }

  @Test
  internal fun `Action places a community adjacent to an owned community`() {
    newGame(PromoCardPack)
    playCorporationWithoutStartingProjects(p1, ArcadianCommunities)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { doTask("Community<Tharsis_4_2>") }

    p1.cardAction1(ArcadianCommunities) { doTask("Community<Tharsis_4_3>") }

    p1.assertCounts(1 to "Community<Tharsis_4_3>")
  }

  @Test
  internal fun `Action does not chain from another player's pieces`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    playCorporationWithoutStartingProjects(p1, ArcadianCommunities)
    p2.runOperation("CityTile<Tharsis_1_1>")
    p2.runOperation("Community<Tharsis_4_2>")
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { doTask("Community<Tharsis_9_7>") }

    p1.cardAction1(ArcadianCommunities) {
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_2_1>") }
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_4_3>") }
      doTask("Community<Tharsis_9_6>")
    }
  }

  @Test
  internal fun `Developing a community removes it and pays its Arcadian owner`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    playCorporationWithoutStartingProjects(p1, ArcadianCommunities)
    p1.runOperation("Community<Tharsis_1_1>")
    p1.sneak("-40 MC")

    p1.runOperation("GreeneryTile<Tharsis_1_1>").expect("-Community, 3 MC")

    p1.assertCounts(3 to "MC", 0 to "Community<Tharsis_1_1>")
    p2.assertCounts(0 to "MC")
  }

  @Test
  internal fun `Developing a Land Claim community pays its Arcadian owner`() {
    newGame(PromoCardPack, CorporateEraExpansion)
    playCorporationWithoutStartingProjects(p1, ArcadianCommunities)
    p1.runOperation("$LandClaim") { doTask("Community<Tharsis_1_1>") }
    p1.sneak("-40 MC")

    p1.runOperation("GreeneryTile<Tharsis_1_1>").expect("-Community, 3 MC")

    p1.assertCounts(3 to "MC", 0 to "Community<Tharsis_1_1>")
  }
}
