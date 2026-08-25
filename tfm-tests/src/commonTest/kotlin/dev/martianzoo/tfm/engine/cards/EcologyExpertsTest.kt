package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.engine.TestOption.PreludeExpansion
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EcologyExpertsTest : CardTest() {
  @Test
  internal fun `Plays Decomposers while ignoring its global requirement`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("10 Megacredit, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) {
      doTask("PlayCard<Class<ProjectCard>, Class<$Decomposers>>")
      p1.pay(megacredits = 5)
    }

    p1.assertCounts(
        1 to "$Decomposers",
        1 to "Microbe<$Decomposers>",
    )
  }

  @Test
  internal fun `Splice money from Ecology Experts tags can pay for Decomposers`() {
    newGame(PreludeExpansion, PromoCardPack)
    val p2 = requireP2()
    p2.manual("$SpliceTacticalGenomics")
    engine.phase("Prelude")
    p1.manual("4 Megacredit, ProjectCard, PreludeCard")
    val spliceMoney = p2.count("Megacredit")

    p1.playPrelude(EcologyExperts) {
      p1.playProject(Decomposers, 5) { doTask("2 Megacredit<Player1>") }
    }

    p1.assertCounts(3 to "Megacredit", 1 to "Microbe<$Decomposers>")
    p2.count("Megacredit") shouldBe spliceMoney + 4
  }

  @Test
  internal fun `Can play a card without a bio tag`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("2 Megacredit, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) { p1.playProject(DustSeals, 2) }

    p1.assertCounts(1 to "$DustSeals")
  }

  @Test
  internal fun `Viral Enhancers sees only its own plant tag`() {
    newGame(PreludeExpansion, CorporateEraExpansion)
    engine.phase("Prelude")
    p1.manual("9 Megacredit, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) {
      p1.playProject(ViralEnhancers, 9)
    }

    p1.assertCounts(1 to "Plant")
  }

  @Test
  internal fun `Ecological Zone sees only its own tags`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("12 Megacredit, ProjectCard, PreludeCard, GreeneryTile<Tharsis_4_4>")

    p1.playPrelude(EcologyExperts) {
      p1.playProject(EcologicalZone, 12) { placeTile(4, 5) }
    }

    p1.assertCounts(2 to "Animal<$EcologicalZone>")
  }
}
