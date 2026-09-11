package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class MarsUniversityTest : CardTest() {
  @Test
  internal fun `Two tag effects can each draw before the next discard`() {
    newGame(CorporateEraExpansion)
    p1.runOperation(
        "5 ProjectCard, $MarsUniversity"
    ) { /* Decline Mars University's discard-and-draw effect. */
      declineTask()
    }
    val manual = p1.also { it.autoExecPolicy = NONE }

    manual
        .runOperation("$Research") {
          doTask("2 ProjectCard")
          doTask("-ProjectCard")
          doTask("ProjectCard")
          doTask("-ProjectCard")
          doTask("ProjectCard")
        }
        .expect("2 ProjectCard")
  }
}
