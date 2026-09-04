package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.BoomTown
import dev.martianzoo.tfm.tests.cards.cardnames.DoubleDown
import dev.martianzoo.tfm.tests.cards.cardnames.SmallAsteroid
import dev.martianzoo.tfm.tests.cards.cardnames.SpaceStation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class BoomTownTest : CardTest() {
  @Test
  internal fun `Accepts either metal bonus while retaining the normal city restriction`() {
    newGame(PreludeExpansion, PromoCardPack)
    engine.phase("Prelude")

    shouldThrow<NarrowingException> { p1.playPrelude(BoomTown) { placeTile(4, 2) } }
    p1.manual("CityTile<Tharsis_2_1>")
    shouldThrow<NarrowingException> { p1.playPrelude(BoomTown) { placeTile(1, 1) } }
    p1.playPrelude(BoomTown) { placeTile(8, 9) }.expect("Titanium, PROD[2 Titanium]")
  }

  @Test
  internal fun `Reduces every titanium for its owner and restores the value when removed`() {
    newGame(CorporateEraExpansion, PreludeExpansion, PromoCardPack)
    val p2 = requireP2()
    engine.phase("Prelude")
    p1.playPrelude(BoomTown) { placeTile(1, 1) }

    p1.count("BaseResourceValue<Class<Titanium>>") shouldBe 2
    p2.count("BaseResourceValue<Class<Titanium>>") shouldBe 3

    engine.phase("Action")
    p1.manual("4 MC, 3 Titanium, ProjectCard")
    p2.manual("7 MC, Titanium, ProjectCard")
    p1.playProject(SmallAsteroid, mc = 4, titanium = 3)
    p2.playProject(SpaceStation, mc = 7, titanium = 1)

    p1.count("MC") shouldBe 0
    p1.count("Titanium") shouldBe 0
    p2.count("MC") shouldBe 0

    p1.manual("-$BoomTown")
    p1.count("BaseResourceValue<Class<Titanium>>") shouldBe 3
  }

  @Test
  internal fun `Double Down copies the placement and production but not the persistent penalty`() {
    newGame(PreludeExpansion, PromoCardPack)
    engine.phase("Prelude")
    p1.playPrelude(BoomTown) { placeTile(1, 1) }

    p1.playPrelude(DoubleDown) {
      doTask("CopyPrelude<$BoomTown>")
      placeTile(8, 9)
    }

    p1.count("CityTile") shouldBe 2
    p1.count("PROD[Titanium]") shouldBe 4
    p1.count("BaseResourceValue<Class<Titanium>>") shouldBe 2
  }
}
