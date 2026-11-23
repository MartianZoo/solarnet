package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
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
    val agents = Agents(game)
    val admin = agents[ADMIN]
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    admin.runOperation("Provider")
    p1.runOperation("RedToken, BlueToken")
    p2.runOperation("RedToken, BlueToken")

    p1.runOperation("SelectorEvent") { autoExecNow(EAGER) }

    p1.count("Token") shouldBe 0
    p2.count("Token") shouldBe 2
  }
}
