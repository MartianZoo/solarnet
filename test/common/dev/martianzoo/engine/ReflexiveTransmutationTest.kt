package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.toComponent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ReflexiveTransmutationTest {
  private val game =
      Engine.newGame(
          testGamePremise(
              """
              CLASS Token {
                HAS MAX 1 This
                This:: Gained
                -This:: Removed
                Pulse:: HeardPulse
              }
              CLASS Gained
              CLASS Removed
              CLASS Pulse : Signal
              CLASS HeardPulse
              """,
              players = 0,
          )
      )
  private val admin = game.testAgent(ADMIN)

  @Test
  internal fun exchangeAtCapacityFiresBothDirectionsWithoutChangingState() {
    admin.runOperation("Token")
    val tokenType = admin.resolve("Token")
    val observedCounts = mutableListOf<Int>()
    game.components.listenToCount(tokenType, game.reader, observedCounts::add)

    val result = admin.runOperation("Token FROM Token")

    admin.count("Token") shouldBe 1
    admin.count("Gained") shouldBe 2
    admin.count("Removed") shouldBe 1
    observedCounts.shouldContainExactly(1)
    result.changes
        .filter { it.change.gaining?.type == tokenType }
        .map { it.change }
        .shouldContainExactly(
            ComponentChange.Transmute(1, tokenType.toComponent(), tokenType.toComponent())
        )
    admin.runOperation("Pulse")
    admin.count("HeardPulse") shouldBe 1
  }

  @Test
  internal fun exchangeRequiresTheWholeSourceCount() {
    admin.runOperation("Token")

    shouldThrow<LimitsException> { admin.runOperation("2 Token FROM Token!") }

    admin.count("Token") shouldBe 1
    admin.count("Gained") shouldBe 1
    admin.count("Removed") shouldBe 0
  }

  @Test
  internal fun optionalExchangeCanBeDeclined() {
    admin.runOperation("Token")

    admin.runOperation("Token FROM Token?") { doTask("Ok") }

    admin.count("Token") shouldBe 1
    admin.count("Gained") shouldBe 1
    admin.count("Removed") shouldBe 0
  }

  @Test
  internal fun optionalExchangeCanExecute() {
    admin.runOperation("Token")

    admin.runOperation("Token FROM Token?") { doTask("Token FROM Token!") }

    admin.count("Token") shouldBe 1
    admin.count("Gained") shouldBe 2
    admin.count("Removed") shouldBe 1
  }
}
