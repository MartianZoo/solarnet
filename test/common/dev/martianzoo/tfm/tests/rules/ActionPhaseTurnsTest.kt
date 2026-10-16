package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActionPhaseTurnsTest : TfmGameplayTest(NONE) {
  @Test
  internal fun `Multiplayer skips passed players and lets the last player continue`() {
    newTestGame(playerCount = 3)

    shouldThrow<TaskException> { stan.doTask("Pass") }
    kim.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    shouldThrow<TaskException> { kim.doTask("Pass") }
    kim.selectTask("-Baton")
    stan.doTask("Pass")
    rob.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    rob.selectTask("-Baton")

    // Declining the second action did not pass Kim; Stan is skipped on the next circuit.
    kim.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    kim.selectTask("-Baton")
    rob.doTask("Pass")

    kim.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    kim.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    kim.doTask("Pass")
    admin.count("ProductionPhase") shouldBe 1
    admin.count("Baton") shouldBe 0

    kim.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    stan.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    rob.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    admin.count("SolarPhase") shouldBe 1
    admin.count("Baton") shouldBe 0

    kim.doTask("VenusStep! BY Admin")
    admin.count("ResearchPhase") shouldBe 1
    admin.count("Baton") shouldBe 0
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    stan.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    rob.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    admin.count("Generation") shouldBe 2
    // Passing only lasts for that generation, and the start token has moved to Stan.
    stan.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
  }

  @Test
  internal fun `Solo keeps offering actions until the player passes`() {
    newTestGame(playerCount = 1)

    kim.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    kim.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    kim.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    kim.doTask("Pass")
    admin.count("ProductionPhase") shouldBe 1
    admin.count("Baton") shouldBe 0
    kim.doTasks("13 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    admin.count("SolarPhase") shouldBe 1
    admin.count("Baton") shouldBe 0

    kim.doTask("VenusStep! BY Admin")
    admin.count("ResearchPhase") shouldBe 1
    admin.count("Baton") shouldBe 0
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    admin.count("Generation") shouldBe 2
    kim.doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
  }
}
