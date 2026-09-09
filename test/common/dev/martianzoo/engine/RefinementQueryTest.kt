package dev.martianzoo.engine

import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.testsupport.PLAYER3
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RefinementQueryTest {
  @Test
  internal fun bareDependencyRefinesEveryCompatibleDomain() {
    val game = setUpGame()
    val p1 = game.agent(PLAYER1)
    val p2 = game.agent(PLAYER2)
    p1.count("StartToken") shouldBe 1
    p2.count("StartToken") shouldBe 0

    listOf("Player", "Owner", "Actor", "Anyone", "Component").forEach { domain ->
      p2.count("$domain(HAS StartToken)") shouldBe 1
    }

    p2.count("Player(HAS StartToken<Owner>)") shouldBe 0
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
    game.agent(PLAYER1).manual("Token<Player1>")
    val p2 = game.agent(PLAYER2)

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
    val admin = game.agent(ADMIN)
    admin.manual("Token<Player1>")

    admin.manual("EACH Player(HAS =1 (RANK Player { Player(HAS Token) })) { Prize<Player> }")

    game.agent(PLAYER1).count("Prize") shouldBe 1
    game.agent(PLAYER2).count("Prize") shouldBe 1
    game.agent(PLAYER3).count("Prize") shouldBe 1
  }
}
