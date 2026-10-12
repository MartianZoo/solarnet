package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
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
      selectTask("UseAction<Kim, StandardProject(HAS cost)>!")
      kim.count("MC") shouldBe 30
      shouldThrow<NarrowingException> { doTask("UseAction<SellPatentsProject, Action1>") }
      shouldThrow<NarrowingException> { doTask("PlayProject<Class<Mine>>") }
      doTask("UseAction<PowerPlantProject, Action1>")
      doTasks("-11 MC", "PROD[Energy]")
      selectTask("UseAction<Kim, StandardProject(HAS cost)>!")
      shouldThrow<NarrowingException> { doTask("Ok") }
      doTask("UseAction<PowerPlantProject, Action1>")
      doTasks("-11 MC", "PROD[Energy]")
    }

    kim.assertCounts(10 to "ProjectCard", 1 to "$EstablishedMethods")
  }

  @Test
  internal fun `First project cannot leave the mandatory second project unaffordable`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.setToExMachina(0, "MC")
    val preludesBefore = kim.count("PreludeCard")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<EstablishedMethods>>",
            "EstablishedMethods FROM PreludeCard",
        )
        selectTask("30 MC!")
        doTasks("UseAction<CityProject, Action1>", "-25 MC")
        selectTask("PROD[MC]")
        doTask("CityTile<Tharsis_3_3>")
        selectTask("UseAction<Kim, StandardProject(HAS cost)>!")
        doTask("UseAction<PowerPlantProject, Action1>")
        selectTask("-X MC?")
        doTask("-11 MC")
      }
    }
    kim.assertCounts(0 to "$EstablishedMethods", 0 to "CityTile")
    kim.count("PreludeCard") shouldBe preludesBefore
    kim.count("MC") shouldBe 0
  }

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
          "4 MC",
          "-25 MC",
          "PROD[MC]",
          "CityTile<Tharsis_3_3>",
          "2 MC",
          "UseAction<PowerPlantProject, Action1>",
          "-11 MC",
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
          "-11 MC",
          "PROD[Energy]",
          "UseAction<CityProject, Action1>",
          "-25 MC",
          "PROD[MC]",
          "CityTile<Tharsis_3_5>",
      )
    }

    kim.count("MC") shouldBe 0
  }
}
