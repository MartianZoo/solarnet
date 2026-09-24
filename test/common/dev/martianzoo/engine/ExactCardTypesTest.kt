package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ExactCardTypesTest {
  @Test
  internal fun `a card keeps its face when it moves and turns face up`() {
    val game = Engine.newGame(premise(players = 2))
    val player = game.testAgent(PLAYER1)
    val otherPlayer = game.testAgent(PLAYER2)
    game.testAgent(ADMIN).runOperation("Hand, Selecting")

    player.runOperation("ProjectCard<Class<Alpha>, Hand>, ProjectCard<Class<Beta>, Hand>")
    player.count("ProjectCard<Class<Alpha>, Hand>") shouldBe 1
    player.count("ProjectCard<Class<Beta>, Hand>") shouldBe 1
    shouldThrow<ExpressionException> { player.resolve("ProjectCard<Class<CorpFace>, Hand>") }
    shouldThrow<GameplayException> { player.runOperation("Alpha") }
    player.count("Alpha") shouldBe 0
    shouldThrow<GameplayException> {
      player.runOperation("ProjectCard<Class<Alpha>, Selecting>")
    }
    player.count("ProjectCard<Class<Alpha>, Hand>") shouldBe 1
    player.count("ProjectCard<Class<Alpha>, Selecting>") shouldBe 0
    shouldThrow<GameplayException> {
      otherPlayer.runOperation("ProjectCard<Class<Alpha>, Selecting>")
    }
    otherPlayer.count("ProjectCard<Class<Alpha>>") shouldBe 0
    otherPlayer.runOperation("CorpCard<Class<CorpFace>, Selecting>")
    otherPlayer.count("CorpCard<Class<CorpFace>, Selecting>") shouldBe 1

    player.runOperation("ProjectCard<Class<Alpha>, Selecting FROM Hand>")
    player.count("ProjectCard<Class<Alpha>, Selecting>") shouldBe 1
    player.count("ProjectCard<Class<Beta>, Hand>") shouldBe 1
    player.runOperation("ProjectCard<Class<Alpha>, Hand FROM Selecting>")
    player.count("ProjectCard<Class<Alpha>, Hand>") shouldBe 1
    player.count("ProjectCard<Selecting>") shouldBe 0

    shouldThrow<GameplayException> {
      player.runOperation("Play<Class<ProjectCard>, Class<Alpha>, Selecting>")
    }
    player.count("ProjectCard<Class<Alpha>, Hand>") shouldBe 1
    player.runOperation("Play<Class<ProjectCard>, Class<Alpha>, Hand>")

    player.count("ProjectCard<Class<Alpha>, Hand>") shouldBe 0
    player.count("ProjectCard<Class<Beta>, Hand>") shouldBe 1
    player.count("Alpha") shouldBe 1
    player.count("Beta") shouldBe 0
    shouldThrow<GameplayException> {
      player.runOperation("ProjectCard<Class<Alpha>, Hand>")
    }
    shouldThrow<GameplayException> {
      otherPlayer.runOperation("ProjectCard<Class<Alpha>, Hand>")
    }
    player.count("Alpha") shouldBe 1
    otherPlayer.runOperation("Play<Class<CorpCard>, Class<CorpFace>, Selecting>")
    otherPlayer.count("CorpFace") shouldBe 1
    otherPlayer.count("CorpCard<Class<CorpFace>>") shouldBe 0
  }

  @Test
  internal fun `a counted draw splits into separate face choices`() {
    val game = Engine.newGame(premise())
    val player = game.testAgent(PLAYER1)
    game.testAgent(ADMIN).runOperation("Hand")

    player.addTasks("2 ProjectCard")
    player.tasks.ids().size shouldBe 2

    player.doTask("ProjectCard<Class<Alpha>, Hand>")
    shouldThrow<GameplayException> { player.doTask("ProjectCard<Class<Alpha>, Hand>") }
    player.doTask("ProjectCard<Class<Beta>, Hand>")

    player.tasks.isEmpty() shouldBe true
    player.count("ProjectCard<Class<Alpha>, Hand>") shouldBe 1
    player.count("ProjectCard<Class<Beta>, Hand>") shouldBe 1
  }

  private fun premise(players: Int = 1) =
      testGamePremise(
          """
          ABSTRACT CLASS CardLocation : System {
            CLASS Hand { HAS MAX 1 This }
            CLASS Selecting { HAS MAX 1 This }
          }
          ABSTRACT CLASS Card<Class<CardFront>> : Owned<Player> {
            ABSTRACT CLASS CardBack<CardLocation> : Atomized {
              DEFAULT CardBack<Hand>
              CLASS ProjectCard : Card<Class<ProjectFront>>
              CLASS CorpCard : Card<Class<CorpFront>>
            }
            ABSTRACT CLASS CardFront<Class<CardBack>> : Card<Class<This>> {
              HAS MAX 1 Card<Player, Class<This>>
              ABSTRACT CLASS ProjectFront : CardFront<Class<ProjectCard>> {
                CLASS Alpha
                CLASS Beta
              }
              ABSTRACT CLASS CorpFront : CardFront<Class<CorpCard>> {
                CLASS CorpFace
              }
            }
          }
          CLASS Play<Class<@CardBack>, Class<@CardFront>, @CardLocation> : Owned<Player>, Signal {
            This:: @CardFront FROM @CardBack<Class<@CardFront>, @CardLocation>
          }
          """,
          players = players,
      )
}
