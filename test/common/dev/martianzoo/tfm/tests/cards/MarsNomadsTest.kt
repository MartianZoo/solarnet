package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.Cimmeria
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.CrediCor
import dev.martianzoo.tfm.tests.cards.cardnames.LakefrontResorts
import dev.martianzoo.tfm.tests.cards.cardnames.LandClaim
import dev.martianzoo.tfm.tests.cards.cardnames.MarsNomads
import dev.martianzoo.tfm.tests.cards.cardnames.MiningGuild
import dev.martianzoo.tfm.tests.cards.cardnames.Philares
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class MarsNomadsTest : CardTest() {
  @Test
  internal fun `Play places the marker on an empty land area without collecting its bonus`() {
    newGame(PromoCardPack)

    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_1_1>") }

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
  internal fun `Action requires an adjacent destination`() {
    newGame(PromoCardPack)
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_1_1>") }
    admin.phase("Action")

    shouldThrow<NarrowingException> {
      p1.cardAction1(MarsNomads) {
        doTask("NomadsMarker<Tharsis_4_2 FROM Tharsis_1_1>")
      }
    }
  }

  @Test
  internal fun `Action cannot move the marker onto a community`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_1_1>") }
    p2.runOperation("Community<Tharsis_2_2>")
    admin.phase("Action")

    shouldThrow<NarrowingException> {
      p1.cardAction1(MarsNomads) {
        doTask("NomadsMarker<Tharsis_2_2 FROM Tharsis_1_1>")
      }
    }

    p1.assertCounts(1 to "NomadsMarker<Tharsis_1_1>")
    p2.assertCounts(1 to "Community<Tharsis_2_2>")
  }

  @Test
  internal fun `A community can reserve the marker's area while the marker still blocks tiles`() {
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
    newGame(PromoCardPack, TurmoilExpansion)
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
  internal fun `Nomads movement collects its placement bonus without triggering Mars First`() {
    newGame(PromoCardPack, TurmoilExpansion)
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_2_1>") }
    admin.runOperation("Ruling<MarsFirst> FROM Ruling")
    admin.phase("Action")

    p1.runOperation("CityTile<Tharsis_3_3>").expect("Steel")

    p1.cardAction1(MarsNomads) {
          doTask("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>")
        }
        .expect("2 Steel")
  }

  @Test
  internal fun `Nomads movement does not trigger Philares`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.runOperation("$Philares, CityTile<Tharsis_1_2>")
    p2.runOperation("-2 Steel")
    p1.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_2_1>") }
    admin.phase("Action")

    p1.cardAction1(MarsNomads) {
      doTask("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>")
    }

    shouldThrow<TaskException> { p2.doTask("Steel") }
    p2.assertCounts(0 to "Steel", 0 to "Titanium")
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

  @Test
  internal fun `Moving onto MSL Curiosity requires paying for and placing a colony`() {
    newGame(Cimmeria, ColoniesExpansion, PromoCardPack, colonyTiles = testColonyTiles(2))
    p1.playCorp(CrediCor, 1)
    admin.phase("Action")
    p1.playProject(MarsNomads, 13) { doTask("NomadsMarker<Cimmeria_3_2>") }

    p1.cardAction1(MarsNomads) {
          doTask("NomadsMarker<Cimmeria_3_3 FROM Cimmeria_3_2>")
          doTask("Colony<Luna>")
        }
        .expect("-5 MC, Colony<Luna>, PROD[2 MC], 0 OwnedTile")
  }

  @Test
  internal fun `Moving onto MSL Curiosity fails when every colony tile already has an own colony`() {
    val tiles = testColonyTiles(2)
    newGame(Cimmeria, ColoniesExpansion, PromoCardPack, colonyTiles = tiles)
    p1.playCorp(CrediCor, 1)
    admin.phase("Action")
    p1.runOperation("100 MC")
    p1.playProject(MarsNomads, 13) { doTask("NomadsMarker<Cimmeria_3_2>") }
    tiles.forEach { tile ->
      p1.stdProject("BuildColonyProject") { doTask("Colony<$tile>") }
    }
    val moneyBefore = p1.count("MC")

    shouldThrow<DependencyException> {
      p1.cardAction1(MarsNomads) {
        doTask("NomadsMarker<Cimmeria_3_3 FROM Cimmeria_3_2>")
      }
    }
    p1.assertCounts(1 to "NomadsMarker<Cimmeria_3_2>", 0 to "NomadsMarker<Cimmeria_3_3>")
    p1.count("MC") shouldBe moneyBefore
    p1.count("Colony") shouldBe 5
  }

  @Ignore // The greenery fallback still treats the marker area as available.
  @Test
  internal fun `Allows greenery elsewhere when its marker blocks the last adjacent area`() {
    plantBeyondNomads().expect("GreeneryTile<Tharsis_9_7>, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `BUG - Blocks greenery elsewhere when its marker blocks the last adjacent area`() {
    shouldThrow<NarrowingException> { plantBeyondNomads() }
    p1.assertCounts(8 to "Plant", 1 to "GreeneryTile", 0 to "GreeneryTile<Tharsis_9_7>")
    requireP2().count("NomadsMarker<Tharsis_2_2>") shouldBe 1
  }

  private fun plantBeyondNomads(): TaskResult {
    newGame(PromoCardPack)
    p1.runOperation("GreeneryTile<Tharsis_1_1>, 8 Plant")
    val p2 = requireP2()
    p2.runOperation("CityTile<Tharsis_2_1>")
    p2.runOperation("$MarsNomads") { doTask("NomadsMarker<Tharsis_2_2>") }
    admin.phase("Action")
    shouldThrow<DeadEndException> { p1.convertPlants { placeTile(2, 2) } }
    return p1.convertPlants { placeTile(9, 7) }
  }
}
