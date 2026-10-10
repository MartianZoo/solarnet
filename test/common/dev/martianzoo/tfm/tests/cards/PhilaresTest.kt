package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PhilaresTest : TfmSandboxTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame(kimCorporation = Philares)
    kim.stdAction("DoRequiredActionsAction") { placeTile(4, 2) }
  }

  @Test
  internal fun `Pays its owner when an opponent places an adjacent greenery`() {
    stan
        .stdProject("GreeneryProject") {
          placeTile(3, 2)
          kim.doTask("Titanium")
        }
        .expect("Titanium<Kim>")
  }

  @Test
  internal fun `Pays its owner for creating adjacency to an opponent's tile`() {
    stan.exMachina("NormalCityTile<Tharsis_2_2>")
    kim.setToExMachina(23, "MC")

    kim.stdProject("GreeneryProject") {
          placeTile(3, 2)
          kim.doTask("Titanium")
        }
        .expect("Titanium")
  }

  @Test
  internal fun `Does not pay when an opponent joins their own tiles`() {
    stan.exMachina("NormalCityTile<Tharsis_1_1>")

    stan
        .stdProject("GreeneryProject") { placeTile(2, 1) }
        .expect(
            "0 Steel<Kim>, 0 Titanium<Kim>, 0 MC<Kim>, 0 Plant<Kim>, 0 Energy<Kim>, 0 Heat<Kim>"
        )
  }

  @Test
  internal fun `Does not pay its owner for adjacency to their own tile`() {
    kim.setToExMachina(23, "MC")

    kim.stdProject("GreeneryProject") { placeTile(3, 2) }
        .expect("0 Steel, 0 Titanium, -23 MC, 0 Energy, 0 Heat")
  }

  @Test
  internal fun `Kaguya creates a new adjacency without renewing an Arcadian reservation`() {
    stan.exMachina("$ArcadianCommunities, GreeneryTile<Tharsis_4_3>")

    stan
        .playProject(KaguyaTech, 10) {
          doTask("CityTile<Tharsis_4_3> FROM GreeneryTile<Tharsis_4_3>")
          kim.doTask("Titanium")
        }
        .expect("-10 MC, Titanium<Kim>, 0 Community<Stan>")
  }
}
