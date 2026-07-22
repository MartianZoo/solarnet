package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class CeosFavoriteTest : CardTest() {
  // FAQ: "this card can still be played without effect."
  @Test
  fun `can be played without effect when no card already has a resource`() {
    val game = newBareGame(Canon.fromOptionCodes("VERB", 2))
    val p1 = game.tfm(PLAYER1)
    p1.sneak("10 ProjectCard, ForcedPrecipitation")

    p1.manual("CeosFavoriteProject")

    p1.assertCounts(0 to "Floater")
  }

  // FAQ: "This card can be played to add an additional resource to 'Search for Life'."
  @Test
  fun `must add to an eligible card, even Search for Life`() {
    val game = newBareGame(Canon.fromOptionCodes("VERB", 2))
    val p1 = game.tfm(PLAYER1)
    p1.sneak("10 ProjectCard, SearchForLife, Science<SearchForLife>")

    shouldThrow<NarrowingException> { p1.manual("CeosFavoriteProject") { doTask("Ok") } }
    p1.manual("CeosFavoriteProject") { doTask("Science<SearchForLife>") }
        .expect("Science<SearchForLife>")

    p1.assertCounts(2 to "Science<SearchForLife>")
  }
}
