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
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActionSequencingTest {
  @Test
  internal fun `billing settlement belongs to the action provider's owner`() {
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
