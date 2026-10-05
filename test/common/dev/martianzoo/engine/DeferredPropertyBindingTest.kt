package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DeferredPropertyBindingTest {
  @Test
  internal fun `a named fanout rebinds the player captured by its properties`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Score : Owned<Player>
                CLASS Prize : Owned<Player>
                CLASS Rule { score = COUNT "Score<Me@Player>" }
                CLASS Grant : Owned<Player> {
                  This: EACH Me@Player { Prize / EVAL Rule.score }
                }
                """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    p1.runOperation("2 Score")
    p2.runOperation("5 Score")

    p1.runOperation("Grant")

    p1.count("Prize") shouldBe 2
    p2.count("Prize") shouldBe 5
  }

  @Test
  internal fun `deferred requirements cannot borrow another player's resources`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Score : Owned<Player>
                CLASS Prize : Owned<Player>
                ABSTRACT CLASS Rule { requirement = Requirement }
                CLASS Qualified : Rule { requirement = HAS "2 Score" }
                CLASS Grant : Owned<Player> {
                  This:: EACH @Rule { EVAL @Rule.requirement: Prize }
                }
                """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    game.testAgent(ADMIN).runOperation("Qualified")
    p1.runOperation("2 Score")

    p1.runOperation("Grant")
    shouldThrow<RequirementException> { p2.runOperation("Grant") }

    p1.count("Prize") shouldBe 1
    p2.count("Prize") shouldBe 0
    p2.count("Grant") shouldBe 0
  }

  @Test
  internal fun `fanout properties retain the outer player when the selector does not rebind Me`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Score : Owned<Player>
                CLASS OtherScore : Owned<Player>
                CLASS Prize : Owned<Player>
                ABSTRACT CLASS Rule { HAS MAX 1 This; score = Metric }
                CLASS Shared { score = COUNT "Score" }
                CLASS Single : Rule { score = COUNT "EVAL Shared.score" }
                CLASS Double : Rule { score = COUNT "OtherScore" }
                CLASS Grant : Owned<Player> {
                  This: EACH @Rule { Prize / EVAL @Rule.score }
                }
                """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    game.testAgent(ADMIN).runOperation("Single, Double")
    p1.runOperation("2 Score, 4 OtherScore")
    p2.runOperation("5 Score, 10 OtherScore")

    p1.runOperation("Grant")
    p2.runOperation("Grant")

    p1.count("Prize") shouldBe 6
    p2.count("Prize") shouldBe 15
  }

  @Test
  internal fun `rank properties retain the outer player when the selector does not rebind Me`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Score : Owned<Player>
                CLASS OtherScore : Owned<Player>
                ABSTRACT CLASS Rule { HAS MAX 1 This; score = Metric }
                CLASS First : Rule { score = COUNT "Score" }
                CLASS Second : Rule { score = COUNT "OtherScore" }
                CLASS Prize<Rule> : Owned<Player>
                CLASS Grant : Owned<Player> {
                  This: EACH Winner@Rule(HAS =1 (RANK Ranked@Rule { EVAL Ranked@Rule.score })) { Prize<Winner@Rule> }
                }
                """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    game.testAgent(ADMIN).runOperation("First, Second")
    p1.runOperation("2 Score, OtherScore")
    p2.runOperation("Score, 3 OtherScore")

    p1.runOperation("Grant")
    p2.runOperation("Grant")

    p1.count("Prize<First>") shouldBe 1
    p1.count("Prize<Second>") shouldBe 0
    p2.count("Prize<First>") shouldBe 0
    p2.count("Prize<Second>") shouldBe 1
  }

  @Test
  internal fun `a deferred property uses the player actually bound by its trigger`() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Pulse
                CLASS Score : Owned<Player>
                CLASS Prize : Owned<Player>
                ABSTRACT CLASS Rule {
                  score = Metric
                  Pulse BY Me@Player: Prize<Me@Player> / EVAL This.score
                }
                CLASS ConcreteRule : Rule { score = COUNT "Score" }
                """,
                players = 2,
            )
        )
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    game.testAgent(ADMIN).runOperation("ConcreteRule")
    p1.runOperation("2 Score")
    p2.runOperation("Score")

    p1.runOperation("Pulse")
    p2.runOperation("Pulse")

    p1.count("Prize") shouldBe 2
    p2.count("Prize") shouldBe 1
  }
}
