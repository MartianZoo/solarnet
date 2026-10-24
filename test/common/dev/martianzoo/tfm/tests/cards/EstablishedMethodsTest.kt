package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EstablishedMethodsTest : TfmSandboxTest() {
  @Test
  internal fun `Established Methods grants 30 MC and requires two paid standard projects`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<EstablishedMethods>>",
          "EstablishedMethods FROM PreludeCard",
          "30 MC",
      )

      kim.count("MC") shouldBe 30
      shouldThrow<NarrowingException> { doTask("UseAction<SellPatentsProject, Action1>") }
      shouldThrow<TaskException> { doTasks(*playCardTasks("Mine")) }
      doTask("UseAction<PowerPlantProject, Action1>")
      doTasks("11 Pay<> FROM MC", "PROD[Energy]")

      shouldThrow<TaskException> { doTask("Ok") }
      doTask("UseAction<PowerPlantProject, Action1>")
      doTasks("11 Pay<> FROM MC", "PROD[Energy]")
    }

    kim.assertCounts(10 to "ProjectCard", 1 to "$EstablishedMethods")
  }

  // Unresolved adjudication (question 10, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3334230/article/44569176#44569176
  @Test
  internal fun `First project cannot leave the mandatory second project unaffordable`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(2, "PreludeCard")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<EstablishedMethods>>",
            "EstablishedMethods FROM PreludeCard",
        )
        doTask("30 MC")
        doTasks("UseAction<CityProject, Action1>", "25 Pay<> FROM MC")
        doTask("PROD[MC]")
        doTask("CityTile<Tharsis_3_3>")

        doTask("UseAction<PowerPlantProject, Action1>")
        doTask("11 Pay<> FROM MC")
      }
    }
    kim.assertCounts(0 to "$EstablishedMethods", 0 to "CityTile")
    kim.count("PreludeCard") shouldBe 2
    kim.count("MC") shouldBe 0
  }

  // Unresolved adjudication (question 10, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3334230/article/44569176#44569176
  @Test
  internal fun `First project benefit can fund the mandatory second project`() {
    newTestGame(addOptions = "PreludeExpansion", kimCorporation = CrediCor)
    stan.exMachina("OceanTile<Tharsis_3_2>")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<EstablishedMethods>>",
          "EstablishedMethods FROM PreludeCard",
          "30 MC",
          "UseAction<CityProject, Action1>",
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_3_3>",
          "2 MC",
          "4 MC",
          "UseAction<PowerPlantProject, Action1>",
          "11 Pay<> FROM MC",
          "PROD[Energy]",
      )
    }
    kim.count("MC") shouldBe 0
  }

  @Test
  internal fun `Established Methods can supplement its grant to buy different projects`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.setToExMachina(6, "MC")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<EstablishedMethods>>",
          "EstablishedMethods FROM PreludeCard",
          "30 MC",
          "UseAction<PowerPlantProject, Action1>",
          "11 Pay<> FROM MC",
          "PROD[Energy]",
          "UseAction<CityProject, Action1>",
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_3_5>",
      )
    }

    kim.count("MC") shouldBe 0
  }
}
