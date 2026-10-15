package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class HighCirclesTest : TfmSandboxTest() {
  @Test
  internal fun `Its delegates enable a Scientists project through Excentric Sponsor during Preludes`() {
    newTestGame("PreludeExpansion, HighCircles, TurmoilExpansion", playerCount = 2)
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<HighCircles>>",
          "HighCircles FROM PreludeCard",
          "TerraformRating",
      )
      doTask("SearchForCard<Kim, ReferenceFilter<Class<Ruling>>>!")
      doTask("2 PartyDelegate<Scientists>")
    }

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>",
              "ExcentricSponsor FROM PreludeCard",
              "PlayCard<Class<ProjectCard>, Class<SupportedResearch>>",
          )
          // Hand the discount to Admin before the remaining payment is offered.
          selectTask("-25* Owed")
          doTask("SupportedResearch FROM ProjectCard")
          doTask("2 ProjectCard<Kim, Hand>")
          doTask("Ok" /* No MC payment. */)
        }
        .expect("ProjectCard, $SupportedResearch")
  }

  @Test
  internal fun `Its delegates enable a Scientists project through Ecology Experts during Preludes`() {
    newTestGame(
        "PreludeExpansion, HighCircles, TurmoilExpansion, EcologyExperts, Unsafe",
        playerCount = 2,
    )
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<HighCircles>>",
          "HighCircles FROM PreludeCard",
          "TerraformRating",
      )
      doTask("SearchForCard<Kim, ReferenceFilter<Class<Ruling>>>!")
      doTask("2 PartyDelegate<Scientists>")
    }

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
              "EcologyExperts FROM PreludeCard",
              "PROD[Plant]",
              "PlayCard<Class<ProjectCard>, Class<SupportedResearch>>",
          )
          // Hand the requirement waiver to Admin before playing the project.
          selectTask("-Required* / Required")
          doTasks("-3 MC", "SupportedResearch FROM ProjectCard")
          doTask("2 ProjectCard<Kim, Hand>")
        }
        .expect("ProjectCard, $SupportedResearch")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Its influence adds to the chairmans Global Event payout`() {
      newTestGame("PreludeExpansion, HighCircles, TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTask("PlayCard<Class<PreludeCard>, Class<$HighCircles>>")
        doTask("2 PartyDelegate<Scientists>")
        autoExecNow(EAGER)
      }
      kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
      stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
      stan.autoExecNow(EAGER)

      stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
      stan.autoExecNow(EAGER)

      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      kim.count("Chairman") shouldBe 1
      admin.doTask("ExploreFirstDirective")
      kim.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      expectChanges("2 Plant<Kim>, 2 Steel<Kim>") {
        stan.doTask("VenusStep! BY Admin")
        players.forEach { it.autoExecNow(EAGER) }

        // Aquifer Released by Public Council pays for both the card and chairman influence.
        stan.doTask("OceanTile<Tharsis_1_5> BY Admin")
        players.forEach { it.autoExecNow(EAGER) }
      }
    }
  }
}
