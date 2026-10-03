package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.HiredRaiders
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class HiredRaidersTest : CardTest() {
  // Resolved FAQ: Hired Raiders may steal less than its maximum, but must steal at least one.
  @Test
  internal fun `Cannot decline stealing when resources are available`() {
    newGame(CorporateEraExpansion)
    p1.runOperation("MC, ProjectCard")
    val p2 = requireP2()
    p2.runOperation("2 Steel, 3 MC")
    admin.phase("Action")

    p1.playProject(HiredRaiders, 1) {
          shouldThrow<NarrowingException> { declineTask() }
          p2.assertCounts(2 to "Steel", 3 to "MC")
          doTask("3 MC<Player1> FROM MC<Player2>")
        }
        .expect("0 Steel<Player1>, 0 Steel<Player2>, -3 MC<Player2>")
  }

  @Test
  internal fun `May steal less than the offered maximum`() {
    newGame(CorporateEraExpansion, players = 3)
    admin.phase("Action")
    p1.autoExecPolicy = NONE
    p1.runOperation("2 MC, ProjectCard")
    val p2 = requireP2()
    val p3 = game.testTfm(PLAYER3)
    p2.runOperation("2 Steel")
    p3.runOperation("2 Steel")

    p1.playProject(HiredRaiders, 1) {
          doTask("Steel<Player1> FROM Steel<Player3>")
        }
        .expect("Steel<Player1>, -Steel<Player3>")

    p2.assertCounts(2 to "Steel")
  }
}
