package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.testsupport.count
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
  private val admin = Agents(game)

  @Test
  internal fun concreteSelfTransmutationIsRejectedWithoutFiringEffects() {
    admin.runSystem("Token")
    shouldThrow<ExpressionException> { admin.runSystem("Token FROM Token") }
    admin.world.reader.count("Token") shouldBe 1
    admin.world.reader.count("Gained") shouldBe 1
    admin.world.reader.count("Removed") shouldBe 0
  }

  @Test
  internal fun optionalSelfTransmutationIsInvalidEvenWhenItCouldBeDeclined() {
    admin.runSystem("Token")
    shouldThrow<ExpressionException> { admin.runSystem("Token FROM Token?") }
    admin.world.reader.count("Token") shouldBe 1
  }

  @Test
  internal fun optionalAbstractTransferCanDoNothingWhenOnlySelfIsAvailable() {
    admin.runSystem("Token")
    admin.runSystem("Token FROM Thing?")
    admin.world.reader.count("Token") shouldBe 1
    admin.world.reader.count("Gained") shouldBe 1
    admin.world.reader.count("Removed") shouldBe 0
  }

  @Test
  internal fun sharedAbstractVariableCannotTransmuteIntoItself() {
    shouldThrow<ExpressionException> { admin.runSystem("@Thing FROM @Thing") }
  }
}
