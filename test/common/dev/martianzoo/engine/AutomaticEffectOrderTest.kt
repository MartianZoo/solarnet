package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.testsupport.count
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
    val admin = world.testAgents()
    admin.runSystem("Listener, Token")

    admin.runSystem("Trigger")

    admin.world.reader.count("Token") shouldBe 1
  }

  @Test
  internal fun selfEffectsRetainDeclarationOrder() {
    val world = Engine.newGame(selfEffectPremise)
    val admin = world.testAgents()

    admin.runSystem("Source")

    admin.world.reader.count("Observed") shouldBe 1
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
