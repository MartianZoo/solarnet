package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MilestonesAwardsExpansionTest : TfmSandboxTest() {
  @Test
  internal fun `Briber costs twelve MC in addition to the normal claim cost`() {
    newTestGame(addOptions = "Briber, Builder, Engineer")
    kim.setToExMachina(20, "MC")

    kim.claimMilestone(cn("Briber")).expect("-20 MC, Briber")
  }

  @Test
  internal fun `Briber claim is atomic when the player cannot pay the extra cost`() {
    newTestGame(addOptions = "Briber, Builder, Engineer")
    kim.setToExMachina(19, "MC")

    shouldThrow<LimitsException> { kim.claimMilestone(cn("Briber")) }

    kim.count("MC") shouldBe 19
    kim.count("Milestone") shouldBe 0
  }

  @Test
  internal fun `Philantropist counts own scoring cards but not Vitor's reference`() {
    newTestGame(addOptions = "Vitor, Philantropist, Builder, Engineer")
    kim.exMachina("$Vitor, $SearchForLife, $Tardigrades, $ColonizerTrainingCamp, $DustSeals")
    stan.exMachina("$Trees")

    // Counting played card classes is equivalent to counting cards: CardFront permits at most
    // one played instance of each concrete class, across all owners. Vitor's class reference is
    // not a played card, and Stan's Trees is outside Kim's requirement.
    shouldThrow<RequirementException> { kim.claimMilestone(cn("Philantropist")) }

    kim.exMachina("$SpaceElevator")
    kim.claimMilestone(cn("Philantropist")).expect("-8 MC, Philantropist")
  }

  @Test
  internal fun `Merchant checks resources after the normal claim cost`() {
    newTestGame(addOptions = "Merchant, Builder, Engineer")
    kim.exMachina("2 Steel, 2 Titanium, 2 Plant, 2 Energy, 2 Heat")
    kim.setToExMachina(9, "MC")

    shouldThrow<RequirementException> { kim.claimMilestone(cn("Merchant")) }
    kim.count("MC") shouldBe 9
    kim.count("Merchant") shouldBe 0

    kim.setToExMachina(10, "MC")
    kim.claimMilestone(cn("Merchant")).expect("-8 MC, Merchant")
  }

  @Test
  internal fun `Hydrologist can be claimed after placing four oceans`() {
    newTestGame(addOptions = "Hydrologist, Builder, Engineer")
    val oceans = kim.list("WaterArea").take(4)

    kim.setToExMachina(80, "MC")
    oceans.forEach { kim.stdProject("AquiferProject") { doTask("OceanTile<$it>") } }

    shouldThrow<RequirementException> { stan.claimMilestone(cn("Hydrologist")) }
    kim.claimMilestone(cn("Hydrologist")).expect("-8 MC, Hydrologist")
  }

  // Producer wants 16 printed production, and Producer22 wants 22 because QuickStartVariant hands
  // you 6 at setup. Both start one short of their threshold after these grants.
  private fun claimProducerOneProductionShortOfThreshold(milestone: String, modules: String) {
    newTestGame(addOptions = "$milestone, Builder, Engineer$modules")
    kim.exMachina("PROD[5 Steel, 5 Titanium, 5 Plant]")

    shouldThrow<RequirementException> { kim.claimMilestone(cn(milestone)) }

    kim.exMachina("PROD[Energy]")
    kim.claimMilestone(cn(milestone)).expect("-8 MC, $milestone")
  }

  @Test
  internal fun `Producer requires sixteen printed production`() =
      claimProducerOneProductionShortOfThreshold("Producer", ", -QuickStartVariant")

  @Test
  internal fun `Producer22 requires twenty two printed production`() =
      claimProducerOneProductionShortOfThreshold("Producer22", ", -CorporateEraExpansion")

  @Test
  internal fun `Producer counts Quick Start production toward sixteen`() {
    newTestGame(addOptions = "Producer, Builder, Engineer, -CorporateEraExpansion")
    kim.exMachina("PROD[3 Steel, 3 Titanium, 3 Plant]")

    shouldThrow<RequirementException> { kim.claimMilestone(cn("Producer")) }

    kim.exMachina("PROD[Energy]")
    kim.claimMilestone(cn("Producer")).expect("-8 MC, Producer")
  }

  @Test
  internal fun `Producer22 can be selected without Quick Start`() {
    newTestGame(addOptions = "Producer22, Builder, Engineer, -QuickStartVariant")
    kim.exMachina("PROD[7 Steel, 7 Titanium, 7 Plant]")

    shouldThrow<RequirementException> { kim.claimMilestone(cn("Producer22")) }

    kim.exMachina("PROD[Energy]")
    kim.claimMilestone(cn("Producer22")).expect("-8 MC, Producer22")
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `Dry Deserts does not erase Hydrologist credit for a removed ocean`() {
      newTestGame(addOptions = "TurmoilExpansion, Hydrologist, Builder, Engineer", playerCount = 2)
      // Generation 1: Kim earns the credit that Dry Deserts will later test.
      // Revealed events fix delegate placements; none will resolve before the final claim.
      kim.turn { stdProject("AquiferProject") { placeTile(1, 2) } }
      stan.pass()
      kim.pass()
      kim.wgt("VenusStep")
      admin.doTask("SolarnetGlobalEvent")
      players.forEach { it.buyCards(0) }

      // Generation 2: Democratic Reform adds an Admin ocean elsewhere, preserving Kim's ocean.
      stan.pass()
      kim.pass()
      stan.wgt("VenusStep")
      stan.doTask("OceanTile<Tharsis_1_4> BY Admin")
      admin.doTask("FreeAcademiaTreaty")
      players.forEach { it.buyCards(0) }

      // Generation 3: Minimal Impact Policy's Dry Deserts removes Kim's original ocean.
      kim.pass()
      stan.pass()
      kim.wgt("VenusStep")
      kim.doTask("-OceanTile<Tharsis_1_2>")
      admin.doTask("SelfSufficiencyProgram")
      players.forEach { it.buyCards(0) }

      // Generation 4: reusing the emptied area still earns another credit toward four placements.
      stan.pass()
      kim.stdProject("AquiferProject") { placeTile(1, 2) }
      kim.stdProject("AquiferProject") { placeTile(1, 5) }
      kim.stdProject("AquiferProject") { placeTile(2, 6) }
      kim.claimMilestone(cn("Hydrologist")).expect("-8 MC, Hydrologist")
    }
  }
}
