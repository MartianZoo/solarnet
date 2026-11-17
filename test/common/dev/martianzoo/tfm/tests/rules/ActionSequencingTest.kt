package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActionSequencingTest : TfmSandboxTest() {

  @Test
  internal fun `use-card action rejects a different card after Admin places the marker`() {
    game = setUpGame()
    val manual = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    manual.runOperation("$SymbioticFungus, $Ants")

    manual.beginOperation("UseAction<StandardAction>") {
      doTasks(*cardActionTasks("$SymbioticFungus"))
      manual.count("ActionUsedMarker<$SymbioticFungus>") shouldBe 1
      shouldThrow<TaskException> { doTask("UseAction<$Ants>") }
      abort()
    }
  }
}
