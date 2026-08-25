package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestOption.Cimmeria
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MiningRightsTest : CardTest() {
  @Test
  internal fun `Links production to its prior area choice without prioritizing it`() {
    newGame()
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual
        .manual("$MiningRights") {
          shouldThrow<TaskException> { doTask("PROD[Steel]") }
          placeTile(1, 1)
          doTask("2 Steel")
          doTask("PROD[Steel]")
        }
        .expect("2 Steel, PROD[Steel]")
    p1.count("PROD[Titanium]") shouldBe 0
  }

  @Test
  internal fun `Robotic Workforce re-evaluates its production box instead of remembering steel`() {
    // https://boardgamegeek.com/thread/2663453/rule-opinions-mining-rights-robotic-workforce
    newGame(Cimmeria)

    p1.manual("$MiningRights") {
          placeTile(6, 4)
          doTask("PROD[Steel]")
        }
        .expect("Titanium, 2 Steel, PROD[Steel]")

    val manual = p1.godMode().also { it.autoExecMode = NONE }
    manual.beginManual("$RoboticWorkforce")
    manual.reviseTask(
        "CopyProductionBox<CardFront(HAS BuildingTag OR WildTagUse(HAS BuildingTag))>",
        "CopyProductionBox<$MiningRights>",
    )
    manual.finish { doTask("PROD[Titanium]") }.expect("PROD[Titanium]")
  }

  @Test
  internal fun `Cannot select a card-bonus area`() {
    newGame()
    shouldThrow<NotNowException> { p1.manual("$MiningRights") { placeTile(2, 1) } }
  }
}
