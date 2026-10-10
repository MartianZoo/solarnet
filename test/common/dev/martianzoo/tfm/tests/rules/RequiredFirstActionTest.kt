package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RequiredFirstActionTest : TfmGameplayTest() {
  @Test
  internal fun `Can pass the first generation and draw three cards in the second`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)
    passFirstGeneration()

    kim.stdAction("RequiredActionsSignal").expect("3 ProjectCard")
  }

  @Test
  internal fun `Can play a project after resolving its mandatory card draw`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)
    passFirstGeneration()
    kim.stdAction("RequiredActionsSignal")

    kim.playProject(Mine, 4).expect("PROD[Steel]")
  }

  @Test
  internal fun `Cannot play a project before resolving its mandatory card draw`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)

    shouldThrow<NarrowingException> { kim.playProject(Mine, 4) }
    kim.count("$Mine") shouldBe 0
    kim.count("RequiredAction") shouldBe 1
  }

  @Test
  internal fun `Cannot buy a standard project before resolving its mandatory card draw`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)
    passFirstGeneration()

    shouldThrow<NarrowingException> { kim.stdProject("AsteroidProject") }
    admin.count("TemperatureStep") shouldBe 0
    kim.count("RequiredAction") shouldBe 1
  }

  @Test
  internal fun `Cannot convert available heat before resolving its mandatory card draw`() {
    newTestGame(addOptions = "PreludeExpansion", playerCount = 2, kimCorporation = Inventrix)
    with(kim) { playPrelude(ExcentricSponsor) { playProject(Soletta, 10) } }
    kim.playPrelude(Donation)
    stan.playPrelude(Biolab)
    stan.playPrelude(SupplyDrop)
    passFirstGeneration()
    kim.count("Heat") shouldBe 8

    shouldThrow<NarrowingException> { kim.convertHeat() }
    kim.count("Heat") shouldBe 8
    kim.count("RequiredAction") shouldBe 1
  }

  private fun passFirstGeneration() {
    kim.pass()
    stan.pass()
    kim.wgt("OxygenStep")
    kim.buyCards(0)
    stan.buyCards(0)
    stan.pass()
  }
}
