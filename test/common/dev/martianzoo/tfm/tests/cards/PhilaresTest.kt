package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
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
              *standardProjectTasks("GreeneryProject"),
              "23 Pay<> FROM MC",
              "GreeneryTile<Tharsis_3_2>",
          )
          shouldThrow<TaskException> { kim.selectTask("StandardResource") }
          shouldThrow<TaskException> { kim.doTask("Titanium") }
          shouldThrow<TaskException> { selectTask("Titanium<Kim>") }
          shouldThrow<TaskException> { doTask("Titanium<Kim>") }
          // Stan chooses the timing; Kim alone chooses and takes the resource.
          doTask("NOW Kim { StandardResource<Kim>! }")
          shouldThrow<TaskException> { doTask("OxygenStep") }
          shouldThrow<TaskException> { narrowTask("Titanium<Kim>") }
          kim.narrowTask("Titanium")
          kim.count("Titanium") shouldBe 0
          shouldThrow<TaskException> { doTask("Titanium<Kim>") }
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
          *standardProjectTasks("GreeneryProject"),
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
              *standardProjectTasks("GreeneryProject"),
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
              *standardProjectTasks("GreeneryProject"),
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
              *playCardTasks("KaguyaTech"),
              "10 Pay<> FROM MC",
              "KaguyaTech FROM ProjectCard",
              "PROD[2 MC]",
          )
          doTask("ProjectCard<Stan, Hand>")
          doTask("CityTile<Tharsis_4_3> FROM GreeneryTile<Tharsis_4_3>")
          // The active player chooses when to hand this task to its owner.
          doTask("NOW Kim { StandardResource<Kim>! }")
          kim.doTask("Titanium")
          doTask("Plant")
        }
        .expect("Titanium<Kim>, 0 Community<Stan>")
  }
}
