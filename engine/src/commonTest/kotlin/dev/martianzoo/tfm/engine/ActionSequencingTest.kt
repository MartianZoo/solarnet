package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActionSequencingTest {
  @Test
  fun `city standard project creates independent production and placement tasks after payment`() {
    val game = setUpGame()
    val p1 = game.tfm(PLAYER1)
    p1.godMode().manual("25 Megacredit")
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual.beginManual("UseAction1<CitySP>")
    p1.count("Owed<Class<Megacredit>>") shouldBe 25
    game.tasks.extract { it }.none { it.instruction.toString().startsWith("Production<") } shouldBe
        true
    game.tasks.extract { it }.none { it.instruction.toString().startsWith("CityTile<") } shouldBe
        true

    manual.doTask("Payment<Class<CitySP>>")
    p1.count("Payment<Class<CitySP>>") shouldBe 1
    game.tasks.extract { it }.none { it.instruction.toString().startsWith("Production<") } shouldBe
        true
    game.tasks.extract { it }.none { it.instruction.toString().startsWith("CityTile<") } shouldBe
        true

    manual.doTask("25 Pay<Class<Megacredit>> FROM Megacredit")
    p1.count("Owed<Class<Megacredit>>") shouldBe 0
    manual.doTask("CostPaid<Class<CitySP>> FROM Payment<Class<CitySP>>")

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

  @Test
  fun `use-card action rejects a different card after placing the marker`() {
    val game = setUpGame()
    val manual = game.tfm(PLAYER1).godMode().also { it.autoExecMode = NONE }
    manual.manual("$SymbioticFungus, $Ants")

    manual.beginManual("UseAction1<UseCardActionSA>") {
      doTask("ActionUsedMarker<$SymbioticFungus>")
      shouldThrow<TaskException> { doTask("UseAction<$Ants>") }
      abort()
    }
  }
}
