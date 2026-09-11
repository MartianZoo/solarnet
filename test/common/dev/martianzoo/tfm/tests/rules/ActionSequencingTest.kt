package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActionSequencingTest {
  @Test
  internal fun `invoice settlement belongs to the action provider's owner`() {
    val game = setUpGame()
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    p1.runOperation("$Steelworks, 4 Energy")
    game.testTfm(ADMIN).phase("Action")

    p1.runOperation("UseAction<$Steelworks, Action1>") { p1.pay(energy = 4) }

    p1.count("Steel") shouldBe 2
    p2.count("Steel") shouldBe 0
  }

  @Test
  internal fun `card purchase waits for its complete adjusted debt to be paid`() {
    val game = setUpGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("$Polyphemos, 5 MC")
    val manual = p1

    manual.beginOperation("ProjectCard<Selecting> THEN BuySelectedCards")

    p1.count("Owed<>") shouldBe 5
    p1.count("ProjectCard<Hand>") shouldBe 0

    manual.autoExecPolicy = NONE
    manual.doTask("5 Pay<Class<MC>> FROM MC")

    p1.count("ProjectCard<Hand>") shouldBe 1
  }

  @Test
  internal fun `use-card action rejects a different card after placing the marker`() {
    val game = setUpGame()
    val manual = game.testTfm(PLAYER1).also { it.autoExecPolicy = NONE }
    manual.runOperation("$SymbioticFungus, $Ants")

    manual.beginOperation("UseAction<UseActionOnCardAction, Action1>") {
      doTask("ActionUsedMarker<$SymbioticFungus>")
      shouldThrow<TaskException> { doTask("UseAction<$Ants>") }
      abort()
    }
  }
}
