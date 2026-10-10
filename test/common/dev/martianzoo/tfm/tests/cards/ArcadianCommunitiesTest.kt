package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ArcadianCommunitiesTest : TfmSandboxTest() {
  @Test
  internal fun `Initial community requires an empty land area but no adjacency`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.exMachina("NormalCityTile<Tharsis_1_1>")
    kim.exMachina("Community<Tharsis_1_3>")

    kim.stdAction("RequiredActionsSignal") {
          shouldThrow<LimitsException> { doTask("Community<Tharsis_1_1>") }
          shouldThrow<LimitsException> { doTask("Community<Tharsis_1_3>") }
          doTask("Community<Tharsis_9_7>")
        }
        .expect("Community<Tharsis_9_7>")
  }

  @Test
  internal fun `Action places a community adjacent to an owned tile`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.exMachina("NormalCityTile<Tharsis_1_1>")
    kim.exMachina("GreeneryTile<Tharsis_2_2>")
    kim.stdAction("RequiredActionsSignal") { doTask("Community<Tharsis_9_7>") }

    kim.cardAction1(ArcadianCommunities) {
          shouldThrow<NarrowingException> { doTask("Community<Tharsis_4_2>") }
          shouldThrow<LimitsException> { doTask("Community<Tharsis_2_2>") }
          doTask("Community<Tharsis_2_1>")
        }
        .expect("Community<Tharsis_2_1>")
  }

  @Test
  internal fun `Action places a community adjacent to an owned community`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.stdAction("RequiredActionsSignal") { doTask("Community<Tharsis_4_2>") }

    kim.cardAction1(ArcadianCommunities) { doTask("Community<Tharsis_4_3>") }
        .expect("Community<Tharsis_4_3>")
  }

  @Test
  internal fun `Action does not chain from another player's pieces`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    stan.exMachina("NormalCityTile<Tharsis_1_1>")
    stan.exMachina("Community<Tharsis_4_2>")
    kim.stdAction("RequiredActionsSignal") { doTask("Community<Tharsis_9_7>") }

    kim.cardAction1(ArcadianCommunities) {
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_2_1>") }
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_4_3>") }
      doTask("Community<Tharsis_9_6>")
    }
  }

  @Test
  internal fun `Developing a community removes it and pays its Arcadian owner`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.stdAction("RequiredActionsSignal") { doTask("Community<Tharsis_1_1>") }
    kim.setToExMachina(23, "MC")

    kim.stdProject("GreeneryProject") { placeTile(1, 1) }.expect("-Community, -20 MC, 0 MC<Stan>")
  }

  @Test
  internal fun `Developing a Land Claim community pays its Arcadian owner`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.stdAction("RequiredActionsSignal") { doTask("Community<Tharsis_9_7>") }
    kim.playProject(LandClaim, 1) { doTask("Community<Tharsis_1_1>") }
    kim.setToExMachina(23, "MC")

    kim.stdProject("GreeneryProject") { placeTile(1, 1) }.expect("-Community, -20 MC")
  }

  @Test
  internal fun `Artificial Lake pays the claim owner even though oceans are unowned`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.stdAction("RequiredActionsSignal") { doTask("Community<Tharsis_1_3>") }
    kim.setToExMachina(14, "MC")
    kim.setToExMachina(12, "TemperatureStep")

    shouldThrow<LimitsException> { stan.playProject(ArtificialLake, 15) { placeTile(1, 3) } }
    kim.setToExMachina(15, "MC")
    kim.playProject(ArtificialLake, 15) { placeTile(1, 3) }
        .expect("-Community, -12 MC, OceanTile<Tharsis_1_3>, 0 MC<Stan>")
  }
}
