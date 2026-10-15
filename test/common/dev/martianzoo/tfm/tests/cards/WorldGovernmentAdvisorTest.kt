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
        .inTurn {
          doTask("UseCardAction<WorldGovernmentAdvisor, Action1>")

          doTask("TemperatureStep BY Admin")
        }
        .expect("0 TerraformRating")
  }

  @Test
  internal fun `Raises Venus and triggers Aphrodite even with World Government disabled`() {
    newTestGame("WorldGovernmentAdvisor, -WorldGovernmentRule")
    kim.exMachina("$WorldGovernmentAdvisor")
    stan.exMachina("$Aphrodite")

    kim.inTurn {
          doTask("UseCardAction<WorldGovernmentAdvisor, Action1>")

          doTask("VenusStep BY Admin")
          stan.doTask("2 MC")
        }
        .expect("0 TerraformRating")
  }

  @Test
  internal fun `Neutral oceans trigger other players effects but give no ocean credit`() {
    newTestGame("WorldGovernmentAdvisor, LakefrontResorts, Hydrologist, Builder, Engineer")
    kim.exMachina("$WorldGovernmentAdvisor")
    stan.exMachina("$ArcticAlgae, $LakefrontResorts")

    kim.inTurn {
          doTask("UseCardAction<WorldGovernmentAdvisor, Action1>")

          doTask("OceanTile<Tharsis_1_2> BY Admin")
          stan.doTask("2 Plant")
          stan.doTask("PROD[MC]")
        }
        .expect("PROD[MC<Stan>], 0 OceanCredit, 0 TerraformRating")
  }

  @Test
  internal fun `Its owner places the neutral ocean awarded at zero degrees`() {
    newTestGame("WorldGovernmentAdvisor")
    kim.exMachina("$WorldGovernmentAdvisor, 14 TemperatureStep")

    kim.inTurn {
          doTask("UseCardAction<WorldGovernmentAdvisor, Action1>")

          doTask("TemperatureStep BY Admin")
          // Hand the threshold ocean choice to the start player before placing it.
          selectTask("OceanTile<> BY Admin")

          doTask("OceanTile<Tharsis_1_2> BY Admin")
        }
        .expect("0 TerraformRating, 0 Plant")
  }

  @Test
  internal fun `Homeostasis Bureau pays for personal terraforming but not this action`() {
    newTestGame("WorldGovernmentAdvisor")
    kim.exMachina("$WorldGovernmentAdvisor, $HomeostasisBureau")

    kim.inTurn {
          doTask("UseCardAction<WorldGovernmentAdvisor, Action1>")

          doTask("TemperatureStep BY Admin")
        }
        .expect("0 TerraformRating, 0 MC")
    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "14 Pay<> FROM MC")
      doTask("TemperatureStep")
      doTasks("TerraformRating", "3 MC")
    }
  }
}
