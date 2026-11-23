package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.testsupport.count
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EachSelectorBindingTest {
  @Test
  internal fun selectedHolderComesFromTheEnclosingEvent() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS SelectorEvent
                ABSTRACT CLASS Token : Owned<Player>
                CLASS RedToken : Token
                CLASS BlueToken : Token
                CLASS Provider {
                  SelectorEvent BY Me@Player: EACH @Token<Me@Player> { -@Token }
                }
                """,
                players = 2,
            )
        )
    val admin = game.testAgents()
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    admin.runSystem("Provider")
    p1.runOperation("RedToken, BlueToken")
    p2.runOperation("RedToken, BlueToken")

    p1.runOperation("SelectorEvent")

    p1.count("Token") shouldBe 0
    p2.count("Token") shouldBe 2
  }
}
