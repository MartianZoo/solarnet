package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.EstablishedMethods
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
          shouldThrow<TaskException> { doTask("UseAction<PlayCardFromHandAction, Action1>") }
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
