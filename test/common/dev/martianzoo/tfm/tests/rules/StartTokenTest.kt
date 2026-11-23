package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.withTestSoloSetup
import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class StartTokenTest : TfmGameplayTest() {
  @Test
  internal fun `Start token begins with the first player and passes after each research phase`() {
    newTestGame(playerCount = 3)

    admin.assertCounts(
        3 to "NextPlayer",
        1 to "NextPlayer<Kim, Stan>",
        1 to "NextPlayer<Stan, Rob>",
        1 to "NextPlayer<Rob, Kim>",
    )
    admin.assertCounts(1 to "StartToken<Kim>", 0 to "StartToken<Stan>")

    listOf(kim, stan, rob).forEach { it.doTask("Pass") }
    players.forEach { it.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat") }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.assertCounts(0 to "StartToken<Kim>", 1 to "StartToken<Stan>")

    listOf(stan, rob, kim).forEach { it.doTask("Pass") }
    players.forEach { it.doTask("Heat FROM Energy") }
    players.forEach { it.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat") }
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.assertCounts(0 to "StartToken<Stan>", 1 to "StartToken<Rob>")

    listOf(rob, kim, stan).forEach { it.doTask("Pass") }
    players.forEach { it.doTask("Heat FROM Energy") }
    players.forEach { it.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat") }
    rob.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.assertCounts(1 to "StartToken<Kim>", 0 to "StartToken<Rob>")
    admin.assertCounts(1 to "StartToken")
  }

  @Test
  internal fun `Start token stays with the only player in a solo game`() {
    newTestGame(playerCount = 1)

    kim.doTask("Pass")
    // 14 TR plus -1 MC production after the solo Colonies setup penalty.
    kim.doTasks(
        "13 MC",
        "Steel",
        "Titanium",
        "Plant",
        "Energy",
        "Heat",
        "VenusStep! BY Admin",
        "4 ProjectCard<Selecting>",
        "-4 ProjectCard<Selecting>",
    )

    admin.assertCounts(1 to "StartToken<Kim>", 0 to "NextPlayer")
  }

  internal class Setup {
    @Test
    internal fun `solo setup links each greenery to its own city`() {
      // The second greenery neighbors the first city, but not the second city.
      shouldThrow<IllegalArgumentException> {
            withTestSoloSetup(
                canonicalPremise(players = 1),
                listOf("Tharsis_4_1", "Tharsis_5_1", "Tharsis_5_8", "Tharsis_3_1").map(::cn),
            )
          }
          .message
          .shouldContain("greenery must be adjacent")
    }

    @Test
    internal fun `Turn order reads the start token holder`() {
      val setup = canonicalPremise(Hellas, PromoCards, players = 2)
      val game = TfmEngine.newGame(setup)
      val agents = Agents(game) { NONE }
      val admin = agents[ADMIN]
      val p1 = agents[PLAYER1]
      val p2 = agents[PLAYER2]

      admin.exMachina("StartToken<Player2> FROM StartToken<Player1>")
      admin.beginOperation("SetupPhase FROM BootstrapPhase")
      admin.count("SetupPhase") shouldBe 1
      admin.count("Baton") shouldBe 0
      p1.doTasks(
          "20 TerraformRating",
          "CorporationCard",
          "10 ProjectCard<Selecting>",
          "-3 ProjectCard<Selecting>",
      )
      p2.doTasks(
          "20 TerraformRating",
          "CorporationCard",
          "10 ProjectCard<Selecting>",
          "-5 ProjectCard<Selecting>",
      )
      p2.completeOperation {
        doTasks(
            "PlayCard<Class<CorporationCard>, Class<$CrediCor>>",
            "57 MC",
            "15 Pay<> FROM MC",
            "5 ProjectCard<Hand FROM Selecting>",
        )
      }
      p1.completeOperation {
        doTasks(
            "PlayCard<Class<CorporationCard>, Class<$InterplanetaryCinematics>>",
            "30 MC",
            "20 Steel",
            "21 Pay<> FROM MC",
            "7 ProjectCard<Hand FROM Selecting>",
        )
      }

      admin.count("ActionPhase") shouldBe 1
      p2.completeOperation {
        doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
        doTask("Ok")
      }
      admin.count("Baton<Player1>") shouldBe 1
    }

    @Test
    internal fun `Research passes the token according to the explicit seat order`() {
      val game = setUpGame(players = 3)
      val agents = Agents(game) { NONE }
      val admin = agents[ADMIN]
      admin.exMachina(
          "NextPlayer<Player1, Player3> FROM NextPlayer<Player1, Player2>, " +
              "NextPlayer<Player3, Player2> FROM NextPlayer<Player3, Player1>, " +
              "NextPlayer<Player2, Player1> FROM NextPlayer<Player2, Player3>"
      )
      admin.beginOperation("ResearchPhase FROM SetupPhase")
      admin.assertCounts(0 to "StartToken<Player1>", 1 to "StartToken<Player3>")
    }
  }
}
