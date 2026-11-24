package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agenttestsupport.runSystem
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
    val agents = Agents(game) { EAGER }
    val player1 = agents[PLAYER1]
    val player2 = agents[PLAYER2]

    agents.runSystem("EventCard")
    agents.world.reader.count("Pending") shouldBe 1
    agents.runSystem("Resolve")

    player1.count("Coin") shouldBe 1
    player2.count("Coin") shouldBe 1
    agents.runSystem("-Current<EventCard>")
    agents.world.reader.count("Pending") shouldBe 0
  }
}
