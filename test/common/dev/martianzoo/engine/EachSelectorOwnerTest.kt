package dev.martianzoo.engine

import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EachSelectorOwnerTest {
  @Test
  internal fun selectorOwnerComesFromTheEnclosingEvent() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS SelectorEvent
                ABSTRACT CLASS Token : Owned<Player>
                CLASS RedToken : Token
                CLASS BlueToken : Token
                CLASS Prize : Owned<Player>
                CLASS Provider {
                  SelectorEvent: EACH Token<Owner> { Prize<Owner> }
                }
                """,
                players = 2,
            )
        )
    val engine = game.agent(ENGINE)
    val p1 = game.agent(PLAYER1)
    val p2 = game.agent(PLAYER2)
    engine.manual("Provider")
    p1.manual("RedToken, BlueToken")
    p2.manual("RedToken, BlueToken")

    p1.manual("SelectorEvent")

    p1.count("Prize") shouldBe 2
    p2.count("Prize") shouldBe 0
  }
}
