package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EcologyExpertsTest : CardTest() {
  @Test
  internal fun `Cannot choose another Prelude card`() {
    newGame(GameConfig("PreludeExpansion, EcologyExperts, Unsafe", "Player1", "Player2"))
    admin.phase("Prelude")
    p1.runOperation("ProjectCard, 2 PreludeCard")
    val unplayedPreludes = p1.count("PreludeCard")

    shouldThrow<NarrowingException> {
      with(p1) { playPrelude(EcologyExperts) { playPrelude(ExcentricSponsor) } }
    }

    p1.assertCounts(0 to "$EcologyExperts", 0 to "$ExcentricSponsor")
    p1.count("PreludeCard") shouldBe unplayedPreludes
  }

  @Test
  internal fun `Plays Decomposers while ignoring its global requirement`() {
    newGame(GameConfig("PreludeExpansion, EcologyExperts, Unsafe", "Player1", "Player2"))
    admin.phase("Prelude")
    p1.runOperation("10 MC, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) {
      doTask("PlayCard<Class<ProjectCard>, Class<$Decomposers>, Hand>")
      p1.pay(mc = 5)
    }

    p1.assertCounts(
        1 to "$Decomposers",
    )
  }

  @Test
  internal fun `Splice money from Ecology Experts tags can pay for Decomposers`() {
    newGame(
        GameConfig("PreludeExpansion, PromoCardPack, EcologyExperts, Unsafe", "Player1", "Player2")
    )
    val p2 = requireP2()
    p2.runOperation("$SpliceTacticalGenomics") { doTask("2 MC") }
    admin.phase("Prelude")
    p1.runOperation("4 MC, ProjectCard, PreludeCard")
    val spliceMoney = p2.count("MC")

    with(p1) {
      playPrelude(EcologyExperts) {
        doTask("2 MC<Player1>")
        playProject(Decomposers, 5) { doTask("2 MC<Player1>") }
      }
    }

    p1.assertCounts(3 to "MC")
    p2.count("MC") shouldBe spliceMoney + 4
  }

  @Test
  internal fun `Can play a card without a bio tag`() {
    newGame(GameConfig("PreludeExpansion, EcologyExperts, Unsafe", "Player1", "Player2"))
    admin.phase("Prelude")
    p1.runOperation("2 MC, ProjectCard, PreludeCard")

    with(p1) {
      playPrelude(EcologyExperts) { playProject(DustSeals, 2) }
    }

    p1.assertCounts(1 to "$DustSeals")
  }

  @Test
  internal fun `Double Down copies Ecology Experts project play and requirement waiver`() {
    newGame(
        GameConfig("PreludeExpansion, PromoCardPack, EcologyExperts, Unsafe", "Player1", "Player2")
    )
    admin.phase("Prelude")
    p1.runOperation("7 MC, 2 ProjectCard, 2 PreludeCard")

    with(p1) {
      playPrelude(EcologyExperts) { playProject(DustSeals, 2) }
      playPrelude(DoubleDown) {
        doTask("CopyPrelude<$EcologyExperts>")
        playProject(Decomposers, 5)
      }
    }

    p1.assertCounts(1 to "$DustSeals", 1 to "$Decomposers")
  }

  @Test
  internal fun `Ecology Experts does not waive a political requirement`() {
    newGame(
        GameConfig(
            "PreludeExpansion, TurmoilExpansion, EcologyExperts, Unsafe",
            "Player1",
            "Player2",
        )
    )
    admin.phase("Prelude")
    p1.runOperation("20 MC, ProjectCard")

    shouldThrow<RequirementException> {
      with(p1) { playPrelude(EcologyExperts) { playProject(SupportedResearch, 3) } }
    }
    p1.assertCounts(0 to "$EcologyExperts", 0 to "$SupportedResearch", 1 to "ProjectCard")
  }
}
