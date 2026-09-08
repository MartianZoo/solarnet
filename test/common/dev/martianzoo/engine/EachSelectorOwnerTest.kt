package dev.martianzoo.engine

import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
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
                CLASS Provider {
                  SelectorEvent: EACH Token<Owner> { -Token<Owner> }
                }
                """,
                players = 2,
            )
        )
    val admin = game.agent(ADMIN)
    val p1 = game.agent(PLAYER1)
    val p2 = game.agent(PLAYER2)
    admin.manual("Provider")
    p1.manual("RedToken, BlueToken")
    p2.manual("RedToken, BlueToken")

    p1.manual("SelectorEvent")

    p1.count("Token") shouldBe 0
    p2.count("Token") shouldBe 2
  }
}
