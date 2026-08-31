package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PhilaresTest : CardTest() {
  @Test
  internal fun `Pays its owner when an opponent places an adjacent greenery`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.manual("$Philares, GreeneryTile<Tharsis_3_2>")
    p1.manual("23 MC")
    engine.phase("Action")

    p1.stdProject("GreenerySP") {
      placeTile(4, 3)
      p2.doTask("Titanium").expect("Titanium")
    }
  }

  @Test
  internal fun `Pays its owner when an opponent creates an adjacency`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.manual("$Philares")
    p2.manual("CityTile<Tharsis_2_3>")
    p1.manual("CityTile<Tharsis_3_3>") { p2.doTask("Steel") }.expect("Steel<Player2>")
  }

  @Test
  internal fun `Pays its owner for creating adjacency to an opponent's tile`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$Philares")
    p2.manual("CityTile<Tharsis_2_3>")

    p1.manual("CityTile<Tharsis_3_3>") { p1.doTask("Titanium") }.expect("Titanium")
  }

  @Test
  internal fun `Does not pay when an opponent joins two of their own tiles`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.manual("$Philares")
    p1.manual("CityTile<Tharsis_2_3>")

    p1.manual("CityTile<Tharsis_3_3>").expect("0 Steel<Player2>, 0 Titanium<Player2>")
  }

  @Test
  internal fun `Does not pay its owner for adjacency to their own tile`() {
    newGame(PromoCardPack)
    p1.manual("$Philares")
    p1.manual("23 MC")
    engine.phase("Action")
    p1.stdAction("HandleMandates") { placeTile(4, 2) }
    p1.stdProject("GreenerySP") { placeTile(3, 2) }.expect("0 Steel, 0 Titanium")
  }

  @Test
  internal fun `The active player orders a Philares reward before its owner chooses it`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.manual("$Philares")
    p2.manual("CityTile<Tharsis_2_3>")
    val manual = p1.also { it.autoExecMode = NONE }

    manual.beginManual("CityTile<Tharsis_3_3>") {
      val reward = tasks.ids().single()
      manual.addTasks("Plant?")

      shouldThrow<TaskException> { p2.doTask("Steel") }
      manual.doTask("Plant")
      manual.selectTask(reward)
      p2.doTask("Steel")
    }

    p1.count("Plant") shouldBe 1
    p2.count("Steel") shouldBe 1
  }

  @Test
  internal fun `A delegated Philares reward prevents the active player from continuing`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.manual("$Philares")
    p2.manual("CityTile<Tharsis_2_3>")
    val manual = p1.also { it.autoExecMode = NONE }

    manual.beginManual("CityTile<Tharsis_3_3>") {
      val reward = tasks.ids().single()
      manual.addTasks("Heat?")
      manual.selectTask(reward)

      shouldThrow<TaskException> { manual.doTask("Heat") }
      p2.doTask("Titanium")
      manual.doTask("Heat")
    }

    p1.count("Heat") shouldBe 1
    p2.count("Titanium") shouldBe 1
  }
}
