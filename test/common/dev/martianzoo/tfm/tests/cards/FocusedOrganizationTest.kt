package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
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

    kim.cardAction1(FocusedOrganization) {
          doTask("-Steel")
          doTask("Plant")
        }
        .expect("-Steel, Plant, 0 ProjectCard")
  }

  @Test
  @Ignore // The sequential resource loss does not open a heat payment choice.
  internal fun `Can spend a Stormcraft floater as heat for its action`() {
    setupStormcraftFloater()

    kim.cardAction1(FocusedOrganization) {
          doTask("-Floater<$StormcraftIncorporated>")
          doTask("Plant")
        }
        .expect("-Floater<$StormcraftIncorporated>, Plant, 0 Heat, 0 ProjectCard")
  }

  @Test
  internal fun `BUG - Rejects a Stormcraft floater when spending a standard resource`() {
    setupStormcraftFloater()
    val projectCardsBefore = kim.count("ProjectCard")

    shouldThrow<NarrowingException> {
      kim.cardAction1(FocusedOrganization) {
        doTask("-Floater<$StormcraftIncorporated>")
        doTask("Plant")
      }
    }
    kim.count("Floater<$StormcraftIncorporated>") shouldBe 1
    kim.count("ProjectCard") shouldBe projectCardsBefore
    kim.count("$FocusedOrganization") shouldBe 1
  }

  private fun setupStormcraftFloater() {
    newTestGame("FocusedOrganization, Unsafe", kimCorporation = StormcraftIncorporated)
    kim.exMachina("$FocusedOrganization, Floater<$StormcraftIncorporated>")
  }
}
