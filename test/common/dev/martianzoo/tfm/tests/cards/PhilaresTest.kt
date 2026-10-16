package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PhilaresTest : TfmSandboxTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame(kimCorporation = Philares)
    kim.inTurn {
      doTasks("DoRequiredActions", "GreeneryTile<Tharsis_4_2>")
      doTask("OxygenStep")
      doTasks("Plant", "TerraformRating")
    }
  }

  @Test
  internal fun `Pays its owner when an opponent places an adjacent greenery`() {
    stan
        .inTurn {
          doTasks(
              "UseStandardProject<GreeneryProject>",
              "23 Pay<> FROM MC",
              "GreeneryTile<Tharsis_3_2>",
          )
          kim.doTask("Titanium")
          doTask("OxygenStep")
          doTask("TerraformRating")
        }
        .expect("Titanium<Kim>")
  }

  @Test
  internal fun `Pays its owner for creating adjacency to an opponent's tile`() {
    stan.exMachina("NormalCityTile<Tharsis_2_2>")
    kim.setToExMachina(23, "MC")

    kim.inTurn {
      doTasks(
          "UseStandardProject<GreeneryProject>",
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_3_2>",
      )
      doTask("OxygenStep")
      doTask("TerraformRating")

      doTask("Titanium")
    }
  }

  @Test
  internal fun `Does not pay when an opponent joins their own tiles`() {
    stan.exMachina("NormalCityTile<Tharsis_1_1>")

    stan
        .inTurn {
          doTasks(
              "UseStandardProject<GreeneryProject>",
              "23 Pay<> FROM MC",
              "GreeneryTile<Tharsis_2_1>",
          )
          doTask("OxygenStep")
          doTask("TerraformRating")
        }
        .expect(
            "0 Steel<Kim>, 0 Titanium<Kim>, 0 MC<Kim>, 0 Plant<Kim>, 0 Energy<Kim>, 0 Heat<Kim>"
        )
  }

  @Test
  internal fun `Does not pay its owner for adjacency to their own tile`() {
    kim.setToExMachina(23, "MC")

    kim.inTurn {
          doTasks(
              "UseStandardProject<GreeneryProject>",
              "23 Pay<> FROM MC",
              "GreeneryTile<Tharsis_3_2>",
          )
          doTask("OxygenStep")
          doTask("TerraformRating")
        }
        .expect("0 Steel, 0 Titanium, 0 Energy, 0 Heat")
  }

  @Test
  internal fun `Kaguya creates a new adjacency without renewing an Arcadian reservation`() {
    stan.exMachina("$ArcadianCommunities, GreeneryTile<Tharsis_4_3>")

    stan
        .inTurn {
          doTasks(
              "PlayProject<Class<KaguyaTech>>",
              "10 Pay<> FROM MC",
              "KaguyaTech FROM ProjectCard",
              "PROD[2 MC]",
          )
          doTask("ProjectCard<Stan, Hand>")
          doTask("CityTile<Tharsis_4_3> FROM GreeneryTile<Tharsis_4_3>")
          kim.doTask("Titanium")
          doTask("Plant")
        }
        .expect("Titanium<Kim>, 0 Community<Stan>")
  }
}
