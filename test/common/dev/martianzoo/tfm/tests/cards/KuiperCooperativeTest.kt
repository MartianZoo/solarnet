package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.Agent.OperationBody
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.AstroDrill
import dev.martianzoo.tfm.tests.cards.cardnames.KuiperCooperative
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class KuiperCooperativeTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PromoCardPack)
    p1.playCorp(KuiperCooperative, 0)
    engine.phase("Action")
  }

  @Test
  internal fun `Starts with money and titanium production`() {
    p1.assertCounts(33 to "MC", 1 to "PROD[Titanium]")
  }

  @Test
  internal fun `Action adds one asteroid per space tag`() {
    p1.cardAction1(KuiperCooperative).expect("2 Asteroid<$KuiperCooperative>")
  }

  @Test
  internal fun `Asteroids can help pay for an asteroid standard project`() {
    p1.cardAction1(KuiperCooperative)

    p1.stdProject(
            "AsteroidSP",
            payment = { payWithKuiperAsteroids(this) },
        )
        .expect("-2 Asteroid<$KuiperCooperative>, -12 MC, TemperatureStep")
  }

  @Test
  internal fun `Asteroids can help pay for an aquifer standard project`() {
    p1.cardAction1(KuiperCooperative)

    p1.stdProject("AquiferSP", payment = { payWithKuiperAsteroids(this) }) {
          placeTile(1, 2)
        }
        .expect("-2 Asteroid<$KuiperCooperative>, -16 MC, OceanTile, TerraformRating")
  }

  @Test
  internal fun `Asteroids cannot pay for another standard project`() {
    p1.cardAction1(KuiperCooperative)

    shouldThrow<NarrowingException> {
      p1.stdProject("PowerPlantSP", payment = { payWithKuiperAsteroids(this) })
    }
  }

  @Test
  internal fun `An asteroid on another card cannot make a Kuiper payment`() {
    p1.manual("$AstroDrill, Asteroid<$AstroDrill>")

    shouldThrow<TaskException> {
      p1.stdProject(
          "AsteroidSP",
          payment = {
            doTask("PayFromCard<$KuiperCooperative> FROM Asteroid<$AstroDrill>")
          },
      )
    }
  }

  private fun payWithKuiperAsteroids(body: OperationBody) {
    body.doTask("2 PayFromCard<$KuiperCooperative> FROM Asteroid<$KuiperCooperative>")
    body.doTask("Pay<Class<MC>> FROM MC / Owed<>")
  }
}
