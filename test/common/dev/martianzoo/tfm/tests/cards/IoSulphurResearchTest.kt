package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class IoSulphurResearchTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `CORRECT BEHAVIOR - May choose lesser card draw with three Venus tags`() {
    kim.exMachina("$AerialMappers, $DeuteriumExport, $Dirigibles")

    kim.inTurn {
          doTasks(
              *playCardTasks("IoSulphurResearch"),
              "17 Pay<> FROM MC",
              "IoSulphurResearch FROM ProjectCard",
              "ProjectCard",
          )
        }
        .expect("0 ProjectCard")
  }
}
