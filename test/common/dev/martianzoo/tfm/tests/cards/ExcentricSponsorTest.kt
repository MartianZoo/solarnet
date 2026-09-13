package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
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
}
