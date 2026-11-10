package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StandardActionTest : TfmSandboxTest() {
  @Test
  internal fun `the standard card action offers the card play and then payment`() {
    newTestGame()
    kim.setToExMachina(20, "MC")
    kim.exMachina("ProjectCard<Selecting>, ProjectCard<Revealed>")

    kim.inTurn {
      kim.doTask("UseAction<PlayCardAction, Action1>")
      shouldThrow<TaskException> {
        kim.doTask("PlayCard<Class<ProjectCard>, Class<Mine>, Selecting>")
      }
      shouldThrow<TaskException> {
        kim.doTask("PlayCard<Class<ProjectCard>, Class<Mine>, Revealed>")
      }
      kim.count("MC") shouldBe 20
      kim.doTasks(
          "PlayCard<Class<ProjectCard>, Class<Mine>, Hand>",
          "4 Pay<> FROM MC",
          "Mine FROM ProjectCard",
          "PROD[Steel]",
      )
    }
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `the milestone and award are chosen after paying`() {
    newTestGame()
    kim.setToExMachina(40, "MC")
    kim.exMachina("15 TerraformRating")

    kim.inTurn {
      kim.doTask("UseAction<ClaimMilestoneAction, Action1>")
      shouldThrow<TaskException> { kim.doTask("Terraformer35") }
      kim.doTasks("8 Pay<> FROM MC", "Terraformer35")
    }
    kim.inTurn {
      kim.doTask("UseAction<FundAwardAction, Action1>")
      shouldThrow<TaskException> { kim.doTask("Banker") }
      kim.doTasks("8 Pay<> FROM MC", "Banker")

      kim.count("MC") shouldBe 24
    }
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `required work replaces the ordinary turn choice`() {
    newTestGame(kimCorporation = Inventrix)

    kim.inTurn {
      shouldThrow<TaskException> {
        kim.doTasks(
            "UseAction<PlayCardAction, Action1>",
            "PlayCard<Class<ProjectCard>, Class<Mine>, Hand>",
        )
      }
      kim.doTasks("DoRequiredActions", "3 ProjectCard")

      kim.count("RequiredAction") shouldBe 0
    }
    kim.inTurn {
      kim.doTasks(
          "UseAction<UseStandardProjectAction, Action1>",
          "UseAction<PowerPlantProject, Action1>",
          "11 Pay<> FROM MC",
          "PROD[Energy]",
      )
    }
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `using a card through the standard action consumes its action for the generation`() {
    newTestGame()
    kim.exMachina("DevelopmentCenter, 2 Energy")
    kim.setToExMachina(0, "ProjectCard")

    kim.inTurn {
      kim.doTasks(
          "UseAction<UseActionOnCardAction, Action1>",
          "UseAction<DevelopmentCenter, Action1>",
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      kim.count("ActionUsedMarker<DevelopmentCenter>") shouldBe 1
    }
    kim.inTurn {
      shouldThrow<LimitsException> {
        kim.doTasks(
            "UseAction<UseActionOnCardAction, Action1>",
            "UseAction<DevelopmentCenter, Action1>",
            "Pay<Class<Energy>> FROM Energy",
            "ProjectCard",
        )
      }
      kim.doTask("Pass")

      kim.count("Energy") shouldBe 1
      kim.count("ProjectCard") shouldBe 1
    }
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `an unqualified milestone claim cannot complete`() {
    newTestGame()
    kim.setToExMachina(8, "MC")

    kim.inTurn {
      shouldThrow<RequirementException> {
        kim.doTasks(
            "UseAction<ClaimMilestoneAction, Action1>",
            "8 Pay<> FROM MC",
            "Terraformer35",
        )
      }
      kim.count("Terraformer35") shouldBe 0
      kim.count("MC") shouldBe 8
      kim.doTask("Pass")
    }
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `plant conversion preserves Ecolines discount and leaves tile choice to the player`() {
    newTestGame(kimCorporation = Ecoline)
    kim.exMachina("4 Plant")

    kim.inTurn {
      kim.doTask("UseAction<ConvertPlantsAction, Action1>")
      shouldThrow<TaskException> { kim.doTask("PlaceGreeneryTile") }
      kim.doTask("7 Pay<Class<Plant>> FROM Plant")
      kim.doTasks(
          "GreeneryTile<Tharsis_1_3>",
          "OxygenStep",
          "TerraformRating",
      )

      kim.count("Plant") shouldBe 0
    }
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `a free milestone and award need no payment choice`() {
    newTestGame(addOptions = "NirgalEnterprises", kimCorporation = NirgalEnterprises)
    kim.exMachina("15 TerraformRating")

    kim.inTurn {
          kim.doTasks("UseAction<ClaimMilestoneAction, Action1>", "Terraformer35")
        }
        .expect("0 MC")
    kim.inTurn {
          kim.doTasks("UseAction<FundAwardAction, Action1>", "Banker")
        }
        .expect("0 MC")
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `a production reward recognizes the selected turn option`() {
    newTestGame(addOptions = "SuitableInfrastructure")
    kim.exMachina("SuitableInfrastructure")
    kim.setToExMachina(11, "MC")

    kim.inTurn {
          doTasks(
              "UseAction<UseStandardProjectAction, Action1>",
              "UseAction<PowerPlantProject, Action1>",
          )
          doTask("11 Pay<> FROM MC")
          doTask("PROD[Energy]")
          doTask("-SuitableInfrastructureBonus!")
          doTask("2 MC")
        }
        .expect("PROD[Energy], -9 MC")
    game.isIdle() shouldBe true
  }
}
