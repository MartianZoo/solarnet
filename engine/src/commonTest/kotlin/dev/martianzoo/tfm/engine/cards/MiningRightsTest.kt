package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestOption.TerraCimmeriaMapOption
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MiningRightsTest : CardTest() {
  @Test
  fun `Links production to its prior area choice without prioritizing it`() {
    newGame()
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual
        .manual("$MiningRights") {
          shouldThrow<TaskException> { doTask("PROD[Steel]") }
          doTask("MiningRights_SpecialTile<Tharsis_1_1>")
          doTask("2 Steel")
          doTask("PROD[Steel]")
        }
        .expect("2 Steel, PROD[Steel]")
    p1.count("PROD[Titanium]") shouldBe 0
  }

  @Test
  fun `Robotic Workforce re-evaluates its production box instead of remembering steel`() {
    // https://boardgamegeek.com/thread/2663453/rule-opinions-mining-rights-robotic-workforce
    newGame(TerraCimmeriaMapOption)

    p1.manual("$MiningRights") {
          doTask("MiningRights_SpecialTile<Cimmeria_6_4>")
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
  fun `Cannot select a card-bonus area`() {
    newGame()
    shouldThrow<NotNowException> {
      p1.manual("$MiningRights") { doTask("MiningRights_SpecialTile<Tharsis_2_1>") }
    }
  }
}
