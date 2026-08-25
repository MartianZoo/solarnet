package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class MarsUniversityTest : CardTest() {
  @Test
  internal fun `Two tag effects can discard twice before drawing twice`() {
    newGame(CorporateEraExpansion)
    p1.manual(
        "5 ProjectCard, $MarsUniversity"
    ) { /* Decline Mars University's discard-and-draw effect. */
      declineTask()
    }
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual
        .manual("$Research") {
          doTask("2 ProjectCard")
          doTask("-ProjectCard")
          doTask("-ProjectCard")
          doTask("ProjectCard")
          doTask("ProjectCard")
        }
        .expect("2 ProjectCard")
  }

  @Test
  internal fun `Two tag effects can each draw before the next discard`() {
    newGame(CorporateEraExpansion)
    p1.manual(
        "5 ProjectCard, $MarsUniversity"
    ) { /* Decline Mars University's discard-and-draw effect. */
      declineTask()
    }
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual
        .manual("$Research") {
          doTask("2 ProjectCard")
          doTask("-ProjectCard")
          doTask("ProjectCard")
          doTask("-ProjectCard")
          doTask("ProjectCard")
        }
        .expect("2 ProjectCard")
  }
}
