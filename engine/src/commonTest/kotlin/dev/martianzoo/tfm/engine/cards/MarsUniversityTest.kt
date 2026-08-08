package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.canon.Canon.Option.CorporateEraExpansion
import kotlin.test.Test

class MarsUniversityTest : CardTest() {
  @Test
  fun `two tag effects can discard twice before drawing twice`() {
    newGame(CorporateEraExpansion)
    p1.manual("5 ProjectCard, MarsUniversity") { doTask("Ok") }
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual
        .manual("Research") {
          doTask("2 ProjectCard")
          doFirstTask("-ProjectCard")
          doFirstTask("-ProjectCard")
          doFirstTask("ProjectCard")
          doFirstTask("ProjectCard")
        }
        .expect("2 ScienceTag, 2 ProjectCard")
  }

  @Test
  fun `two tag effects can each draw before the next discard`() {
    newGame(CorporateEraExpansion)
    p1.manual("5 ProjectCard, MarsUniversity") { doTask("Ok") }
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual
        .manual("Research") {
          doTask("2 ProjectCard")
          doFirstTask("-ProjectCard")
          doTask("ProjectCard")
          doFirstTask("-ProjectCard")
          doFirstTask("ProjectCard")
        }
        .expect("2 ScienceTag, 2 ProjectCard")
  }
}
