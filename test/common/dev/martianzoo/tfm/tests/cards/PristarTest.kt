package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PristarTest : TfmGameplayTest() {
  @Test
  internal fun `Pays its production bonus when TR did not increase`() {
    newTestGame(playerCount = 2, kimCorporation = Pristar)
    kim.pass()
    val money = kim.count("MC")
    val income = kim.count("TerraformRating") + kim.production(cn("MC")) + 6

    stan.pass()

    admin.count("VenusSolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 1
    kim.count("MC") shouldBe money + income
  }

  @Test
  internal fun `An opponent's TR increase does not suppress the production bonus`() {
    newTestGame(playerCount = 2, kimCorporation = Pristar)
    kim.pass()
    stan.stdProject("AsteroidProject")

    stan.pass()

    admin.count("VenusSolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 1
  }

  @Test
  internal fun `Does not pay its production bonus after a TR increase`() {
    newTestGame(playerCount = 2, kimCorporation = Pristar)
    kim.turn { stdProject("AsteroidProject") }
    stan.pass()

    kim.pass()

    admin.count("VenusSolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 0
  }
}
