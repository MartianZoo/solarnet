package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.WorldGovernmentAdvisor
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
      doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
      selectTask("TemperatureStep.")
      doTasks("TerraformRating", "PROD[Heat]")
      selectOnlyTask()
    }
    kim.exMachina("$WorldGovernmentAdvisor")
    kim.inTurn {
          doTask("UseCardAction<WorldGovernmentAdvisor, Action1>")
          selectOnlyTask()
          doTask("TemperatureStep BY Admin")
        }
        .expect("0 TerraformRating")
    shouldThrow<RequirementException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("ClaimMilestone<Class<Thawer>>")
        selectTask("-X MC?")
        doTask("-MC / Owed<Class<MC>>")
        selectOnlyTask()
      }
    }

    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
      selectTask("TemperatureStep.")
      doTask("TerraformRating")
      selectOnlyTask()
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Thawer>>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
    }
  }

  private fun asteroid(vararg bonuses: String) {
    kim.inTurn {
      doTasks(
          "UseStandardProject<AsteroidProject>",
          "-14 MC",
          "TemperatureStep",
          "TerraformRating",
          "ThawerCredit",
          *bonuses,
      )
    }
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `Snow Cover does not undo credit for earlier temperature increases`() {
      newTestGame(addOptions = "TurmoilExpansion, Thawer, Builder, Engineer", playerCount = 2)
      // Generation 1: reveal Mohole Lake; its Snow Cover effect will resolve in generation 4.
      // Later reveals fix delegate placements but will not resolve during this scenario.
      kim.pass()
      stan.pass()
      kim.wgt("VenusStep")
      admin.doTask("MoholeLakeGlobalEvent")
      players.forEach { it.buyCards(0) }

      // Generation 2: Democratic Reform gives Stan an Admin ocean, leaving temperature alone.
      stan.pass()
      kim.pass()
      stan.wgt("VenusStep")
      stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
      admin.doTask("ExploreFirstDirective")
      players.forEach { it.buyCards(0) }

      // Generation 3: Minimal Impact Policy removes the sole ocean automatically.
      kim.pass()
      stan.pass()
      kim.wgt("VenusStep")
      admin.doTask("MoralMovement")
      players.forEach { it.buyCards(0) }

      // Generation 4: Kim earns five temperature credits before Snow Cover removes two steps.
      stan.pass()
      repeat(5) { kim.stdProject("AsteroidProject") }
      admin.count("TemperatureStep") shouldBe 5
      kim.pass()
      stan.wgt("VenusStep")
      admin.doTask("FreeAcademiaTreaty")
      players.forEach { it.buyCards(0) }

      // Generation 5: the track fell, but Kim's five earned credits still qualify.
      admin.count("TemperatureStep") shouldBe 3
      kim.claimMilestone(cn("Thawer"))
    }
  }
}
