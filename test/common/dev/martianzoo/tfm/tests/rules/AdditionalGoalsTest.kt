package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class AdditionalGoalsTest : TfmSandboxTest() {
  @Test
  internal fun `Terraformer26 can coexist with Terraformer35 and uses its own threshold`() {
    newTestGame(addOptions = "TurmoilExpansion, Terraformer26, Terraformer35, Builder")
    kim.setToExMachina(25, "TerraformRating")
    shouldThrow<RequirementException> { kim.claimMilestone(cn("Terraformer26")) }

    kim.stdProject("AsteroidProject")
    kim.claimMilestone(cn("Terraformer26")).expect("-8 MC, Terraformer26")
    shouldThrow<RequirementException> { stan.claimMilestone(cn("Terraformer35")) }
    stan.setToExMachina(35, "TerraformRating")
    stan.claimMilestone(cn("Terraformer35")).expect("-8 MC, Terraformer35")
  }

  @Test
  internal fun `Lobbyist counts a chairman and six delegates without double counting leaders`() {
    newTestGame(addOptions = "TurmoilExpansion, Lobbyist, Builder, Engineer")
    kim.exMachina("5 PartyDelegate<Scientists>, Chairman FROM Chairman<Neutral>")
    shouldThrow<RequirementException> { kim.claimMilestone(cn("Lobbyist")) }

    kim.stdAction("LobbyAction<Action1>") { doTask("PartyDelegate<Scientists>") }
    kim.claimMilestone(cn("Lobbyist")).expect("-8 MC, Lobbyist")
  }
}
