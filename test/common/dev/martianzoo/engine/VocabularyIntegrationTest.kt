package dev.martianzoo.engine

import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VocabularyIntegrationTest {
  @Test
  internal fun worldCanonicalizesSessionInputBeforeEngineResolution() {
    val world =
        Engine.newGame(
            testGamePremise(),
            locale = "EN_us",
            inputOnlySynonyms = listOf("Counter" to "Token"),
        )
    val player = world.agent(PLAYER1)

    player.manual("Counter")

    world.vocabulary.locale shouldBe "en-us"
    player.count("Counter") shouldBe 1
  }
}
