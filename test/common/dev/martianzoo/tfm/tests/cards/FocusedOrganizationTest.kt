package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class FocusedOrganizationTest : TfmSandboxTest() {
  @Test
  internal fun `May gain a different resource than it spends`() {
    newTestGame("FocusedOrganization")
    kim.exMachina("$FocusedOrganization, Steel")

    kim.inTurn {
          doTask("UseCardAction<FocusedOrganization, Action1>")
          doTask("-ProjectCard<Kim, Hand>")
          doTask("-Steel")
          doTask("ProjectCard<Kim, Hand>")

          doTask("Plant")
        }
        .expect("0 ProjectCard")
  }

  @Test
  @Ignore // The sequential resource loss does not open a heat payment choice.
  internal fun `Can spend a Stormcraft floater as heat for its action`() {
    setupStormcraftFloater()

    useFocusedOrganization {
          doTask("PayFromCard<$StormcraftIncorporated> FROM Floater<$StormcraftIncorporated>")
        }
        .expect("-Floater<$StormcraftIncorporated>, Plant, 0 Heat, 0 ProjectCard")
  }

  // Unresolved adjudication (question 15, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3443314/article/45507249#45507249
  @Test
  internal fun `BUG - Rejects a Stormcraft floater when spending a standard resource`() {
    setupStormcraftFloater()
    kim.setToExMachina(1, "ProjectCard")

    useFocusedOrganization {
      shouldThrow<TaskException> {
        doTask("PayFromCard<$StormcraftIncorporated> FROM Floater<$StormcraftIncorporated>")
      }
      abort()
    }
    kim.count("Floater<$StormcraftIncorporated>") shouldBe 1
    kim.count("ProjectCard") shouldBe 1
    kim.count("$FocusedOrganization") shouldBe 1
  }

  private fun setupStormcraftFloater() {
    newTestGame("FocusedOrganization, Unsafe", kimCorporation = StormcraftIncorporated)
    kim.exMachina("$FocusedOrganization, Floater<$StormcraftIncorporated>")
  }

  private fun useFocusedOrganization(spendResource: OperationBlock): TaskResult = kim.inTurn {
    doTask("UseCardAction<FocusedOrganization, Action1>")
    doTask("-ProjectCard<Kim, Hand>")
    spendResource()
    doTask("ProjectCard<Kim, Hand>")
    doTask("Plant")
  }
}
