package dev.martianzoo.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AutomaticEffectOrderTest {
  @Test
  internal fun selfEffectsRetainDeclarationOrder() {
    val world = Engine.newGame(selfEffectPremise) as WholeWorld
    val admin = world.agent(ADMIN)

    admin.runOperation("Source")

    admin.count("Observed") shouldBe 1
  }

  @Test
  internal fun rollbackDoesNotChangeWhichAutomaticSiblingRunsFirst() {
    if (randomAutomaticEffectOrderEnabled) return

    val world = Engine.newGame(premise) as WholeWorld
    val admin = world.agent(ADMIN)
    admin.runOperation("Earlier")
    admin.runOperation("Later")
    admin.runOperation("Token")
    val baseline = world.timeline.checkpoint()

    val firstContext = removalContext(world, admin)
    world.timeline.rollBack(baseline)

    admin.runOperation("-Earlier")
    world.timeline.rollBack(baseline)

    removalContext(world, admin) shouldBe firstContext
  }

  private fun removalContext(world: WholeWorld, admin: Agent): String {
    val before = world.timeline.checkpoint()
    admin.runOperation("Trigger")
    val removal =
        world.events.changesSince(before).single { it.change.removing.toString() == "Token" }
    return checkNotNull(removal.cause).context.toString()
  }

  private companion object {
    val selfEffectPremise =
        testGamePremise(
            """
            CLASS Source {
              HAS MAX 1 This
              This:: Watcher<This>
              This:: Pulse
            }
            CLASS Watcher<Source> { Pulse:: Observed. }
            CLASS Pulse
            CLASS Observed
            """,
            players = 0,
        )

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
