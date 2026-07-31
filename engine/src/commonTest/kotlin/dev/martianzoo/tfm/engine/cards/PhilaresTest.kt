package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.AutoExecMode.NONE
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PhilaresTest : CardTest() {
  @Test
  fun `with Philares owned by p2, p1 places an adjacent greenery`() {
    newGame("TerraformingMars,TharsisMap,PromoCardPack")
    val p2 = requireP2()
    p2.manual("Philares, GreeneryTile<Tharsis_3_2>")
    p1.manual("23")
    engine.phase("Action")

    p1.stdProject("GreenerySP") {
      doTask("GreeneryTile<Tharsis_4_3>")
      game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
      p2.doTask("Titanium").expect("Titanium")
    }
  }

  @Test
  fun `with Philares owned by p2, p1 creates adjacency`() {
    newGame("TerraformingMars,TharsisMap,PromoCardPack")
    val p2 = requireP2()
    p2.manual("Philares")
    p1.autoExecMode = NONE
    p2.autoExecMode = NONE
    p2.manual("CityTile<Tharsis_2_3>")
    val checkpoint = game.timeline.checkpoint()

    p1.godMode().beginManual("CityTile<Tharsis_3_3>") {
      game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    }

    game.events.changesSince(checkpoint).first().actor shouldBe PLAYER1
    p2.doTask("Steel").expect("Steel<Player2>")
    game.events.changesSince(checkpoint).last().actor shouldBe PLAYER2
  }

  @Test
  fun `with Philares owned by p1 and a p2 tile, p1 creates adjacency`() {
    newGame("TerraformingMars,TharsisMap,PromoCardPack")
    val p2 = requireP2()
    p1.manual("Philares")
    p1.autoExecMode = NONE
    p2.autoExecMode = NONE
    p2.manual("CityTile<Tharsis_2_3>")

    p1.godMode().beginManual("CityTile<Tharsis_3_3>") {
      game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER1)
    }

    p1.doTask("Titanium").expect("Titanium")
  }

  @Test
  fun `with Philares owned by p2, p1 joins two p1 tiles`() {
    newGame("TerraformingMars,TharsisMap,PromoCardPack")
    val p2 = requireP2()
    p2.manual("Philares")
    p1.autoExecMode = NONE
    p2.autoExecMode = NONE
    p1.manual("CityTile<Tharsis_2_3>")

    p1.manual("CityTile<Tharsis_3_3>")

    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `with Philares and an own tile, p1 places an adjacent greenery`() {
    newGame("TerraformingMars,TharsisMap,PromoCardPack")
    p1.manual("Philares")
    p1.manual("-Mandate, GreeneryTile<Tharsis_4_2>, 23")
    engine.phase("Action")
    p1.stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_3_2>") }
    game.tasks.isEmpty() shouldBe true
  }
}
