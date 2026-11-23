package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.testsupport.count
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AutomaticEffectNarrowingTest {
  @Test
  internal fun automaticGainUsesTheOnlyTargetWithRemainingCapacity() {
    val game = Engine.newGame(premise)
    val admin = game.testAgents()

    admin.runSystem("First")
    admin.runSystem("Second")
    admin.runSystem("Third")

    admin.world.reader.count("Link<First, Second>") shouldBe 1
    admin.world.reader.count("Link<Second, Third>") shouldBe 1
    admin.world.reader.count("Link") shouldBe 2
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
