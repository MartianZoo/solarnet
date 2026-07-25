package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class CrashSiteCleanupTest : CardTest() {
  @Test
  fun `another player's plants must have been removed this generation`() {
    val game = newGame("BMX", 2)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    p1.phase("Action")
    p1.sneak("100, 2 ProjectCard, Plant<Player1>, 2 Plant<Player2>")

    shouldThrow<RequirementException> { p1.playProject("CrashSiteCleanup", 4) }

    p1.manual("-Plant<Player1>")
    shouldThrow<RequirementException> { p1.playProject("CrashSiteCleanup", 4) }

    p2.manual("-Plant<Player2>")
    shouldThrow<RequirementException> { p1.playProject("CrashSiteCleanup", 4) }

    p1.manual("-Plant<Player2>")
    p1.playProject("CrashSiteCleanup", 4) { doTask("Titanium") }.expect("Titanium")

    game.tfm(ENGINE).manual("Generation")
    shouldThrow<RequirementException> { p1.playProject("CrashSiteCleanup", 4) }
  }
}
