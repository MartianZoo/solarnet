package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ThawerTest : TfmSandboxTest() {
  @Test
  internal fun `Thawer credits only the player's own temperature increases`() {
    newTestGame(addOptions = "Thawer, Builder, Engineer, $WorldGovernmentAdvisor")
    kim.setToExMachina(90, "MC")
    asteroid()
    asteroid()
    asteroid("PROD[Heat]")
    asteroid()
    stan.inTurn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
          "ThawerCredit",
      )
    }
    kim.exMachina("$WorldGovernmentAdvisor")
    kim.inTurn {
          doTasks(*cardActionTasks("$WorldGovernmentAdvisor"))

          doTask("TemperatureStep BY Admin")
        }
        .expect("0 TerraformRating")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(
            "UseAction<ClaimMilestoneAction, Action1>",
            "8 Pay<> FROM MC",
            "Thawer",
        )
      }
    }

    kim.inTurn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "ThawerCredit",
      )
    }
    kim.inTurn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Thawer",
      )
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
      newTestGame(
          "-QuickStartVariant, -ColoniesExpansion, TurmoilExpansion, Thawer, Builder, Engineer",
          playerCount = 2,
          firstGlobalEvents =
              listOf("$MoholeLakeGlobalEvent", "$MinimalImpactPolicy", "$FreeAcademiaTreaty"),
      )
      // Prepare enough cash; the temperature increases and their credits remain real actions.
      kim.exMachina("100 MC")
      kim.turn {
        repeat(2) {
          doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
              "ThawerCredit",
          )
        }
      }
      stan.doTask("Pass")
      kim.turn {
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "ThawerCredit",
            "PROD[Heat]",
        )
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "ThawerCredit",
        )
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "ThawerCredit",
            "PROD[Heat]",
        )
      }
      admin.count("TemperatureStep") shouldBe 5
      kim.doTask("Pass")
      kim.doTasks("25 MC", "2 Heat")
      stan.doTask("20 MC")
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

      // Snow Cover resolves in the next generation and removes temperature, not earned credit.
      stan.doTask("Pass")
      kim.doTask("Pass")
      kim.doTasks("24 MC", "2 Heat")
      stan.doTasks(
          "19 MC",
          "VenusStep! BY Admin",
      )
      stan.doTask("-2 TemperatureStep BY Admin").expect("0 ThawerCredit<Kim>")
      players.forEach { it.doTask("0 ProjectCard") }
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

      admin.count("TemperatureStep") shouldBe 3
      kim.count("ThawerCredit") shouldBe 5
      // Re-raising a removed step earns fresh credit and the milestone claim actually finishes.
      kim.turn {
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "ThawerCredit",
            "-3 MC", // Reds policy.
        )
        doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Thawer")
      }
      kim.count("ThawerCredit") shouldBe 6
      kim.count("Thawer") shouldBe 1
    }
  }
}
