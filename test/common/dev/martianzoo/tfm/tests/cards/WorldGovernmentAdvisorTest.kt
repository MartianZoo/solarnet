package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class WorldGovernmentAdvisorTest : TfmSandboxTest() {
  @Test
  internal fun `Its owner chooses even when another player holds the start token`() {
    newTestGame("WorldGovernmentAdvisor")
    stan.exMachina("$WorldGovernmentAdvisor")

    stan
        .cardAction1(WorldGovernmentAdvisor) { doTask("TemperatureStep BY Admin") }
        .expect("TemperatureStep, 0 TerraformRating")
  }

  @Test
  internal fun `Raises Venus and triggers Aphrodite even with World Government disabled`() {
    newTestGame("WorldGovernmentAdvisor, -WorldGovernmentRule")
    kim.exMachina("$WorldGovernmentAdvisor")
    stan.exMachina("$Aphrodite")

    kim.cardAction1(WorldGovernmentAdvisor) { doTask("VenusStep BY Admin") }
        .expect("VenusStep, 0 TerraformRating, 2 MC<Stan>")
  }

  @Test
  internal fun `Neutral oceans trigger other players effects but give no ocean credit`() {
    newTestGame("WorldGovernmentAdvisor, LakefrontResorts, Hydrologist, Builder, Engineer")
    kim.exMachina("$WorldGovernmentAdvisor")
    stan.exMachina("$ArcticAlgae, $LakefrontResorts")

    kim.cardAction1(WorldGovernmentAdvisor) { doTask("OceanTile<Tharsis_1_2> BY Admin") }
        .expect(
            "OceanTile<Tharsis_1_2>, 2 Plant<Stan>, PROD[MC<Stan>], 0 OceanCredit, 0 TerraformRating"
        )
  }

  @Test
  internal fun `Its owner places the neutral ocean awarded at zero degrees`() {
    newTestGame("WorldGovernmentAdvisor")
    kim.exMachina("$WorldGovernmentAdvisor, 14 TemperatureStep")

    kim.cardAction1(WorldGovernmentAdvisor) {
          doTask("TemperatureStep BY Admin")
          doTask("OceanTile<Tharsis_1_2> BY Admin")
        }
        .expect("TemperatureStep, OceanTile<Tharsis_1_2>, 0 TerraformRating, 0 Plant")
  }

  @Test
  internal fun `Homeostasis Bureau pays for personal terraforming but not this action`() {
    newTestGame("WorldGovernmentAdvisor")
    kim.exMachina("$WorldGovernmentAdvisor, $HomeostasisBureau")

    kim.cardAction1(WorldGovernmentAdvisor) { doTask("TemperatureStep BY Admin") }
        .expect("TemperatureStep, 0 TerraformRating, 0 MC")
    kim.stdProject("AsteroidProject").expect("TemperatureStep, TerraformRating, -11 MC")
  }
}
