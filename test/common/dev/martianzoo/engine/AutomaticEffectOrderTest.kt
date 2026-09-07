package dev.martianzoo.engine

import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AutomaticEffectOrderTest {
  @Test
  internal fun rollbackDoesNotChangeWhichAutomaticSiblingRunsFirst() {
    if (randomAutomaticEffectOrderEnabled) return

    val world = Engine.newGame(premise) as WholeWorld
    val admin = world.agent(ADMIN)
    admin.manual("Earlier")
    admin.manual("Later")
    admin.manual("Token")
    val baseline = world.timeline.checkpoint()

    val firstContext = removalContext(world, admin)
    world.timeline.rollBack(baseline)

    admin.manual("-Earlier")
    world.timeline.rollBack(baseline)

    removalContext(world, admin) shouldBe firstContext
  }

  private fun removalContext(world: WholeWorld, admin: Agent): String {
    val before = world.timeline.checkpoint()
    admin.manual("Trigger")
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
            CLASS Earlier { Trigger:: -Token. }
            CLASS Later { Trigger:: -Token. }
            """,
            players = 0,
        )
  }
}
