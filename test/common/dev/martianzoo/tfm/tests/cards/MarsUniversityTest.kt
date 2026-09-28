package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class MarsUniversityTest : CardTest() {
  @Test
  internal fun `Two tag effects exchange one card at a time`() {
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
          shouldThrow<TaskException> { doTask("-ProjectCard") }
          doTask("ProjectCard FROM ProjectCard")
          shouldThrow<TaskException> { doTask("-ProjectCard") }
          doTask("ProjectCard FROM ProjectCard")
        }
        .expect("2 ProjectCard")
  }

  @Test
  internal fun `Mars University cannot exchange a card with an empty hand`() {
    newGame(CorporateEraExpansion)
    p1.playCorp(CrediCor, 1)
    admin.phase("Action")

    p1.playProject(MarsUniversity, 8).expect("-ProjectCard")
  }

  @Test
  internal fun `Mars University may decline its discard even with another project in hand`() {
    newGame(CorporateEraExpansion)
    p1.playCorp(CrediCor, 3)
    admin.phase("Action")
    p1.playProject(MarsUniversity, 8) { declineTask() }

    p1.playProject(SearchForLife, 3) { declineTask() }.expect("-ProjectCard")
  }
}
