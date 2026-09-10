package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class NitrogenRichAsteroidTest : CardTest() {
  @Test
  internal fun `May choose the lesser production branch with three plant tags`() {
    newGame(CorporateEraExpansion)
    admin.phase("Action")
    p1.runOperation("$Ecoline, $AdaptedLichen, $Lichen")
    p1.runOperation("31 MC, ProjectCard")

    p1.playProject(NitrogenRichAsteroid, 31) { doTask("PROD[Plant]") }.expect("PROD[Plant]")
  }
}
