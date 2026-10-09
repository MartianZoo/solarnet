package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Ignore
import kotlin.test.Test

internal class WgProjectTest : CardTest() {

  @Ignore // WG Project does not make the Prelude 1 pool available.
  @Test
  internal fun `Drawn Prelude is playable without explicitly selecting its pool`() {
    playDonationWithoutPreludePool().expect("12 MC, 0 PreludeCard")
  }

  @Test
  internal fun `BUG - Drawn Prelude is unavailable without explicitly selecting its pool`() {
    shouldThrow<NarrowingException> { playDonationWithoutPreludePool() }.detail shouldContain
        "$Donation"
    p1.assertCounts(9 to "MC", 1 to "ProjectCard", 0 to "$WgProject", 0 to "PreludeCard")
  }

  private fun playDonationWithoutPreludePool(): TaskResult {
    newGame(GameConfig("WgProject, TurmoilExpansion", "Player1", "Player2"))
    admin.runOperation("-Chairman<Neutral>")
    admin.phase("Action")
    p1.runOperation("9 MC, ProjectCard, Chairman")
    return p1.playProject(WgProject, 9) { p1.playPrelude(Donation) }
  }
}
