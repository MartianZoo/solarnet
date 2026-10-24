package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActionPhaseTurnsTest : TfmGameplayTest(NONE) {
  @Test
  internal fun `Multiplayer skips passed players and lets the last player continue`() {
    newTestGame(playerCount = 3)

    shouldThrow<TaskException> { stan.doTask("Pass") }
    kim.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    shouldThrow<TaskException> { kim.doTask("Pass") }
    val handoff = kim.selectTask("PassBaton")
    handoff.expect("-Baton<Kim>, Baton<Stan>")
    val batonChanges =
        handoff.changes
            .map { it.change }
            .filter {
              it.gaining?.type?.className == cn("Baton") ||
                  it.removing?.type?.className == cn("Baton")
            }
    val transfer = batonChanges.single() as ComponentChange.Transmute
    transfer.removing.type shouldBe kim.resolve("Baton<Kim>")
    transfer.gaining.type shouldBe kim.resolve("Baton<Stan>")
    admin.count("ActionPhase") shouldBe 1
    stan.doTask("Pass")
    rob.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    rob.selectTask("PassBaton")

    // Declining the second action did not pass Kim; Stan is skipped on the next circuit.
    kim.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    kim.selectTask("PassBaton")
    rob.doTask("Pass")
    val lastPlayerActions = game.events.nextOrdinal

    kim.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    kim.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    game.events.changesSince(lastPlayerActions).none {
      it.change.gaining?.type?.className == cn("Baton") ||
          it.change.removing?.type?.className == cn("Baton")
    } shouldBe true
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
    stan.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
  }

  @Test
  internal fun `Solo keeps offering actions until the player passes`() {
    newTestGame(playerCount = 1)
    val beforeActions = game.events.nextOrdinal

    kim.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    kim.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    kim.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    game.events.changesSince(beforeActions).none {
      it.change.gaining?.type?.className == cn("Baton") ||
          it.change.removing?.type?.className == cn("Baton")
    } shouldBe true
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
    kim.doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
  }
}
