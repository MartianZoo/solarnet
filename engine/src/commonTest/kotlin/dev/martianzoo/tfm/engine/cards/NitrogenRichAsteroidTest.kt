package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

internal class NitrogenRichAsteroidTest : CardTest() {
  @Test
  internal fun `May choose the lesser production branch with three plant tags`() {
    newGame(CorporateEraExpansion)
    engine.phase("Action")
    p1.manual("$Ecoline, $AdaptedLichen, $Lichen")
    p1.manual("31 Megacredit, ProjectCard")

    p1.playProject(NitrogenRichAsteroid, 31) { doTask("PROD[Plant]") }.expect("PROD[Plant]")
  }
}
