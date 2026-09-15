package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.Engine
import dev.martianzoo.generated.gameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.collections.shouldContain
import kotlin.test.Test

/** Passing characterizations of known Terraforming Mars rule defects. */
internal class BugsTest {
  @Test
  internal fun `SecondPlace incorrectly remains active with only two players`() {
    val twoPlayers =
        Engine.newGame(Canon.gamePremise(gameConfig(playerNames = listOf("Player1", "Player2"))))
    val threePlayers =
        Engine.newGame(
            Canon.gamePremise(gameConfig(playerNames = listOf("Player1", "Player2", "Player3")))
        )

    twoPlayers.classTable.allClassNames.shouldContain(cn("SecondPlace"))
    threePlayers.classTable.allClassNames.shouldContain(cn("SecondPlace"))
  }
}
