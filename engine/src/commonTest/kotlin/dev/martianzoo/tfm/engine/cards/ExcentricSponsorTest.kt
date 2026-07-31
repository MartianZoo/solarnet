package dev.martianzoo.tfm.engine.cards

import kotlin.test.BeforeTest
import kotlin.test.Test

class ExcentricSponsorTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame("TerraformingMars,TharsisMapOption,VenusNextExpansion,PreludeExpansion")
    engine.phase("Prelude")
    p1.manual("44, ProjectCard, PreludeCard")
  }

  @Test
  fun `with enough owed, plays a card after Excentric Sponsor`() {
    p1.playPrelude("ExcentricSponsor") { p1.playProject("NitrogenRichAsteroid", 6) }
        .expect("-6 Megacredit, PROD[Plant], 3 TerraformRating")
  }

  @Test
  fun `with less owed, plays a card after Excentric Sponsor`() {
    p1.playPrelude("ExcentricSponsor") { p1.playProject("GhgImportFromVenus", 0) }
        .expect("PROD[3 Heat], TerraformRating")
  }
}
