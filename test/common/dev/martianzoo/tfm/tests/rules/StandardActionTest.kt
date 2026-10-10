package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TfmTest
import dev.martianzoo.tfm.tests.canonicalPremise
import dev.martianzoo.tfm.tests.setUpGame
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StandardActionTest : TfmTest() {
  @Test
  internal fun `a selected project goes straight to payment without a category task`() {
    game = setUpGame(canonicalPremise())
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("20 MC, ProjectCard")
    admin.phase("Action")
    p1.autoExecPolicy = NONE

    p1.beginOperation("NewTurn")
    p1.doTasks("PlayProject<Class<Mine>>", "-4 MC", "Ok", "Mine FROM ProjectCard", "PROD[Steel]")
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `the milestone and award are chosen before paying`() {
    game = setUpGame(canonicalPremise())
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("40 MC, 15 TerraformRating")
    admin.phase("Action")
    p1.autoExecPolicy = NONE

    p1.beginOperation("NewTurn")
    p1.doTasks("ClaimMilestone<Class<Terraformer35>>", "-8 MC", "Terraformer35")
    p1.beginOperation("SecondAction")
    p1.doTasks("FundAward<Class<Banker>>", "-8 MC", "Banker")

    p1.count("MC") shouldBe 24
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `required work replaces the ordinary turn choice`() {
    game = setUpGame(canonicalPremise())
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("Inventrix")
    admin.phase("Action")
    p1.autoExecPolicy = NONE

    p1.beginOperation("NewTurn")
    shouldThrow<TaskException> { p1.doTask("PlayProject<Class<Mine>>") }
    p1.doTasks("RequiredActionsSignal", "3 ProjectCard")

    p1.count("RequiredAction") shouldBe 0
    p1.beginOperation("SecondAction")
    p1.doTasks("UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `using a card directly still consumes its action for the generation`() {
    game = setUpGame(canonicalPremise())
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("DevelopmentCenter, 2 Energy")
    admin.phase("Action")
    p1.autoExecPolicy = NONE

    p1.beginOperation("NewTurn")
    p1.doTasks("UseCardAction<DevelopmentCenter, Action1>", "-Energy", "ProjectCard")
    p1.count("ActionUsedMarker<DevelopmentCenter>") shouldBe 1
    p1.beginOperation("SecondAction")
    shouldThrow<LimitsException> { p1.doTask("UseCardAction<DevelopmentCenter, Action1>") }
    p1.doTask("Ok")

    p1.count("Energy") shouldBe 1
    p1.count("ProjectCard") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `an unqualified milestone claim cannot settle`() {
    game = setUpGame(canonicalPremise())
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("8 MC")
    admin.phase("Action")
    p1.autoExecPolicy = NONE

    p1.beginOperation("NewTurn")
    shouldThrow<RequirementException> {
      p1.doTasks("ClaimMilestone<Class<Terraformer35>>", "-8 MC", "Terraformer35")
    }
    p1.count("Terraformer35") shouldBe 0
    p1.count("MC") shouldBe 8
    p1.doTask("Pass")
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `plant conversion preserves Ecolines discount and leaves tile choice to the player`() {
    game = setUpGame(canonicalPremise())
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("Ecoline, 4 Plant")
    admin.phase("Corporation")
    admin.phase("Action")
    p1.autoExecPolicy = NONE

    p1.beginOperation("NewTurn")
    p1.doTasks("ConvertPlants", "-7 Plant")
    p1.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p1.doTasks(
        "GreeneryTile<Tharsis_1_3>",
        "OxygenStep",
        "TerraformRating",
    )

    p1.count("Plant") shouldBe 0
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `a free milestone and award need no payment choice`() {
    game =
        setUpGame(
            canonicalPremise(
                PreludeExpansion,
                Prelude2CardPack,
            )
        )
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("NirgalEnterprises, 15 TerraformRating")
    admin.phase("Action")
    p1.autoExecPolicy = NONE
    val startingMoney = p1.count("MC")

    p1.beginOperation("NewTurn")
    p1.doTasks("ClaimMilestone<Class<Terraformer35>>", "Terraformer35")
    p1.beginOperation("SecondAction")
    p1.doTasks("FundAward<Class<Banker>>", "Banker")

    p1.count("MC") shouldBe startingMoney
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `a production reward recognizes the selected turn option`() {
    game =
        setUpGame(
            canonicalPremise(
                PreludeExpansion,
                Prelude2CardPack,
            )
        )
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("SuitableInfrastructure, 11 MC")
    admin.phase("Action")
    p1.autoExecPolicy = NONE

    p1.runOperation("NewTurn") {
          doTask("UseStandardProject<PowerPlantProject>")
          doTask("-11 MC")
          doTask("PROD[Energy]")
          p1.selectTask(p1.tasks.ids().single())
          autoExecNow()
          doTask("2 MC")
        }
        .expect("PROD[Energy], -9 MC")
    game.isIdle() shouldBe true
  }
}
