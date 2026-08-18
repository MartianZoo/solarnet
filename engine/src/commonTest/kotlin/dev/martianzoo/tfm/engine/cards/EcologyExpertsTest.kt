package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.engine.TestOption.PreludeExpansion
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class EcologyExpertsTest : CardTest() {
  @Test
  fun `plays Decomposers while ignoring its global requirement`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("10 Megacredit, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) {
      doTask("PlayCard<Class<ProjectCard>, Class<$Decomposers>>")
      p1.pay(megacredits = 5)
    }

    p1.assertCounts(
        1 to "$Decomposers",
        3 to "Microbe<$Decomposers>",
    )
  }

  @Test
  fun `can play a card without a bio tag`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("2 Megacredit, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) { p1.playProject(DustSeals, 2) }

    p1.assertCounts(1 to "$DustSeals")
  }

  @Test
  fun `Viral Enhancers sees all three relevant microbe and plant tags`() {
    newGame(PreludeExpansion, CorporateEraExpansion)
    engine.phase("Prelude")
    p1.manual("9 Megacredit, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) {
      p1.playProject(ViralEnhancers, 9)
    }

    p1.assertCounts(3 to "Plant")
  }

  @Test
  fun `Ecological Zone sees its plant tag`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("12 Megacredit, ProjectCard, PreludeCard, GreeneryTile<Tharsis_4_4>")

    p1.playPrelude(EcologyExperts) {
      p1.playProject(EcologicalZone, 12) { doTask("Tile128<Tharsis_4_5>") }
    }

    p1.assertCounts(3 to "Animal<$EcologicalZone>")
  }
}
