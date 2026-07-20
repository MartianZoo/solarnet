package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class CeosFavoriteTest : CardTest() {
  @Test
  fun ceosFavoriteProject() {
    val game = newBareGame(Canon.fromOptionCodes("VERB", 2))

    with(game.tfm(PLAYER1)) {
      sneak("10 ProjectCard, ForcedPrecipitation")

      // We can't CEO's onto an empty card
      shouldThrow<DependencyException> {
        manual("CeosFavoriteProject") { doTask("Floater<ForcedPrecipitation>") }
      }

      sneak("Floater<ForcedPrecipitation>")
      assertCounts(1 to "Floater")

      manual("CeosFavoriteProject") { doTask("Floater<ForcedPrecipitation>") }
      assertCounts(2 to "Floater")
    }
  }
}
