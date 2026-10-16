package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import kotlin.test.Test

internal class NobelPrizeTest : TfmSandboxTest() {
  @Test
  internal fun `Prelude draws cards with requirements and leaves its wild unassigned`() {
    newTestGame("PreludeExpansion, NobelPrize")
    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<NobelPrize>>",
              "NobelPrize FROM PreludeCard",
              "5 MC",
              "2 SearchForCard<RequirementFilter>",
          )
        }
        .expect("2 ProjectCard, WildTag<NobelPrize>, 0 Tag<WildTagElection<WildTag<NobelPrize>>>")
  }
}
