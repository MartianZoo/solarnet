package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class BoomTownTest : CardTest() {
  @Test
  internal fun `Accepts either metal bonus while retaining the normal city restriction`() {
    newGame(PreludeExpansion, PromoCardPack)
    admin.phase("Prelude")

    shouldThrow<NarrowingException> { p1.playPrelude(BoomTown) { placeTile(4, 2) } }
    p1.runOperation("CityTile<Tharsis_2_1>")
    shouldThrow<NarrowingException> { p1.playPrelude(BoomTown) { placeTile(1, 1) } }
    p1.playPrelude(BoomTown) { placeTile(8, 9) }.expect("Titanium, PROD[2 Titanium]")
  }

  @Test
  internal fun `Reduces every titanium for its owner and restores the value when removed`() {
    newGame(CorporateEraExpansion, PreludeExpansion, PromoCardPack)
    val p2 = requireP2()
    admin.phase("Prelude")
    p1.playPrelude(BoomTown) { placeTile(1, 1) }

    p1.count("BaseResourceValue<Class<Titanium>>") shouldBe 2
    p2.count("BaseResourceValue<Class<Titanium>>") shouldBe 3

    admin.phase("Action")
    p1.runOperation("4 MC, 3 Titanium, ProjectCard")
    p2.runOperation("7 MC, Titanium, ProjectCard")
    p1.playProject(SmallAsteroid, mc = 4, titanium = 3)
    p2.playProject(SpaceStation, mc = 7, titanium = 1)

    p1.count("MC") shouldBe 0
    p1.count("Titanium") shouldBe 0
    p2.count("MC") shouldBe 0

    p1.runOperation("-$BoomTown")
    p1.count("BaseResourceValue<Class<Titanium>>") shouldBe 3
  }

  @Test
  internal fun `Double Down copies the placement and production but not the persistent penalty`() {
    newGame(PreludeExpansion, PromoCardPack)
    admin.phase("Prelude")
    p1.playPrelude(BoomTown) { placeTile(1, 1) }

    p1.playPrelude(DoubleDown) {
      doTask("CopyPrelude<$BoomTown>")
      placeTile(8, 9)
    }

    p1.count("CityTile") shouldBe 2
    p1.count("PROD[Titanium]") shouldBe 4
    p1.count("BaseResourceValue<Class<Titanium>>") shouldBe 2
  }

  @Test
  internal fun `Its titanium penalty stacks with both alloys PhoboLog and Unity on card actions`() {
    newGame(
        CorporateEraExpansion,
        PreludeExpansion,
        PromoCardPack,
        TurmoilExpansion,
        startingProjects = listOf(5),
    )
    p1.playCorp(PhoboLog)
    admin.phase("Prelude")
    p1.playPrelude(BoomTown) { placeTile(1, 1) }
    admin.runOperation("Ruling<Unity> FROM Ruling<Greens>")
    admin.phase("Action")
    p1.runOperation("50 MC")
    p1.playProject(Research, 11)
    p1.playProject(AdvancedAlloys, 9)
    p1.playProject(MercurianAlloys, 3)
    p1.playProject(IcyImpactors, 15)

    // Each titanium is worth 3 - 1 + 1 + 1 + 1 + 1 = 6 MC; one plus 4 MC pays exactly 10.
    p1.cardAction1(IcyImpactors) {
          p1.pay(mc = 4, titanium = 1)
        }
        .expect("-Titanium, -4 MC, 2 Asteroid<$IcyImpactors>")
  }
}
