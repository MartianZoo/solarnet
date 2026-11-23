package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.testsupport.count
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GlobalEffectRoutingTest {
  @Test
  internal fun pendingEventCreatesOneOwnedSignalPerPlayer() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Event : System { HAS MAX 1 This }
                CLASS EventCard : Event { This:: Current<This> }
                CLASS Current<Event> : System {
                  HAS MAX 1 This
                  This:: Pending<This, Class<Reward>>
                }
                ABSTRACT CLASS GlobalEffect : Owned<Player>, Signal, System
                CLASS Reward : GlobalEffect { This: Coin }
                CLASS Pending<Current<Event>, Class<@GlobalEffect>> : System {
                  Resolve: EACH Me@Player { @GlobalEffect }
                }
                CLASS Resolve : Signal, System
                CLASS Coin : Owned<Player>
                """,
                players = 2,
            )
        )
    val admin = game.testAgents()
    val player1 = game.testAgent(PLAYER1)
    val player2 = game.testAgent(PLAYER2)

    admin.runSystem("EventCard")
    admin.world.reader.count("Pending") shouldBe 1
    admin.runSystem("Resolve")

    player1.count("Coin") shouldBe 1
    player2.count("Coin") shouldBe 1
    admin.runSystem("-Current<EventCard>")
    admin.world.reader.count("Pending") shouldBe 0
  }
}
