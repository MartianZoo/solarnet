package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class HighCirclesTest : TfmSandboxTest() {
  @Test
  internal fun `Its delegates enable a Scientists project through Excentric Sponsor during Preludes`() {
    newTestGame("PreludeExpansion, HighCircles, TurmoilExpansion", playerCount = 2)
    kim.playPrelude(HighCircles) { doTask("2 PartyDelegate<Scientists>") }

    with(kim) { playPrelude(ExcentricSponsor) { playProject(SupportedResearch, 0) } }
        .expect("ProjectCard, $SupportedResearch")
  }

  @Test
  internal fun `Its delegates enable a Scientists project through Ecology Experts during Preludes`() {
    newTestGame(
        "PreludeExpansion, HighCircles, TurmoilExpansion, EcologyExperts, Unsafe",
        playerCount = 2,
    )
    kim.playPrelude(HighCircles) { doTask("2 PartyDelegate<Scientists>") }

    with(kim) { playPrelude(EcologyExperts) { playProject(SupportedResearch, 3) } }
        .expect("ProjectCard, $SupportedResearch")
  }
}
