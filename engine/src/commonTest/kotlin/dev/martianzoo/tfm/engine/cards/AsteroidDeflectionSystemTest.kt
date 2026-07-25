package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class AsteroidDeflectionSystemTest : CardTest() {
  @Test
  fun `protects plants and scores revealed space cards`() {
    val game = newGame("BRMPX", 2)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    p1.phase("Action")
    p1.sneak("100, ProjectCard, Plant, Psychrophiles, Microbe<Psychrophiles>, PROD[Energy]")

    p1.playProject("AsteroidDeflectionSystem", 13).expect("PROD[-Energy]")

    shouldThrow<DeadEndException> { p2.manual("-Plant<Player1>") }
    p2.manual("-Microbe<Player1, Psychrophiles<Player1>>")
        .expect("-Microbe<Player1, Psychrophiles<Player1>>")
    p1.manual("-Plant<Player1>").expect("-Plant")

    p1.cardAction1("AsteroidDeflectionSystem") {
      doTask("Asteroid<AsteroidDeflectionSystem>")
    }
    game.tfm(ENGINE).phase("End")
    p1.assertCounts(21 to "VictoryPoint")
  }
}
