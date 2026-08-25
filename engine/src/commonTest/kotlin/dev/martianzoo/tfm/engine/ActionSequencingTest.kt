package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActionSequencingTest {
  @Test
  internal fun `invoice settlement unlocks only its matching action selector`() {
    listOf(
            Triple("First", "Megacredit", 9),
            Triple("Second", "Energy", 3),
            Triple("Third", "Titanium", 3),
        )
        .forEach { (selector, resource, amount) ->
          val game = setUpGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
          val p1 = game.tfm(PLAYER1)
          p1.godMode().manual("$amount $resource")
          val manual = p1.godMode().also { it.autoExecMode = NONE }

          manual.beginManual("UseAction<TradeSA, $selector>") {
            doTask("$amount Owed<Class<$resource>>")
            doTask("Invoice<TradeSA, $selector, Class<$resource>>")
            doTask("$amount Pay<Class<$resource>> FROM $resource")

            val tradeTasks =
                game.tasks.extract { it.instruction.toString() }.filter { it.startsWith("Trade") }
            tradeTasks.shouldHaveSize(1)
            abort()
          }
        }
  }

  @Test
  internal fun `invoice settlement belongs to the action provider's owner`() {
    val game = setUpGame()
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    p1.godMode().manual("$Steelworks, 4 Energy")
    game.tfm(ENGINE).phase("Action")

    p1.godMode().manual("UseAction<$Steelworks, First>") {
      p1.pay(energy = 4)
    }

    p1.count("Steel") shouldBe 2
    p2.count("Steel") shouldBe 0
  }

  @Test
  internal fun `city standard project creates independent production and placement tasks after payment`() {
    val game = setUpGame()
    val p1 = game.tfm(PLAYER1)
    p1.godMode().manual("25 Megacredit")
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual.beginManual("UseAction<CitySP, First>")
    manual.doTask("Owed<> / CitySP.cost")
    p1.count("Owed<>") shouldBe 25
    game.tasks.extract { it }.none { it.instruction.toString().startsWith("Production<") } shouldBe
        true
    game.tasks.extract { it }.none { it.instruction.toString().startsWith("CityTile<") } shouldBe
        true

    manual.doTask("Invoice<CitySP, First>")
    p1.count("Invoice<CitySP, First>") shouldBe 1
    game.tasks.extract { it }.none { it.instruction.toString().startsWith("Production<") } shouldBe
        true
    game.tasks.extract { it }.none { it.instruction.toString().startsWith("CityTile<") } shouldBe
        true

    manual.doTask("25 Pay<Class<Megacredit>> FROM Megacredit")
    p1.count("Owed<>") shouldBe 0
    p1.count("Invoice<CitySP, First>") shouldBe 0

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
  internal fun `card purchase waits for its complete adjusted debt to be paid`() {
    val game = setUpGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    val p1 = game.tfm(PLAYER1)
    p1.godMode().manual("$Polyphemos, 5 Megacredit")
    val manual = p1.godMode()

    manual.beginManual("Selecting THEN ProjectCard<Selecting> THEN BuySelectedCards")

    p1.count("Owed<>") shouldBe 5
    p1.count("ProjectCard<Hand>") shouldBe 0

    manual.doTask("5 Pay<Class<Megacredit>> FROM Megacredit")

    p1.count("ProjectCard<Hand>") shouldBe 1
  }

  @Test
  internal fun `use-card action rejects a different card after placing the marker`() {
    val game = setUpGame()
    val manual = game.tfm(PLAYER1).godMode().also { it.autoExecMode = NONE }
    manual.manual("$SymbioticFungus, $Ants")

    manual.beginManual("UseAction<UseCardActionSA, First>") {
      doTask("ActionUsedMarker<$SymbioticFungus>")
      shouldThrow<TaskException> { doTask("UseAction<$Ants>") }
      abort()
    }
  }
}
