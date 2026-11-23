package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.state.Actor.Companion.ADMIN
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AutomaticEffectNarrowingTest {
  @Test
  internal fun automaticGainUsesTheOnlyTargetWithRemainingCapacity() {
    val game = Engine.newGame(premise)
    val admin = Agents(game)[ADMIN]

    admin.runOperation("First")
    admin.runOperation("Second")
    admin.runOperation("Third")

    admin.count("Link<First, Second>") shouldBe 1
    admin.count("Link<Second, Third>") shouldBe 1
    admin.count("Link") shouldBe 2
  }

  private companion object {
    val premise =
        testGamePremise(
            """
            ABSTRACT CLASS Person {
              HAS MAX 1 This
              HAS MAX 1 Link<This, Person>, MAX 1 Link<Person, This>
              HAS MAX 0 Link<This, This>
              This IF 2 Person:: Link<Person, This>
            }
            CLASS First : Person
            CLASS Second : Person
            CLASS Third : Person
            CLASS Link<Previous@Person, Next@Person>
            """,
            players = 0,
        )
  }
}
