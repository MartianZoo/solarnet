package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.WorldGovernmentAdvisor
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ThawerTest : TfmSandboxTest() {
  @Test
  internal fun `Thawer credits only the player's own temperature increases`() {
    newTestGame(addOptions = "Thawer, Builder, Engineer, WorldGovernmentAdvisor")
    kim.setToExMachina(90, "MC")
    asteroid()
    asteroid()
    asteroid("PROD[Heat]")
    asteroid()
    stan.inTurn {
      doTasks(*standardProjectTasks("AsteroidProject"), "14 Pay<> FROM MC")
      doTask("TemperatureStep")
      doTasks("TerraformRating", "PROD[Heat]")
      doTask("ThawerCredit")
    }
    kim.exMachina("$WorldGovernmentAdvisor")
    kim.inTurn {
          doTasks(*cardActionTasks("WorldGovernmentAdvisor"))

          doTask("TemperatureStep BY Admin")
        }
        .expect("0 TerraformRating")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("UseAction<ClaimMilestoneAction, Action1>")
        doTask("8 Pay<> FROM MC")
        doTask("Thawer")
      }
    }

    kim.inTurn {
      doTasks(*standardProjectTasks("AsteroidProject"), "14 Pay<> FROM MC")
      doTask("TemperatureStep")
      doTask("TerraformRating")
      doTask("ThawerCredit")
    }
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC")
      doTask("Thawer")
    }
  }

  private fun asteroid(vararg bonuses: String) {
    kim.inTurn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "ThawerCredit",
          *bonuses,
      )
    }
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    // Unresolved adjudication (question 28, 2026-10-14); this records current behavior.
    // https://boardgamegeek.com/thread/3478406/article/47218204#47218204
    @Test
    internal fun `Snow Cover does not undo credit for earlier temperature increases`() {
      newTestGame(addOptions = "TurmoilExpansion, Thawer, Builder, Engineer", playerCount = 2)
      // Generation 1: reveal Mohole Lake; its Snow Cover effect will resolve in generation 4.
      // Later reveals fix delegate placements but will not resolve during this scenario.
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("PartyLeader<Greens, Neutral>")
      admin.doTask("MoholeLakeGlobalEvent")
      admin.doTask("PartyLeader<Kelvinists, Neutral>")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

      // Generation 2: Democratic Reform gives Stan an Admin ocean, leaving temperature alone.
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("PartyLeader<Unity, Neutral>")
      admin.doTask("ExploreFirstDirective")
      admin.doTask("PartyLeader<Reds, Neutral>")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

      // Generation 3: Minimal Impact Policy removes the sole ocean automatically.
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("PartyFollower<Kelvinists, Neutral>")
      admin.doTask("MoralMovement")
      admin.doTask("PartyFollower<Reds, Neutral>")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

      // Generation 4: Kim earns five temperature credits before Snow Cover removes two steps.
      stan.doTask("Pass")
      kim.completeOperation {
        doTasks(*standardProjectTasks("AsteroidProject"))
        autoExecNow(EAGER)
      }
      kim.completeOperation {
        doTasks(*standardProjectTasks("AsteroidProject"))
        autoExecNow(EAGER)
      }
      kim.completeOperation {
        doTasks(*standardProjectTasks("AsteroidProject"))
        autoExecNow(EAGER)
      }
      kim.completeOperation {
        doTasks(*standardProjectTasks("AsteroidProject"))
        autoExecNow(EAGER)
      }
      kim.completeOperation {
        doTasks(*standardProjectTasks("AsteroidProject"))
        autoExecNow(EAGER)
      }
      admin.count("TemperatureStep") shouldBe 5
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("PartyLeader<Scientists, Neutral>")
      admin.doTask("FreeAcademiaTreaty")
      admin.doTask("PartyFollower<Scientists, Neutral>")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

      // Generation 5: the track fell, but Kim's five earned credits still qualify.
      // Re-raising a removed step also earns fresh credit.
      admin.count("TemperatureStep") shouldBe 3
      kim.completeOperation {
            doTasks(*standardProjectTasks("AsteroidProject"))
            autoExecNow(EAGER)
          }
          .expect("TemperatureStep, ThawerCredit, TerraformRating")
      kim.count("ThawerCredit") shouldBe 6
      kim.doTasks("UseAction<ClaimMilestoneAction, Action1>")
    }
  }
}
