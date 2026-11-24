package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.testsupport.count
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.giveTask
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AphroditeTest : TfmSandboxTest() {
  @Test
  internal fun `Pays when an opponent raises Venus`() {
    newTestGame(kimCorporation = Aphrodite)

    stan.inTurn {
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      // The active player chooses when to hand this task to its owner.
      giveTask(kim, "2 MC<Kim>")
    }
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Pays for World Government chosen by another player without granting TR`() {
      newTestGame("-QuickStartVariant, -ColoniesExpansion", kimCorporation = Aphrodite)
      kim.doTask("Pass")
      stan.doTask("Pass")
      rob.doTask("Pass")
      kim.doTasks("20 MC", "Plant")
      stan.doTask("20 MC")
      rob.doTask("20 MC")
      kim.doTask("OxygenStep! BY Admin")

      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
      // Research passes the start token to Stan; use the next genuine Solar phase.
      stan.doTask("Pass")
      rob.doTask("Pass")
      kim.doTask("Pass")
      kim.doTasks("20 MC", "Plant")
      stan.doTask("20 MC")
      rob.doTask("20 MC")

      admin.count("SolarPhase") shouldBe 1
      stan.doTask("VenusStep! BY Admin").expect("0 TerraformRating<Kim>, 0 TerraformRating<Stan>")
      kim.doTask("2 MC")
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    }
  }
}
