package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.TfmTest
import dev.martianzoo.tfm.tests.canonicalCatalog
import dev.martianzoo.tfm.tests.canonicalPremise
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.setUpGame
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VerminTest : TfmSandboxTest() {
  @Test
  internal fun `Another player's city placement adds an animal`() {
    newTestGame()
    kim.exMachina("$Vermin")

    stan.stdProject("CityProject") { placeTile(2, 1) }.expect("Animal<$Vermin<Kim>>")
  }

  @Test
  internal fun `Its action can add a microbe to another card`() {
    newTestGame()
    kim.exMachina("$Vermin, $Decomposers")

    kim.cardAction1(Vermin) { addCardResources(Decomposers) }.expect("Microbe<$Decomposers>")
  }

  @Test
  internal fun `Ten animals cost every player a point per owned city`() {
    newTestGame()
    kim.exMachina(
        "$Vermin, 10 Animal<$Vermin>, NormalCityTile<Tharsis_2_1>, NormalCityTile<Tharsis_2_3>"
    )
    stan.exMachina("NormalCityTile<Tharsis_3_2>")
    rob.exMachina("NormalCityTile<Tharsis_3_3>")

    victoryPoints() shouldBe listOf(18, 19, 19)
  }

  // This synthetic listener tests engine attribution, rather than Vermin's player-facing result.
  internal class Attribution : TfmTest() {
    @Test
    internal fun `Admin is credited for every point removed by Vermin`() {
      game =
          setUpGame(
              canonicalPremise(
                  PromoCardPack,
                  players = 3,
                  catalog = canonicalCatalog(false),
                  additionalClassDeclarations = attributionProbeDeclarations(3),
              )
          )
      val kim = game.testTfm(PLAYER1)
      val p3 = game.testTfm(PLAYER3)
      kim.runOperation("$Vermin, 10 Animal<$Vermin>, CityTile<Tharsis_2_1>, $attributionProbe")
      p3.runOperation("CityTile<Tharsis_3_3>")

      admin.runOperation("End FROM Phase")

      // The probe reacts to each point loss and records the executing actor.
      admin.count("$attribution<Admin>") shouldBe 2
      admin.count("$attribution<Player1>") shouldBe 0
      admin.count("$attribution<Player3>") shouldBe 0
    }
  }
}
