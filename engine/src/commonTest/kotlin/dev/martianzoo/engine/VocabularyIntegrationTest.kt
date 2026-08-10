package dev.martianzoo.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.canonicalPremise
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VocabularyIntegrationTest {
  @Test
  fun worldCanonicalizesSessionInputBeforeEngineResolution() {
    val world =
        Engine.newGame(
            canonicalPremise(),
            locale = "EN_us",
            inputOnlySynonyms = listOf("Cash" to "Megacredit"),
        )
    val player = world.gameplay(PLAYER1).godMode()

    player.manual("Cash")

    world.vocabulary.locale shouldBe "en-us"
    player.count("Cash") shouldBe 1
  }
}
