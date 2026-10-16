package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RedAppeasementTest : TfmGameplayTest(NONE) {
  @BeforeTest
  internal fun prepareRedsSupport() {
    newTestGame("RedAppeasement, TurmoilExpansion", playerCount = 2)
    kim.doTasks(
        "LobbyAction",
        "PartyFollower<Reds> FROM LobbyDelegate",
        "PaidLobbyAction",
        "5 Pay<> FROM MC",
        "PartyLeader<Reds>",
    )
  }

  @Test
  internal fun `Cannot be played after another player has passed`() {
    stan.doTask("Pass")

    shouldThrow<RequirementException> {
      kim.doTasks("PlayProject<Class<RedAppeasement>>", "RedAppeasement FROM ProjectCard")
    }
  }

  @Test
  internal fun `Passes its owner so the next players pass ends the generation`() {
    stan.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "MC FROM ProjectCard",
    )
    stan.selectTask("-Baton")

    kim.doTasks(
        "PlayProject<Class<RedAppeasement>>",
        "RedAppeasement FROM ProjectCard",
        "PROD[2 MC]",
        "Pass",
    )
    kim.count("Pass") shouldBe 1
    kim.count("PlayedEvent<Class<RedAppeasement>>") shouldBe 1
    admin.count("ActionScope") shouldBe 0
    stan.doTask("Pass")

    admin.count("ProductionPhase") shouldBe 1
  }

  @Test
  internal fun `Playing as the second action passes its owner immediately`() {
    stan.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "MC FROM ProjectCard",
    )
    stan.selectTask("-Baton")

    kim.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    kim.doTasks(
        "PlayProject<Class<RedAppeasement>>",
        "RedAppeasement FROM ProjectCard",
        "PROD[2 MC]",
        "Pass",
    )
    kim.count("Pass") shouldBe 1
    stan.doTask("Pass")

    admin.count("ProductionPhase") shouldBe 1
  }
}
