package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AphroditeTest : TfmSandboxTest() {
  @Test
  internal fun `Pays when an opponent raises Venus`() {
    newTestGame(kimCorporation = Aphrodite)

    stan.inTurn {
      doTasks(*standardProjectTasks("AirScrappingProject"), "15 Pay<> FROM MC")
      doTask("VenusStep")
      doTask("TerraformRating")
      kim.doTask("2 MC")
    }
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Pays for World Government chosen by another player without granting TR`() {
      newTestGame(kimCorporation = Aphrodite)
      kim.doTask("Pass")
      stan.doTask("Pass")
      rob.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("OxygenStep! BY Admin")

      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
      // Research passes the start token to Stan; use the next genuine Solar phase.
      stan.doTask("Pass")
      rob.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      admin.count("SolarPhase") shouldBe 1
      stan
          .doTask("VenusStep! BY Admin")
          .expect("VenusStep, 2 MC<Kim>, 0 TerraformRating<Kim>, 0 TerraformRating<Stan>")
    }
  }
}
