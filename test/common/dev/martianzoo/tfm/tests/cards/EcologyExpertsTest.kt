package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EcologyExpertsTest : CardTest() {
  @Test
  internal fun `Plays Decomposers while ignoring its global requirement`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("10 MC, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) {
      doTask("PlayCard<Class<ProjectCard>, Class<$Decomposers>>")
      p1.pay(mc = 5)
    }

    p1.assertCounts(
        1 to "$Decomposers",
    )
  }

  @Test
  internal fun `Splice money from Ecology Experts tags can pay for Decomposers`() {
    newGame(PreludeExpansion, PromoCardPack)
    val p2 = requireP2()
    p2.manual("$SpliceTacticalGenomics")
    engine.phase("Prelude")
    p1.manual("4 MC, ProjectCard, PreludeCard")
    val spliceMoney = p2.count("MC")

    p1.playPrelude(EcologyExperts) {
      p1.playProject(Decomposers, 5) { doTask("2 MC<Player1>") }
    }

    p1.assertCounts(3 to "MC")
    p2.count("MC") shouldBe spliceMoney + 4
  }

  @Test
  internal fun `Can play a card without a bio tag`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("2 MC, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) { p1.playProject(DustSeals, 2) }

    p1.assertCounts(1 to "$DustSeals")
  }
}
