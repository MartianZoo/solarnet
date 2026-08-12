package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon.Option.PromoCardPack
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import kotlin.test.Test

class VerminTest : CardTest() {
  @Test
  fun `city placement and both card actions add the appropriate resources`() {
    newGame(PromoCardPack)
    p1.manual("Vermin, Decomposers")
    engine.phase("Action")

    requireP2().manual("CityTile<Tharsis_2_1>").expect("Animal<Vermin>")
    p1.cardAction1("Vermin") { doTask("Animal<Vermin>!") }
    engine.manual("Generation")
    p1.cardAction1("Vermin") { doTask("Microbe<Decomposers>") }

    p1.assertCounts(2 to "Animal<Vermin>", 2 to "Microbe<Decomposers>")
  }

  @Test
  fun `ten animals make every player lose one point per owned city`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("Vermin, 10 Animal<Vermin>, CityTile<Tharsis_2_1>, CityTile<Tharsis_2_3>")
    p2.manual("CityTile<Tharsis_3_2>")

    engine.phase("End")

    p1.assertCounts(18 to "VictoryPoint")
    p2.assertCounts(19 to "VictoryPoint")
  }

  @Test
  fun `fewer than ten animals do not impose the city penalty`() {
    newGame(PromoCardPack)
    p1.manual("Vermin, 8 Animal<Vermin>, CityTile<Tharsis_2_1>")

    engine.phase("End")

    p1.assertCounts(20 to "VictoryPoint")
  }
}
