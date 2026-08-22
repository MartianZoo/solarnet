package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.AstroDrill
import dev.martianzoo.tfm.engine.cardnames.KuiperCooperative
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class KuiperCooperativeTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PromoCardPack)
    p1.playCorp(KuiperCooperative, 0)
    engine.phase("Action")
  }

  @Test
  fun `Starts with money and titanium production`() {
    p1.assertCounts(33 to "Megacredit", 1 to "PROD[Titanium]")
  }

  @Test
  fun `Action adds one asteroid per space tag`() {
    p1.cardAction1(KuiperCooperative).expect("2 Asteroid<$KuiperCooperative>")
  }

  @Test
  fun `Asteroids can help pay for an asteroid standard project`() {
    p1.cardAction1(KuiperCooperative)

    p1.stdProject(
            "AsteroidSP",
            payment = { payWithKuiperAsteroids(this) },
        )
        .expect("-2 Asteroid<$KuiperCooperative>, -12 Megacredit, TemperatureStep")
  }

  @Test
  fun `Asteroids can help pay for an aquifer standard project`() {
    p1.cardAction1(KuiperCooperative)

    p1.stdProject("AquiferSP", payment = { payWithKuiperAsteroids(this) }) {
          doTask("OceanTile<Tharsis_1_2>")
        }
        .expect("-2 Asteroid<$KuiperCooperative>, -16 Megacredit, OceanTile, TerraformRating")
  }

  @Test
  fun `Asteroids cannot pay for another standard project`() {
    p1.cardAction1(KuiperCooperative)

    shouldThrow<NarrowingException> {
      p1.stdProject("PowerPlantSP", payment = { payWithKuiperAsteroids(this) })
    }
  }

  @Test
  fun `An asteroid on another card cannot make a Kuiper payment`() {
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
    body.doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<>")
  }
}
