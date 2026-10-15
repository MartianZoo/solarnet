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
        "LobbyAction<Action1>",
        "PartyDelegate<Reds>",
        "LobbyAction<Action2>",
        "-5 MC",
        "PartyDelegate<Reds>",
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
        "0 StandardAction",
    )

    kim.doTasks(
        "PlayProject<Class<RedAppeasement>>",
        "RedAppeasement FROM ProjectCard",
        "PROD[2 MC]",
        "Pass",
        "PlayedEvent<Class<RedAppeasement>> FROM RedAppeasement",
    )
    kim.count("Pass") shouldBe 1
    stan.doTask("Pass")

    admin.count("ProductionPhase") shouldBe 1
  }

  @Test
  internal fun `Playing as the second action passes its owner immediately`() {
    stan.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "MC FROM ProjectCard",
        "0 StandardAction",
    )

    kim.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    kim.doTasks(
        "PlayProject<Class<RedAppeasement>>",
        "RedAppeasement FROM ProjectCard",
        "PROD[2 MC]",
        "Pass",
        "PlayedEvent<Class<RedAppeasement>> FROM RedAppeasement",
    )
    kim.count("Pass") shouldBe 1
    stan.doTask("Pass")

    admin.count("ProductionPhase") shouldBe 1
  }
}
