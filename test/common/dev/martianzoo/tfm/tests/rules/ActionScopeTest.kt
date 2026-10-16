package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActionScopeTest : TfmGameplayTest(NONE) {
  @Test
  internal fun `Each accepted action keeps its scope through payment and effects`() {
    newTestGame(playerCount = 2)
    admin.count("ActionScope") shouldBe 0
    kim.count("Baton") shouldBe 1

    kim.completeOperation {
          doTask("UseStandardProject<PowerPlantProject>")
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("11 Pay<> FROM MC")
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("PROD[Energy]")
        }
        .expect("PROD[Energy], -11 MC")
    admin.count("ActionScope") shouldBe 0
    kim.count("Baton") shouldBe 1

    kim.completeOperation {
          doTask("UseStandardProject<PowerPlantProject>")
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("11 Pay<> FROM MC")
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("PROD[Energy]")
        }
        .expect("PROD[Energy], -11 MC")
    admin.count("ActionScope") shouldBe 0

    stan.count("Baton") shouldBe 1
    kim.count("Baton") shouldBe 0
    stan.doTask("UseStandardProject<PowerPlantProject>")
    admin.count("ActionScope<Stan>") shouldBe 1
    admin.count("ActionScope<Kim>") shouldBe 0
  }

  @Test
  internal fun `Declining the second action leaves no scope or pass status`() {
    newTestGame(playerCount = 2)
    kim.completeOperation {
          doTasks("UseStandardProject<PowerPlantProject>", "11 Pay<> FROM MC", "PROD[Energy]")
        }
        .expect("PROD[Energy], -11 MC")
    admin.count("ActionScope") shouldBe 0

    kim.selectTask("-Baton")
    admin.count("ActionScope") shouldBe 0
    kim.count("Pass") shouldBe 0
    stan.doTask("Pass")
    admin.count("ActionScope") shouldBe 0

    kim.doTask("UseStandardProject<PowerPlantProject>")
    admin.count("ActionScope<Kim>") shouldBe 1
  }

  @Test
  internal fun `A rejected third action restores the second actions scope and wild election`() {
    newTestGame("ResearchCoordination", playerCount = 2)
    kim.completeOperation {
          doTasks(
              "PlayProject<Class<ResearchCoordination>>",
              "4 Pay<> FROM MC",
              "ResearchCoordination FROM ProjectCard",
          )
        }
        .expect("ResearchCoordination, -4 MC")
    kim.doTasks("PlayProject<Class<Cartel>>", "8 Pay<> FROM MC", "Cartel FROM ProjectCard")
        .expect("Cartel, -8 MC")
    admin.count("ActionScope<Kim>") shouldBe 1
    kim.count("EarthTag<WildTagElection<WildTag<ResearchCoordination>>>") shouldBe 1

    shouldThrow<TaskException> {
      kim.completeOperation {
        doTask("PROD[2 MC]")
        admin.count("ActionScope") shouldBe 0
        kim.count("WildTagElection") shouldBe 0
        doTask("UseStandardProject<PowerPlantProject>")
      }
    }
    admin.count("ActionScope<Kim>") shouldBe 1
    kim.count("EarthTag<WildTagElection<WildTag<ResearchCoordination>>>") shouldBe 1
    kim.assertProds(1 to "MC")
    kim.count("MC") shouldBe 30

    kim.doTask("PROD[2 MC]").expect("PROD[2 MC]")
    admin.count("ActionScope") shouldBe 0
    kim.count("WildTagElection") shouldBe 0
    kim.count("WildTag") shouldBe 1
    stan.doTask("Pass")
  }

  @Test
  internal fun `Passing leads to production and research without an action scope`() {
    newTestGame(addOptions = "-VenusNextExpansion", playerCount = 2)
    admin.count("ActionScope") shouldBe 0
    kim.doTask("Pass")
    admin.count("ActionScope") shouldBe 0
    stan.doTask("Pass")
    admin.count("ProductionPhase") shouldBe 1
    admin.count("Baton") shouldBe 0
    admin.count("ActionScope") shouldBe 0

    players.forEach { it.autoExecNow(EAGER) }
    admin.count("ResearchPhase") shouldBe 1
    admin.count("Baton") shouldBe 0
    admin.count("ActionScope") shouldBe 0
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("-4 ProjectCard<Selecting>")
    admin.count("ActionScope") shouldBe 0
    stan.doTask("-4 ProjectCard<Selecting>")
    admin.count("ActionPhase") shouldBe 1
    stan.count("Baton") shouldBe 1
    admin.count("ActionScope") shouldBe 0
  }

  @Test
  internal fun `A corporation scope includes its starting project purchase`() {
    newTestGameBeforeCorporationPlay(playerCount = 2)
    kim.doTask("PlayCard<Class<CorporationCard>, Class<CrediCor>>")
    admin.count("ActionScope<Kim>") shouldBe 1
    kim.doTasks("CrediCor FROM CorporationCard", "57 MC")
    admin.count("ActionScope<Kim>") shouldBe 1
    kim.doTask("15 Pay<> FROM MC")
    admin.count("ActionScope<Kim>") shouldBe 1
    kim.doTask("5 ProjectCard<Hand FROM Selecting>")
    admin.count("ActionScope") shouldBe 0

    stan.doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation2>, Hand>")
    admin.count("ActionScope<Stan>") shouldBe 1
    admin.count("ActionScope<Kim>") shouldBe 0
  }

  @Test
  internal fun `New Partners nested Prelude stays inside the original scope`() {
    newTestGame(addOptions = "PreludeExpansion", playerCount = 2)
    kim.completeOperation {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<NewPartner>>",
              "NewPartner FROM PreludeCard",
              "PROD[MC]",
              "2 PreludeCard<Selecting>",
          )
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("PlayCard<Class<PreludeCard>, Class<Donation>>")
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("Donation FROM PreludeCard<Selecting>")
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("21 MC")
        }
        .expect("NewPartner, Donation, PROD[MC], 21 MC")
    admin.count("ActionScope") shouldBe 0
    kim.count("PreludeCard<Hand>") shouldBe 1

    kim.doTask("PlayCard<Class<PreludeCard>, Class<PowerGeneration>>")
    admin.count("ActionScope<Kim>") shouldBe 1
  }

  @Test
  internal fun `Mergers corporation resolves inside its Prelude scope`() {
    newTestGame(addOptions = "PreludeExpansion", playerCount = 2)
    kim.completeOperation {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<Merger>>",
              "Merger FROM PreludeCard",
              "4 CorporationCard<Selecting>",
              "-42 MC",
          )
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("PlayCard<Class<CorporationCard>, Class<CrediCor>>")
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("CrediCor FROM CorporationCard<Selecting>")
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("57 MC")
        }
        .expect("Merger, CrediCor, 15 MC")
    admin.count("ActionScope") shouldBe 0
    kim.count("CorporationCard<Selecting>") shouldBe 0
    kim.count("PreludeCard<Hand>") shouldBe 1
  }

  @Test
  internal fun `Another players Philares reward keeps the placing players scope open`() {
    newTestGame(playerCount = 2, kimCorporation = Philares)
    kim.completeOperation {
          doTasks(
              "DoRequiredActions",
              "GreeneryTile<Tharsis_4_2>",
              "OxygenStep",
              "Plant",
              "TerraformRating",
          )
        }
        .expect("GreeneryTile, OxygenStep, Plant, TerraformRating")
    kim.selectTask("-Baton")

    stan.doTasks(
        "UseStandardProject<GreeneryProject>",
        "23 Pay<> FROM MC",
        "GreeneryTile<Tharsis_3_2>",
        "OxygenStep",
        "TerraformRating",
    )
    admin.count("ActionScope<Stan>") shouldBe 1
    admin.count("ActionScope<Kim>") shouldBe 0
    shouldThrow<TaskException> { stan.doTask("UseStandardProject<PowerPlantProject>") }

    kim.doTask("Titanium").expect("Titanium")
    admin.count("ActionScope") shouldBe 0
    stan.doTask("UseStandardProject<PowerPlantProject>")
    admin.count("ActionScope<Stan>") shouldBe 1
  }
}
