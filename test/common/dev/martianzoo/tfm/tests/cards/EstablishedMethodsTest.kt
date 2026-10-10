package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EstablishedMethodsTest : CardTest() {
  @Test
  internal fun `Established Methods grants 30 MC and requires two paid standard projects`() {
    newGame(PreludeExpansion, PromoCardPack)
    admin.phase("Prelude")
    p1.runOperation("2 ProjectCard, PreludeCard")

    p1.playPrelude(EstablishedMethods) {
          p1.count("MC") shouldBe 30
          shouldThrow<NarrowingException> { doTask("UseAction<SellPatentsProject, Action1>") }
          shouldThrow<TaskException> { doTask("PlayProject<Class<Mine>>") }
          doTask("UseAction<PowerPlantProject, Action1>")
          p1.pay(11)
          shouldThrow<NarrowingException> { doTask("Ok") }
          doTask("UseAction<PowerPlantProject, Action1>")
          p1.pay(11)
        }
        .expect("8 MC, PROD[2 Energy]")

    p1.assertCounts(2 to "ProjectCard", 1 to "$EstablishedMethods")
  }

  @Test
  internal fun `First project cannot leave the mandatory second project unaffordable`() {
    newGame(PreludeExpansion, PromoCardPack)
    admin.phase("Prelude")
    p1.runOperation("PreludeCard")
    val preludesBefore = p1.count("PreludeCard")

    shouldThrow<LimitsException> {
      p1.playPrelude(EstablishedMethods) {
        doTask("UseAction<CityProject, Action1>")
        p1.pay(25)
        placeTile(3, 3)
        doTask("UseAction<PowerPlantProject, Action1>")
        p1.pay(11)
      }
    }
    p1.assertCounts(0 to "$EstablishedMethods", 0 to "CityTile")
    p1.count("PreludeCard") shouldBe preludesBefore
    p1.count("MC") shouldBe 0
  }

  @Test
  internal fun `First project benefit can fund the mandatory second project`() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.playCorp(CrediCor, 0)
    requireP2().runOperation("OceanTile<Tharsis_3_2>")
    p1.runOperation("-${p1.count("MC")} MC")
    admin.phase("Prelude")
    p1.runOperation("PreludeCard")

    p1.playPrelude(EstablishedMethods) {
          doTask("UseAction<CityProject, Action1>")
          p1.pay(25)
          placeTile(3, 3)
          doTask("UseAction<PowerPlantProject, Action1>")
          p1.pay(11)
        }
        .expect("PROD[MC, Energy], CityTile<Tharsis_3_3>")
    p1.count("MC") shouldBe 0
  }

  @Test
  internal fun `Established Methods can supplement its grant to buy different projects`() {
    newGame(PreludeExpansion, PromoCardPack)
    admin.phase("Prelude")
    p1.runOperation("6 MC, PreludeCard")

    p1.playPrelude(EstablishedMethods) {
          doTask("UseAction<PowerPlantProject, Action1>")
          p1.pay(11)
          doTask("UseAction<CityProject, Action1>")
          p1.pay(25)
          placeTile(3, 5)
        }
        .expect("-6 MC, PROD[Energy, MC], CityTile")

    p1.count("MC") shouldBe 0
  }
}
