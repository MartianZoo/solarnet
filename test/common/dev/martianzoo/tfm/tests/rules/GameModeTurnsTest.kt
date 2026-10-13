package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameModeTurnsTest : TfmGameplayTest() {
  @Test
  internal fun multiplayerSkipsPassedPlayersAndLetsTheLastPlayerContinue() {
    newTestGame(playerCount = 3)

    kim.sellPatents(1).expect("-ProjectCard, MC")
    shouldThrow<NarrowingException> { kim.pass() }
    kim.declineSecondAction()
    stan.pass()
    rob.turn { sellPatents(1).expect("-ProjectCard, MC") }

    // Declining the second action did not pass Kim; Stan is skipped on the next circuit.
    kim.turn { sellPatents(1).expect("-ProjectCard, MC") }
    rob.pass()

    kim.sellPatents(1).expect("-ProjectCard, MC")
    kim.sellPatents(1).expect("-ProjectCard, MC")
    kim.pass()
    admin.count("SolarPhase") shouldBe 1

    kim.wgt("VenusStep")
    players.forEach { it.buyCards(0) }
    admin.count("Generation") shouldBe 2
    // Passing only lasts for that generation, and the start token has moved to Stan.
    stan.sellPatents(1).expect("-ProjectCard, MC")
  }

  @Test
  internal fun soloKeepsOfferingActionsUntilThePlayerPasses() {
    newTestGame(playerCount = 1)

    repeat(3) { kim.sellPatents(1).expect("-ProjectCard, MC") }
    kim.pass()
    admin.count("SolarPhase") shouldBe 1

    kim.wgt("VenusStep")
    kim.buyCards(0)
    admin.count("Generation") shouldBe 2
    kim.sellPatents(1).expect("-ProjectCard, MC")
  }
}
