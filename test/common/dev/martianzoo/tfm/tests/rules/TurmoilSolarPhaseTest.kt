package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TfmGameplayTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilSolarPhaseTest : TfmGameplayTest() {
  @Test
  internal fun `Solar waits for the current event before forming government and advancing events`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    // Generation 1: advance Democratic Reform from Coming to Current.
    kim.pass()
    stan.pass()
    kim.wgt("VenusStep")
    admin.doTask("SolarnetGlobalEvent")
    players.forEach { it.buyCards(0) }

    // Generation 2: Democratic Reform must finish its ocean before the government changes.
    stan.pass()
    kim.pass()
    stan.wgt("VenusStep")

    kim.count("TerraformRating") shouldBe 18
    stan.count("TerraformRating") shouldBe 18
    admin.count("Ruling<MarsFirst>") shouldBe 1
    admin.count("Current<DemocraticReform>") shouldBe 1

    stan.doTask("OceanTile<Tharsis_1_2> BY Admin")

    admin.count("Ruling<Reds>") shouldBe 1
    admin.count("DemocraticReform") shouldBe 0
    admin.count("Current<MinimalImpactPolicy>") shouldBe 1
    admin.count("Coming<SolarnetGlobalEvent>") shouldBe 1
    admin.count("Distant") shouldBe 0
    admin.doTask("FreeAcademiaTreaty")
    admin.count("Distant<FreeAcademiaTreaty>") shouldBe 1
  }

  @Test
  internal fun `TR revision happens before Red Influence calculates its charge`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    // Generation 1: Antarctica Melts brings Red Influence, due in generation 4.
    // Later reveals fix delegate placements but their effects occur after this scenario.
    kim.pass()
    stan.pass()
    kim.wgt("VenusStep")
    admin.doTask("AntarcticaMelts")
    players.forEach { it.buyCards(0) }

    // Generation 2: finish Democratic Reform's Admin ocean to advance the event queue.
    stan.pass()
    kim.pass()
    stan.wgt("VenusStep")
    stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
    admin.doTask("ExploreFirstDirective")
    players.forEach { it.buyCards(0) }

    // Generation 3: Minimal Impact Policy resolves; Red Influence becomes Current.
    kim.pass()
    stan.pass()
    kim.wgt("VenusStep")
    admin.doTask("MoralMovement")
    players.forEach { it.buyCards(0) }

    // Generation 4: raise Kim from 18 to 20 TR before Red Influence resolves.
    stan.pass()
    repeat(2) { kim.stdProject("AsteroidProject") }
    kim.count("TerraformRating") shouldBe 20
    kim.pass()
    val afterProduction = kim.count("MC")

    stan.wgt("VenusStep")

    // Revision drops Kim below the 20-TR charge bracket. The new Reds bonus belongs to Stan.
    kim.count("TerraformRating") shouldBe 19
    kim.count("MC") shouldBe afterProduction - 3
  }
}
