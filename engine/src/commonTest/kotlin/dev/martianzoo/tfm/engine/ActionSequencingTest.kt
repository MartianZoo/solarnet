package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActionSequencingTest {
  @Test
  fun `city standard project creates independent production and placement tasks after payment`() {
    val game = setUpGame("BM", 2)
    val p1 = game.tfm(PLAYER1)
    p1.godMode().sneak("25 Megacredit")
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual.beginManual("UseAction1<CitySP>")
    val payment =
        game.tasks.extract { it }.single { it.instruction.toString().startsWith("-25 Megacredit") }
    withClue(payment) {
      payment.then.toString().contains("Production<") shouldBe true
      payment.then.toString().contains("CityTile<") shouldBe true
    }

    manual.doTask(payment.id)

    val results =
        game.tasks
            .extract { it }
            .filter {
              it.instruction.toString().startsWith("Production<") ||
                  it.instruction.toString().startsWith("CityTile<")
            }
    results.shouldHaveSize(2)
    results.count { it.instruction.toString().startsWith("Production<") } shouldBe 1
    results.count { it.instruction.toString().startsWith("CityTile<") } shouldBe 1
    results.none { it.then != null } shouldBe true
  }
}
