package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.data.Player.Companion.PLAYER3
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class VerminTest : CardTest() {
  @Test
  internal fun `City placement and both card actions add the appropriate resources`() {
    newGame(PromoCardPack)
    p1.manual("$Vermin, $Decomposers")
    engine.phase("Action")

    requireP2().manual("CityTile<Tharsis_2_1>").expect("Animal<Player1, $Vermin<Player1>>")
    p1.cardAction1(Vermin) { addCardResources(Vermin) }
    engine.manual("Generation")
    p1.cardAction1(Vermin) { addCardResources(Decomposers) }

    p1.assertCounts(2 to "Animal<$Vermin>", 2 to "Microbe<$Decomposers>")
  }

  @Test
  internal fun `Ten animals make every player lose one point per owned city`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p1.manual("$Vermin, 10 Animal<$Vermin>, CityTile<Tharsis_2_1>, CityTile<Tharsis_2_3>")
    p2.manual("CityTile<Tharsis_3_2>")
    p3.manual("CityTile<Tharsis_3_3>")

    engine.phase("End")

    p1.assertCounts(18 to "VictoryPoint")
    p2.assertCounts(19 to "VictoryPoint")
    p3.assertCounts(19 to "VictoryPoint")
  }

  @Test
  internal fun `Fewer than ten animals do not impose the city penalty`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$Vermin, 7 Animal<$Vermin>, CityTile<Tharsis_2_1>")
    p2.manual("CityTile<Tharsis_3_2>")

    engine.phase("End")

    p1.assertCounts(20 to "VictoryPoint")
    p2.assertCounts(20 to "VictoryPoint")
  }
}
