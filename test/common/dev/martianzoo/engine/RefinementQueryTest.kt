package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.testsupport.count
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RefinementQueryTest {
  @Test
  internal fun aRefinementCandidateFillsAnOmittedDependency() {
    val agents = setUpGame()
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    p1.count("StartToken") shouldBe 1
    p2.count("StartToken") shouldBe 0

    p2.count("Player(HAS StartToken)") shouldBe 1
  }

  @Test
  internal fun writtenArgumentsConstrainTheRefinementCandidate() {
    val agents = setUpGame()
    val p2 = agents[PLAYER2]

    p2.count("Player(HAS StartToken<Player1>)") shouldBe 1
    p2.count("Player(HAS StartToken<Player2>)") shouldBe 0
    p2.count("Player(HAS StartToken<Anyone>)") shouldBe 1
  }

  @Test
  internal fun candidateBindingPrecedesAnyDefaultAndExplicitEmptyArgumentsAcceptIt() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Token<Player> {
                  DEFAULT Token<Player2>
                }
                """,
                players = 2,
            )
        )
    val agents = Agents(game) { EAGER }
    agents[PLAYER1].runOperation("Token<Player1>")
    val p2 = agents[PLAYER2]

    p2.count("Player(HAS Token)") shouldBe 1
    p2.count("Player(HAS Token<>)") shouldBe 0
  }

  @Test
  internal fun nestedRefinementUsesItsOwnCandidateInsideRank() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Token : Owned<Player>
                CLASS Prize : Owned<Player>
                """,
                players = 3,
            )
        )
    val agents = Agents(game) { EAGER }
    agents.runSystem("Token<Player1>")

    agents.runSystem("EACH @Player(HAS =1 (RANK { Player(HAS Token) })) { Prize<@Player> }")

    agents[PLAYER1].count("Prize") shouldBe 1
    agents[PLAYER2].count("Prize") shouldBe 1
    agents[PLAYER3].count("Prize") shouldBe 1
  }
}
