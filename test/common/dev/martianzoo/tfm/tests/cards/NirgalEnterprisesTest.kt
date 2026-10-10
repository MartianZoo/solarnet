package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class NirgalEnterprisesTest : TfmSandboxTest() {
  @Test
  internal fun `Claims a milestone without paying its usual fee`() {
    newTestGame(kimCorporation = NirgalEnterprises)
    kim.setToExMachina(16, "ProjectCard")

    kim.claimMilestone(cn("Planner")).expect("Milestone, 0 MC")
  }

  @Test
  internal fun `Funds an award without paying its usual fee`() {
    newTestGame(kimCorporation = NirgalEnterprises)

    kim.fundAward(cn("Landlord"), 0).expect("Award, 0 MC")
  }

  @Test
  internal fun `Still pays the twelve MC required by Briber`() {
    newTestGame("Briber, Builder, Engineer", kimCorporation = NirgalEnterprises)
    kim.setToExMachina(12, "MC")

    kim.claimMilestone(cn("Briber")).expect("-12 MC, Briber")
  }
}
