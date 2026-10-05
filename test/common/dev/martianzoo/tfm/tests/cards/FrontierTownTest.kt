package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.CrediCor
import dev.martianzoo.tfm.tests.cards.cardnames.FrontierTown
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FrontierTownTest : CardTest() {
  @Test
  internal fun `An ocean placement reward does not consume the city's pending repeat bonus`() {
    newGame(Hellas, PreludeExpansion, Prelude2CardPack, TurmoilExpansion)
    p1.runOperation("18 MC, PROD[Energy]")

    p1.runOperation("FrontierTown") {
      doWithoutAutoExec(p1) {
        placeTile(9, 7)
        placeTile(5, 7)
      }
      placeTile(5, 8)
      placeTile(6, 7)
    }

    p1.assertCounts(1 to "CityTile", 3 to "OceanTile", 0 to "FrontierTownBonus")
  }

  @Test
  internal fun `MSL Curiosity's tripled bonus builds three colonies and costs fifteen mc`() {
    initialize()

    p1.playProject(FrontierTown, 11) {
          placeTile(3, 3)
          doTask("Colony<Luna>")
          doTask("Colony<Ceres>")
          doTask("Colony<Triton>")
        }
        .expect("CityTile<Cimmeria_3_3>, 3 Colony, -26 MC, PROD[2 MC, Steel, -Energy], 3 Titanium")
  }

  @Test
  internal fun `MSL Curiosity cannot be chosen when only two colony tiles are active`() {
    initialize("Luna", "Ceres", "Miranda", "Titan", "Enceladus")
    p1.count("ColonyTile") shouldBe 2
    val moneyBefore = p1.count("MC")

    shouldThrow<DependencyException> {
      p1.playProject(FrontierTown, 11) {
        placeTile(3, 3)
        doTask("Colony<Luna>")
        doTask("Colony<Ceres>")
      }
    }
    p1.assertCounts(
        0 to "$FrontierTown",
        0 to "CityTile<Cimmeria_3_3>",
        0 to "Colony",
        1 to "ProjectCard",
        1 to "PROD[Energy]",
    )
    p1.count("MC") shouldBe moneyBefore
    // The failed placement leaves the card playable on another space.
    p1.playProject(FrontierTown, 11) { placeTile(3, 4) }.expect("CityTile<Cimmeria_3_4>, 0 Colony")
  }

  private fun initialize(vararg colonyTiles: String) {
    newGame(
        Cimmeria,
        ColoniesExpansion,
        PreludeExpansion,
        Prelude2CardPack,
        TurmoilExpansion,
        colonyTiles = testColonyTiles(2, *colonyTiles),
    )
    p1.playCorp(CrediCor, 1)
    admin.phase("Action")
    p1.stdProject("PowerPlantProject")
    p1.runOperation("2 PartyDelegate<MarsFirst>")
  }
}
