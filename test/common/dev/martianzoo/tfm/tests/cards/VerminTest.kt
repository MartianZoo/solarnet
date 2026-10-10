package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VerminTest : CardTest(::attributionProbeDeclarations) {
  @Test
  internal fun `City placement adds an animal`() {
    newGame(PromoCardPack)
    p1.runOperation("$Vermin")

    requireP2().runOperation("CityTile<Tharsis_2_1>").expect("Animal<Player1, $Vermin<Player1>>")
  }

  @Test
  internal fun `Action can add a microbe to another card`() {
    newGame(PromoCardPack)
    p1.runOperation("$Vermin, $Decomposers")
    admin.phase("Action")

    p1.cardAction1(Vermin) { addCardResources(Decomposers) }.expect("Microbe<$Decomposers>")
  }

  @Test
  internal fun `Ten animals make every player lose one point per owned city`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.testTfm(PLAYER3)
    p1.runOperation("$Vermin, 10 Animal<$Vermin>, CityTile<Tharsis_2_1>, CityTile<Tharsis_2_3>")
    p2.runOperation("CityTile<Tharsis_3_2>")
    p3.runOperation("CityTile<Tharsis_3_3>")

    admin.runOperation("End FROM Phase")

    p1.assertCounts(18 to "VictoryPoint")
    p2.assertCounts(19 to "VictoryPoint")
    p3.assertCounts(19 to "VictoryPoint")
  }

  @Test
  internal fun `Admin is credited for every point removed by Vermin`() {
    newGame(PromoCardPack, players = 3)
    val p3 = game.testTfm(PLAYER3)
    p1.runOperation("$Vermin, 10 Animal<$Vermin>, CityTile<Tharsis_2_1>, $attributionProbe")
    p3.runOperation("CityTile<Tharsis_3_3>")

    admin.runOperation("End FROM Phase")

    // The probe reacts to each point loss and records the executing actor.
    admin.count("$attribution<Admin>") shouldBe 2
    admin.count("$attribution<Player1>") shouldBe 0
    admin.count("$attribution<Player3>") shouldBe 0
  }
}
