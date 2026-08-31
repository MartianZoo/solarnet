package dev.martianzoo.engine

import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AutomaticEffectOrderTest {
  @Test
  internal fun rollbackDoesNotChangeWhichAutomaticSiblingRunsFirst() {
    if (randomAutomaticEffectOrderEnabled) return

    val world = Engine.newGame(premise) as WholeWorld
    val engine = world.gameplay(ENGINE).godMode()
    engine.manual("Earlier")
    engine.manual("Later")
    engine.manual("Token")
    val baseline = world.timeline.checkpoint()

    val firstContext = removalContext(world, engine)
    world.timeline.rollBack(baseline)

    engine.manual("-Earlier")
    world.timeline.rollBack(baseline)

    removalContext(world, engine) shouldBe firstContext
  }

  private fun removalContext(world: WholeWorld, engine: Gameplay.GodMode): String {
    val before = world.timeline.checkpoint()
    engine.manual("Trigger")
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
