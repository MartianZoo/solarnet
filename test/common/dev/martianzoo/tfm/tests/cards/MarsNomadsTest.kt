package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.TurmoilCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.LakefrontResorts
import dev.martianzoo.tfm.tests.cards.cardnames.LandClaim
import dev.martianzoo.tfm.tests.cards.cardnames.MarsNomads
import dev.martianzoo.tfm.tests.cards.cardnames.MiningGuild
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import kotlin.test.Test

internal class MarsNomadsTest : CardTest() {
  @Test
  internal fun `Play places the marker on an empty land area without collecting its bonus`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.runOperation("CityTile<Tharsis_2_1>")
    p2.runOperation("Community<Tharsis_1_3>")
    p1.runOperation("OceanTile<Tharsis_1_2>")
    p1.runOperation("-2 Steel")

    p1.runOperation("$MarsNomads") {
      shouldThrowAny { doTask("NomadsMarker<Tharsis_1_2>") }
      shouldThrow<NarrowingException> { doTask("NomadsMarker<Tharsis_2_1>") }
      shouldThrow<NarrowingException> { doTask("NomadsMarker<Tharsis_1_3>") }
      doTask("NomadsMarker<Tharsis_1_1>")
    }

    p1.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 0 to "Steel", 0 to "MC")
  }

  @Test
  internal fun `Action moves the marker to an adjacent area and collects its placement bonus`() {
    newGame(PromoCardPack)
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_2_1>") }
    admin.phase("Action")

    p1.cardAction1(MarsNomads) {
      doTask("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>")
    }

    p1.assertCounts(
        0 to "NomadsMarker<Tharsis_2_1>",
        1 to "NomadsMarker<Tharsis_1_1>",
        2 to "Steel",
    )
  }

  @Test
  internal fun `Action requires an adjacent unoccupied destination`() {
    newGame(PromoCardPack)
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_1_1>") }
    p1.runOperation("CityTile<Tharsis_2_1>")
    admin.phase("Action")

    p1.cardAction1(MarsNomads) {
      shouldThrow<NarrowingException> {
        doTask("NomadsMarker<Tharsis_1_1 FROM Tharsis_1_1>")
      }
      shouldThrow<NarrowingException> {
        doTask("NomadsMarker<Tharsis_4_2 FROM Tharsis_1_1>")
      }
      shouldThrow<NarrowingException> {
        doTask("NomadsMarker<Tharsis_2_1 FROM Tharsis_1_1>")
      }
      doTask("NomadsMarker<Tharsis_2_2 FROM Tharsis_1_1>")
    }

    p1.assertCounts(1 to "NomadsMarker<Tharsis_2_2>")
  }

  @Test
  internal fun `Marker blocks tiles without reserving its current area`() {
    newGame(PromoCardPack, CorporateEraExpansion)
    val p2 = requireP2()
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_1_1>") }

    shouldThrow<DeadEndException> { p1.runOperation("CityTile<Tharsis_1_1>") }
    shouldThrow<DeadEndException> { p2.runOperation("GreeneryTile<Tharsis_1_1>") }
    p2.runOperation("$LandClaim") { doTask("Community<Tharsis_1_1>") }
    p2.assertCounts(
        1 to "Community<Tharsis_1_1>",
        1 to "NomadsMarker<Player1, Tharsis_1_1>",
    )
    admin.phase("Action")
    p1.cardAction1(MarsNomads) {
      doTask("NomadsMarker<Tharsis_2_2 FROM Tharsis_1_1>")
    }

    p2.runOperation("CityTile<Tharsis_1_1>")
    shouldThrow<DeadEndException> { p2.runOperation("CityTile<Tharsis_2_2>") }
  }

  @Test
  internal fun `Movement earns the normal bonus for each adjacent ocean`() {
    newGame(PromoCardPack)
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_4_7>") }
    p1.runOperation("OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>, OceanTile<Tharsis_6_8>")
    admin.phase("Action")

    p1.cardAction1(MarsNomads) {
          doTask("NomadsMarker<Tharsis_5_7 FROM Tharsis_4_7>")
        }
        .expect("6 MC")
  }

  @Test
  internal fun `Lakefront Resorts increases Nomads ocean bonuses`() {
    newGame(PromoCardPack, TurmoilCardPack)
    p1.runOperation("$LakefrontResorts")
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_4_6>") }
    p1.runOperation("OceanTile<Tharsis_4_8>")
    p1.runOperation("-54 MC")
    admin.phase("Action")

    p1.cardAction1(MarsNomads) {
      doTask("NomadsMarker<Tharsis_4_7 FROM Tharsis_4_6>")
    }

    p1.assertCounts(3 to "MC")
  }

  @Test
  internal fun `Nomads collect metal without triggering Mining Guild`() {
    newGame(PromoCardPack)
    p1.runOperation("$MiningGuild")
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_2_1>") }
    p1.runOperation("-5 Steel")
    admin.phase("Action")

    p1.cardAction1(MarsNomads) {
      doTask("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>")
    }

    p1.assertCounts(2 to "Steel", 1 to "PROD[Steel]")
  }

  @Test
  internal fun `Action is unavailable when every adjacent area is occupied or reserved`() {
    newGame(PromoCardPack)
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_1_1>") }
    p1.runOperation("CityTile<Tharsis_2_1>")
    p1.runOperation("GreeneryTile<Tharsis_2_2>")
    admin.phase("Action")

    shouldThrowAny { p1.cardAction1(MarsNomads) }
  }
}
