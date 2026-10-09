package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SponsoredAcademiesTest : TfmSandboxTest() {
  @Test
  internal fun `Point Luna draw supplies the mandatory discard when it is the only hand card`() {
    newTestGame(kimCorporation = PointLuna)
    kim.setToExMachina(1, "ProjectCard")
    kim.setToExMachina(9, "MC")

    kim.playProject(SponsoredAcademies, 9)
        .expect("2 ProjectCard<Kim>, ProjectCard<Stan>, ProjectCard<Rob>")
  }

  @Test
  internal fun `Cannot be played with only one card in hand`() {
    newTestGame()
    kim.setToExMachina(1, "ProjectCard")
    kim.setToExMachina(9, "MC")

    shouldThrow<LimitsException> { kim.playProject(SponsoredAcademies, 9) }

    kim.assertCounts(9 to "MC", 1 to "ProjectCard", 0 to "$SponsoredAcademies")
  }
}
