package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class IoSulphurResearchTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can choose the lesser card draw despite three Venus tags`() {
    kim.exMachina("$AerialMappers, $DeuteriumExport, $Dirigibles")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<IoSulphurResearch>>",
              "-17 MC",
              "IoSulphurResearch FROM ProjectCard",
              "ProjectCard",
          )
        }
        .expect("0 ProjectCard")
  }
}
