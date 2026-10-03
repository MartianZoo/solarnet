package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.fakeWildTags
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ExcentricSponsorTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(VenusNextExpansion, PreludeExpansion)
    admin.phase("Prelude")
    p1.runOperation("44 MC, ProjectCard, PreludeCard")
  }

  @Test
  internal fun `Can apply its full discount to the next card`() {
    with(p1) {
      playPrelude(ExcentricSponsor) { playProject(NitrogenRichAsteroid, 6) }
          .expect("-6 MC, PROD[Plant], 3 TerraformRating")
    }
  }

  @Test
  internal fun `Can play a card costing less than its full discount`() {
    with(p1) {
      playPrelude(ExcentricSponsor) { playProject(GhgImportFromVenus, 0) }
          .expect("PROD[3 Heat], TerraformRating")
    }
  }

  @Test
  internal fun `Double Down copies its project play and discount`() {
    newGame(PreludeExpansion, PromoCardPack)
    admin.phase("Prelude")
    p1.runOperation("2 ProjectCard, 2 PreludeCard")

    with(p1) {
      playPrelude(ExcentricSponsor) { playProject(DustSeals, 0) }
      playPrelude(DoubleDown) {
        doTask("CopyPrelude<$ExcentricSponsor>")
        playProject(Mine, 0)
      }
    }

    p1.count("$Mine") shouldBe 1
    p1.count("PROD[Steel]") shouldBe 1
  }

  @Test
  internal fun `An assigned wild science tag satisfies Excentric Sponsor during the Prelude phase`() {
    newGame(PreludeExpansion, VenusNextExpansion, FakeStuffBundle)
    p1.playCorp(Inventrix, 0)
    admin.phase("Prelude")
    p1.playPrelude(FakeResearchNetwork)
    shouldThrow<RequirementException> {
      with(p1) { playPrelude(ExcentricSponsor) { playProject(FloatingHabs, 0) } }
    }

    with(p1) {
      runOperation("${fakeWildTags("ScienceTag")}, NewTurn") {
            playPrelude(ExcentricSponsor) { playProject(FloatingHabs, 0) }
          }
          .expect("$FloatingHabs, 0 MC")
    }
    p1.count("FakeWildTagUse") shouldBe 0
  }
}
