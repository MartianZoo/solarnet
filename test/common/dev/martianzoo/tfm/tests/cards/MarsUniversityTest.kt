package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class MarsUniversityTest : CardTest() {
  @Test
  internal fun `Research can alternate discard and draw starting with a single card in hand`() {
    newGame(CorporateEraExpansion)
    p1.runOperation("ProjectCard, $MarsUniversity") { declineTask() }

    p1.runOperation("$Research") {
          p1.doTask("-ProjectCard")
          p1.doTask("-ProjectCard")
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
