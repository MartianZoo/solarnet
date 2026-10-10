package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.PoliticalAlliance
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class PoliticalAllianceTest : TfmSandboxTest() {
  @Test
  internal fun `Cannot be played without Turmoil`() {
    newTestGame()

    shouldThrow<DeadEndException> { kim.playProject(PoliticalAlliance, 4) }
  }

  @Test
  internal fun `Cannot be played without party leadership`() {
    newTestGame(addOptions = "TurmoilExpansion")

    shouldThrow<RequirementException> { kim.playProject(PoliticalAlliance, 4) }
  }

  @Test
  internal fun `Cannot substitute the chairmanship for a second party leader`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("-Chairman<Neutral>, Chairman, PartyDelegate<Scientists>")

    shouldThrow<RequirementException> { kim.playProject(PoliticalAlliance, 4) }
  }

  @Test
  internal fun `Can be played with two party leaders and no chairmanship`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyDelegate<Scientists>, PartyDelegate<Unity>")

    kim.playProject(PoliticalAlliance, 4).expect("TerraformRating")
  }
}
