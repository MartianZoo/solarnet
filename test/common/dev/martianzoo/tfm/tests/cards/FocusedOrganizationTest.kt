package dev.martianzoo.tfm.tests.cards

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

    useStormcraftFloaterForFocusedOrganization()
        .expect("-Floater<$StormcraftIncorporated>, Plant, 0 Heat, 0 ProjectCard")
  }

  @Test
  internal fun `BUG - Rejects a Stormcraft floater when spending a standard resource`() {
    setupStormcraftFloater()
    kim.setToExMachina(1, "ProjectCard")

    shouldThrow<TaskException> { useStormcraftFloaterForFocusedOrganization() }
    kim.count("Floater<$StormcraftIncorporated>") shouldBe 1
    kim.count("ProjectCard") shouldBe 1
    kim.count("$FocusedOrganization") shouldBe 1
  }

  private fun setupStormcraftFloater() {
    newTestGame("FocusedOrganization, Unsafe", kimCorporation = StormcraftIncorporated)
    kim.exMachina("$FocusedOrganization, Floater<$StormcraftIncorporated>")
  }

  private fun useStormcraftFloaterForFocusedOrganization(): TaskResult = kim.inTurn {
    doTask("UseCardAction<FocusedOrganization, Action1>")
    doTask("-ProjectCard<Kim, Hand>")
    doTask("-MC")
    doTask("-Floater<$StormcraftIncorporated>")
    doTask("Plant")
  }
}
