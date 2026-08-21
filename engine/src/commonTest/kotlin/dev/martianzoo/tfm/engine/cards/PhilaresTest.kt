package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class PhilaresTest : CardTest() {
  @Test
  fun `Pays its owner when an opponent places an adjacent greenery`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.manual("$Philares, GreeneryTile<Tharsis_3_2>")
    p1.manual("23")
    engine.phase("Action")

    p1.stdProject("GreenerySP") {
      doTask("GreeneryTile<Tharsis_4_3>")
      p2.doTask("Titanium").expect("Titanium")
    }
  }

  @Test
  fun `Pays its owner when an opponent creates an adjacency`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.manual("$Philares")
    p2.manual("CityTile<Tharsis_2_3>")
    p1.manual("CityTile<Tharsis_3_3>") { p2.doTask("Steel") }.expect("Steel<Player2>")
  }

  @Test
  fun `Pays its owner for creating adjacency to an opponent's tile`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$Philares")
    p2.manual("CityTile<Tharsis_2_3>")

    p1.manual("CityTile<Tharsis_3_3>") { p1.doTask("Titanium") }.expect("Titanium")
  }

  @Test
  fun `Does not pay when an opponent joins two of their own tiles`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.manual("$Philares")
    p1.manual("CityTile<Tharsis_2_3>")

    p1.manual("CityTile<Tharsis_3_3>").expect("0 Steel<Player2>, 0 Titanium<Player2>")
  }

  @Test
  fun `Does not pay its owner for adjacency to their own tile`() {
    newGame(PromoCardPack)
    p1.manual("$Philares")
    p1.manual("23")
    engine.phase("Action")
    p1.stdAction("HandleMandates") { doTask("GreeneryTile<Tharsis_4_2>") }
    p1.stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_3_2>") }
        .expect("0 Steel, 0 Titanium")
  }
}
