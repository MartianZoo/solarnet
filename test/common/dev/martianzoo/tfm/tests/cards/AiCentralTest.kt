package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

internal class AiCentralTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Cannot be played without energy production`() {
    kim.exMachina("$SearchForLife, $InventorsGuild, $DesignedMicroorganisms")
    kim.setToExMachina(0, "PROD[Energy]")
    kim.setToExMachina(21, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks("PlayProject<Class<AiCentral>>", "21 Pay<> FROM MC")
        doTask("AiCentral FROM ProjectCard")
        doTask("PROD[-Energy]")
      }
    }
  }

  @Test
  internal fun `Cannot use its action twice in one generation`() {
    kim.exMachina("$AiCentral")
    kim.inTurn {
      doTask("UseCardAction<AiCentral, Action1>")
      doTask("2 ProjectCard<Kim, Hand>")
    }

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks("UseCardAction<AiCentral, Action1>", "2 ProjectCard")
      }
    }
  }

  @Test
  internal fun `Cannot use a second action that AI Central does not have`() {
    shouldThrow<DeadEndException> { useSecondAction() }
    kim.inTurn { doTasks("UseCardAction<AiCentral, Action1>", "2 ProjectCard") }
  }

  // The shared task handler currently leaves failed Admin work pending when a manual Player task
  // can still execute. The proposed Admin contract requires rejection before this request returns.
  @Ignore
  @Test
  internal fun `Cannot use an absent action while another Player task remains`() {
    offerSecondActionWithOtherWork()

    shouldThrow<DeadEndException> { kim.doTask("UseCardAction<AiCentral, Action2>") }

    kim.doTasks("UseCardAction<AiCentral, Action1>", "2 ProjectCard")
  }

  @Test
  internal fun `BUG - pending Player work hides AI Central's nonexistent action failure`() {
    offerSecondActionWithOtherWork()

    kim.doTask("UseCardAction<AiCentral, Action2>").expect("0 ProjectCard")

    admin.tasks.isEmpty() shouldBe false
    // Finishing the unrelated Player task finally exposes the failed Admin action.
    shouldThrow<DeadEndException> { kim.doTask("Plant") }
    kim.count("Plant") shouldBe 0
  }

  private fun offerSecondActionWithOtherWork() {
    kim.exMachina("$AiCentral")
    kim.beginOperation("OfferAction")
    kim.addTasks("Plant!")
  }

  private fun useSecondAction() {
    kim.exMachina("$AiCentral")
    kim.inTurn { doTask("UseCardAction<AiCentral, Action2>") }
  }
}
