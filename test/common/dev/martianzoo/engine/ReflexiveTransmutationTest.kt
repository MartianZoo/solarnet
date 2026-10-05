package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.state.Actor.Companion.ADMIN
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ReflexiveTransmutationTest {
  private val game =
      Engine.newGame(
          testGamePremise(
              """
              ABSTRACT CLASS Thing
              CLASS Token : Thing {
                This:: Gained
                -This:: Removed
              }
              CLASS Other : Thing
              CLASS Gained
              CLASS Removed
              """,
              players = 0,
          )
      )
  private val admin = game.testAgent(ADMIN)

  @Test
  internal fun concreteSelfTransmutationIsRejectedWithoutFiringEffects() {
    admin.runOperation("Token")
    shouldThrow<ExpressionException> { admin.runOperation("Token FROM Token") }
    admin.count("Token") shouldBe 1
    admin.count("Gained") shouldBe 1
    admin.count("Removed") shouldBe 0
  }

  @Test
  internal fun optionalSelfTransmutationIsInvalidEvenWhenItCouldBeDeclined() {
    admin.runOperation("Token")
    shouldThrow<ExpressionException> { admin.runOperation("Token FROM Token?") }
    admin.count("Token") shouldBe 1
  }

  @Test
  internal fun optionalAbstractTransferCanDoNothingWhenOnlySelfIsAvailable() {
    admin.runOperation("Token")
    admin.runOperation("Token FROM Thing?")
    admin.count("Token") shouldBe 1
    admin.count("Gained") shouldBe 1
    admin.count("Removed") shouldBe 0
  }

  @Test
  internal fun sharedAbstractVariableCannotTransmuteIntoItself() {
    shouldThrow<ExpressionException> { admin.runOperation("@Thing FROM @Thing") }
  }

  @Test
  internal fun independentAbstractChoicesMustNarrowToDifferentTypes() {
    admin.runOperation("Token, Other")
    admin.runOperation("Thing FROM Thing") {
      shouldThrow<ExpressionException> { doTask("Token FROM Token") }
      doTask("Other FROM Token")
    }
    admin.count("Token") shouldBe 0
    admin.count("Other") shouldBe 2
    admin.count("Removed") shouldBe 1
  }
}
