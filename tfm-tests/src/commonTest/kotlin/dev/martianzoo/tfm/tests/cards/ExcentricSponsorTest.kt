package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ExcentricSponsorTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(VenusNextExpansion, PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("44, ProjectCard, PreludeCard")
  }

  @Test
  internal fun `Can apply its full discount to the next card`() {
    p1.playPrelude(ExcentricSponsor) { p1.playProject(NitrogenRichAsteroid, 6) }
        .expect("-6 Megacredit, PROD[Plant], 3 TerraformRating")
  }

  @Test
  internal fun `Can play a card costing less than its full discount`() {
    p1.playPrelude(ExcentricSponsor) { p1.playProject(GhgImportFromVenus, 0) }
        .expect("PROD[3 Heat], TerraformRating")
  }
}
