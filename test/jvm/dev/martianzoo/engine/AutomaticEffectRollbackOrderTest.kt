package dev.martianzoo.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AutomaticEffectRollbackOrderTest {
  @Test
  internal fun rollbackDoesNotChangeWhichAutomaticSiblingRunsFirst() {
    if (System.getenv("SOLARNET_RANDOM_AUTOMATIC_EFFECTS").equals("true", ignoreCase = true)) {
      return
    }

    val world = Engine.newGame(premise)
    val player = world.testAgent(PLAYER1)
    player.runOperation("Earlier")
    player.runOperation("Later")
    player.runOperation("Token")
    val baseline = world.timeline.checkpoint()

    val firstContext = removalContext(world, player)
    world.timeline.seek(baseline)
    world.timeline.truncate()

    player.runOperation("-Earlier")
    world.timeline.seek(baseline)
    world.timeline.truncate()

    removalContext(world, player) shouldBe firstContext
  }

  private fun removalContext(world: World, player: Agent): String {
    val before = world.events.nextOrdinal
    player.runOperation("Trigger")
    val removal =
        world.events.changesSince(before).single { it.change.removing.toString() == "Token" }
    return checkNotNull(removal.cause).context.toString()
  }

  private companion object {
    val premise =
        testGamePremise(
            """
            CLASS Trigger : Signal
            CLASS Token
            CLASS Earlier { Trigger:: -Token* }
            CLASS Later { Trigger:: -Token* }
            """,
            players = 1,
        )
  }
}
