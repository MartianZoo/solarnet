package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class BoomTownTest : TfmSandboxTest() {
  @Test
  internal fun `Requires a metal bonus and retains the normal city restriction`() {
    newTestGame(addOptions = "PreludeExpansion")

    shouldThrow<NarrowingException> { kim.playPrelude(BoomTown) { placeTile(4, 2) } }
    kim.exMachina("NormalCityTile<Tharsis_2_1>")
    shouldThrow<NarrowingException> { kim.playPrelude(BoomTown) { placeTile(1, 1) } }
    kim.playPrelude(BoomTown) { placeTile(8, 9) }.expect("Titanium, PROD[2 Titanium]")
  }

  @Test
  internal fun `Reduces titanium value for its owner only`() {
    newTestGame()
    kim.exMachina("$BoomTown, 3 Titanium")
    stan.exMachina("Titanium")

    kim.playProject(SmallAsteroid, mc = 4, titanium = 3).expect("-4 MC, -3 Titanium")
    stan.playProject(SpaceStation, mc = 7, titanium = 1).expect("-7 MC<Stan>, -Titanium<Stan>")
  }

  @Test
  internal fun `Double Down copies placement and production without doubling the titanium penalty`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.playPrelude(BoomTown) { placeTile(1, 1) }
    kim.playPrelude(DoubleDown) {
          doTask("CopyPrelude<$BoomTown>")
          placeTile(8, 9)
        }
        .expect("CityTile, PROD[2 Titanium]")
    startActionPhase()
    kim.setToExMachina(3, "Titanium")

    kim.playProject(SmallAsteroid, mc = 4, titanium = 3).expect("-4 MC, -3 Titanium")
  }

  @Test
  internal fun `Its penalty stacks with both alloys PhoboLog and Unity for card actions`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina(
        "$BoomTown, $PhoboLog, $AdvancedAlloys, $MercurianAlloys, $IcyImpactors, Titanium, Ruling<Unity> FROM Ruling<Greens>"
    )

    // Titanium is worth 3 - 1 + 1 + 1 + 1 + 1 = 6 MC.
    kim.cardAction1(IcyImpactors) { kim.pay(mc = 4, titanium = 1) }
        .expect("-Titanium, -4 MC, 2 Asteroid<$IcyImpactors>")
  }
}
