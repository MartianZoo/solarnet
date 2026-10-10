package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SummitLogisticsTest : TfmSandboxTest() {
  @Test
  internal fun `Is unavailable when Turmoil is absent`() {
    newTestGame("Prelude2CardPack")

    shouldThrow<DeadEndException> { kim.playProject(SummitLogistics, 10) }
  }

  @Test
  internal fun `Counts Earth tags Jovian tags and colonies when Venus Next is absent`() {
    newTestGame("SummitLogistics, TurmoilExpansion, -VenusNextExpansion, Luna")
    kim.exMachina("$EarthOffice, $VestaShipyard, Colony<Luna>, 2 PartyDelegate<Scientists>")

    kim.playProject(SummitLogistics, 10).expect("-7 MC, ProjectCard")
  }

  @Test
  internal fun `Two delegates satisfy its requirement without Scientists ruling`() {
    newTestGame("SummitLogistics, TurmoilExpansion")

    shouldThrow<RequirementException> { kim.playProject(SummitLogistics, 10) }
    kim.exMachina("2 PartyDelegate<Scientists>")
    kim.playProject(SummitLogistics, 10).expect("ProjectCard")
  }

  @Test
  internal fun `Counts all planetary tags and each owned colony`() {
    newTestGame("SummitLogistics, TurmoilExpansion, Luna, Io")
    kim.exMachina(
        "$EarthOffice, $VestaShipyard, $VenusGovernor, Colony<Luna>, Colony<Io>, 2 PartyDelegate<Scientists>"
    )
    stan.exMachina("Colony<Luna>")

    kim.playProject(SummitLogistics, 10).expect("-4 MC, ProjectCard")
  }
}
