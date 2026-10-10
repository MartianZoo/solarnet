package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
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

    kim.playPrelude(EstablishedMethods) {
          kim.count("MC") shouldBe 30
          shouldThrow<NarrowingException> { doTask("UseAction<SellPatentsProject, Action1>") }
          shouldThrow<TaskException> { doTask("PlayProject<Class<Mine>>") }
          doTask("UseAction<PowerPlantProject, Action1>")
          kim.pay(11)
          shouldThrow<NarrowingException> { doTask("Ok") }
          doTask("UseAction<PowerPlantProject, Action1>")
          kim.pay(11)
        }
        .expect("8 MC, PROD[2 Energy]")

    kim.assertCounts(10 to "ProjectCard", 1 to "$EstablishedMethods")
  }

  @Test
  internal fun `First project cannot leave the mandatory second project unaffordable`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.setToExMachina(0, "MC")
    val preludesBefore = kim.count("PreludeCard")

    shouldThrow<LimitsException> {
      kim.playPrelude(EstablishedMethods) {
        doTask("UseAction<CityProject, Action1>")
        kim.pay(25)
        placeTile(3, 3)
        doTask("UseAction<PowerPlantProject, Action1>")
        kim.pay(11)
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

    kim.playPrelude(EstablishedMethods) {
          doTask("UseAction<CityProject, Action1>")
          kim.pay(25)
          placeTile(3, 3)
          doTask("UseAction<PowerPlantProject, Action1>")
          kim.pay(11)
        }
        .expect("PROD[MC, Energy], CityTile<Tharsis_3_3>")
    kim.count("MC") shouldBe 0
  }

  @Test
  internal fun `Established Methods can supplement its grant to buy different projects`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.setToExMachina(6, "MC")

    kim.playPrelude(EstablishedMethods) {
          doTask("UseAction<PowerPlantProject, Action1>")
          kim.pay(11)
          doTask("UseAction<CityProject, Action1>")
          kim.pay(25)
          placeTile(3, 5)
        }
        .expect("-6 MC, PROD[Energy, MC], CityTile")

    kim.count("MC") shouldBe 0
  }
}
