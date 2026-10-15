package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class NirgalEnterprisesTest : TfmSandboxTest() {
  @Test
  internal fun `Claims a milestone without paying its usual fee`() {
    newTestGame(addOptions = "NirgalEnterprises", kimCorporation = NirgalEnterprises)
    kim.setToExMachina(16, "ProjectCard")

    kim.inTurn {
          doTask("ClaimMilestone<Class<Planner>>")
          doTask("Planner")
        }
        .expect("0 MC")
  }

  @Test
  internal fun `Funds an award without paying its usual fee`() {
    newTestGame(addOptions = "NirgalEnterprises", kimCorporation = NirgalEnterprises)

    kim.inTurn {
          doTask("FundAward<Class<Landlord>>")
          doTask("Landlord")
        }
        .expect("0 MC")
  }

  @Test
  internal fun `Still pays the twelve MC required by Briber`() {
    newTestGame("Briber, Builder, Engineer, NirgalEnterprises", kimCorporation = NirgalEnterprises)
    kim.setToExMachina(12, "MC")

    kim.inTurn {
      doTask("ClaimMilestone<Class<Briber>>")
      doTask("Briber")
      doTask("-12 MC")
    }
  }
}
