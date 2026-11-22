package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.state.Actor.Companion.ADMIN
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AutomaticEffectOrderTest {
  @Test
  internal fun selfEffectsFinishBeforeOtherAutomaticListeners() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Trigger : Signal { This:: -Token / Token }
                CLASS Listener { Trigger:: Token }
                CLASS Token
                """,
                players = 0,
            )
        )
    val admin = world.testAgent(ADMIN)
    admin.runOperation("Listener, Token")

    admin.runOperation("Trigger")

    admin.count("Token") shouldBe 1
  }

  @Test
  internal fun selfEffectsRetainDeclarationOrder() {
    val world = Engine.newGame(selfEffectPremise)
    val admin = world.testAgent(ADMIN)

    admin.runOperation("Source")

    admin.count("Observed") shouldBe 1
  }

  private companion object {
    val selfEffectPremise =
        testGamePremise(
            """
            ABSTRACT CLASS Subject
            CLASS Source : Subject {
              HAS MAX 1 This
              This:: Watcher<This>
              This:: Pulse
            }
            CLASS Watcher<Subject> { Pulse:: Observed* }
            CLASS Pulse
            CLASS Observed
            """,
            players = 0,
        )
  }
}
