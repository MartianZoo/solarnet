package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

// Terraforming Mars Comprehensive FAQ 1.8, Self-Replicating Robots entry, pages 72–74.
internal class SelfReplicatingRobotsTest : TfmSandboxTest() {
  @Test
  internal fun `Action can be used only once per generation`() {
    initialize(2)

    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            "UseCardAction<FakeSelfReplicatingRobots, Action1>",
            "StageForReplicatedProject<Class<TitaniumMine>>",
            "-ProjectCard<Kim, Hand>",
            "2 RobotUnit<Class<TitaniumMine>>",
        )
      }
    }

    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<TitaniumMine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<TitaniumMine>>")
    }
  }

  @Test
  internal fun `Action can stage a card with two resources or double its resources`() {
    initialize(1)

    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    kim.assertCounts(2 to "RobotUnit<Class<$Mine>>")

    advanceUntilActionPhase()
    kim.inTurn {
      doTask("UseCardAction<FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<Class<Mine>>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    kim.assertCounts(4 to "RobotUnit<Class<$Mine>>")
  }

  @Test
  internal fun `Staged cards retain independent robot unit counts`() {
    initialize(5)

    stage("Mine")
    advanceUntilActionPhase()
    stage("TitaniumMine")
    advanceUntilActionPhase()
    stage("MartianRails")
    advanceUntilActionPhase()
    stage("SpaceStation")
    advanceUntilActionPhase()
    stage("PowerPlant")

    kim.assertCounts(
        0 to "ProjectCard",
        10 to "RobotUnit",
        2 to "RobotUnit<Class<Mine>>",
        2 to "RobotUnit<Class<TitaniumMine>>",
        2 to "RobotUnit<Class<MartianRails>>",
        2 to "RobotUnit<Class<SpaceStation>>",
        2 to "RobotUnit<Class<PowerPlant>>",
    )
  }

  @Test
  internal fun `Doubling chooses one card rather than every card`() {
    initialize(2)
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<TitaniumMine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<TitaniumMine>>")
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<Mine>>",
      )
      doTask("2 RobotUnit<Class<Mine>>")
    }

    kim.assertCounts(
        4 to "RobotUnit<Class<$Mine>>",
        2 to "RobotUnit<Class<$TitaniumMine>>",
    )
  }

  @Test
  internal fun `More than five cards can be staged`() {
    initialize(6)
    stage("Mine")
    advanceUntilActionPhase()
    stage("TitaniumMine")
    advanceUntilActionPhase()
    stage("MartianRails")
    advanceUntilActionPhase()
    stage("SpaceStation")
    advanceUntilActionPhase()
    stage("PowerPlant")
    advanceUntilActionPhase()
    stage("VestaShipyard")

    kim.assertCounts(0 to "ProjectCard", 12 to "RobotUnit")
  }

  @Test
  internal fun `Staged cards remain outside hand for Planner`() {
    newTestGame(addOptions = "fakes:cards")
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(16, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }

    kim.count("ProjectCard") shouldBe 15
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Planner>>")
        doTask("8 Pay<> FROM MC")
        doTask("Planner")
      }
    }
  }

  @Test
  internal fun `Staged cards remain outside hand for Visionary`() {
    newTestGame(addOptions = "fakes:cards, Visionary, Landlord, Banker", playerCount = 2)
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(2, "ProjectCard")
    stan.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }

    kim.inTurn {
      doTasks("FundAward<Class<Visionary>>", "8 Pay<> FROM MC")
      doTask("Visionary")
    }
    victoryPoints() shouldBe listOf(20, 25)
  }

  @Test
  internal fun `Sell Patents cannot sell a staged card`() {
    initialize(2)
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }

    kim.inTurn {
          doTasks("UseStandardProject<SellPatentsProject>", "1! MC FROM ProjectCard")
        }
        .expect("0 RobotUnit")

    kim.count("ProjectCard") shouldBe 0
    kim.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Excentric ignores resources on a card that is not in play`() {
    newTestGame(addOptions = "HellasMap, fakes:cards", playerCount = 2)
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(1, "ProjectCard")
    stan.exMachina("$SearchForLife, Science<$SearchForLife>")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }

    kim.inTurn {
      doTasks("FundAward<Class<Excentric>>", "8 Pay<> FROM MC")
      doTask("Excentric")
    }
    victoryPoints() shouldBe listOf(20, 28)
  }

  @Test
  internal fun `A card may be staged before its play requirement is met`() {
    initialize(1)
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<DiversitySupport>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<DiversitySupport>>")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PlayProject<Class<DiversitySupport>>")
      }
    }

    kim.assertCounts(0 to "$DiversitySupport", 2 to "RobotUnit<Class<$DiversitySupport>>")
  }

  @Test
  internal fun `Staging a card does not fire its play effects or triggers`() {
    newTestGame(addOptions = "fakes:cards")
    kim.exMachina("$FakeSelfReplicatingRobots, PROD[2 MC, Energy]")
    kim.setToExMachina(1, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<ImmigrantCity>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<ImmigrantCity>>")
    }
    advanceUntilActionPhase()
    replicateImmigrantCity()
    advanceUntilActionPhase()
    replicateImmigrantCity()
    advanceUntilActionPhase()
    replicateImmigrantCity()

    kim.assertProds(3 to "MC", 2 to "Energy")
    kim.count("CityTile") shouldBe 0

    kim.inTurn {
      doTask("PlayProject<Class<ImmigrantCity>>")
      doTask("ProjectCard<Kim, Hand>")
      // Hand the discount to Admin before the remaining payment is offered.
      selectTask("-16* Owed")
      doTask("-16 RobotUnit<Kim, Class<ImmigrantCity>>")
      doTask("Ok") // The discount covers the full cost.
      doTasks("ImmigrantCity FROM ProjectCard", "PROD[-Energy]", "PROD[-2 MC]")
      doTasks("CityTile<Tharsis_7_4>", "PROD[MC]")
    }

    kim.assertProds(2 to "MC", 1 to "Energy")
    kim.count("CityTile") shouldBe 1
  }

  @Test
  internal fun `Resources reduce a staged cards play cost one MC each`() {
    initialize(1)
    kim.setToExMachina(2, "MC")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }

    kim.inTurn {
      doTask("PlayProject<Class<Mine>>")
      doTask("ProjectCard<Kim, Hand>")
      // Hand the discount to Admin before the remaining payment is offered.
      selectTask("-2* Owed")
      doTask("-2 RobotUnit<Kim, Class<Mine>>")
      doTask("2 Pay<> FROM MC")
      doTasks("Mine FROM ProjectCard", "PROD[Steel]")
    }

    kim.assertCounts(0 to "ProjectCard", 1 to "$Mine")
  }

  @Test
  internal fun `A staged card discount cannot reduce its cost below zero`() {
    initialize(1)
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTask("UseCardAction<FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<Class<Mine>>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTask("UseCardAction<FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<Class<Mine>>")
      doTask("4 RobotUnit<Class<Mine>>")
    }

    kim.inTurn {
          doTask("PlayProject<Class<Mine>>")
          doTask("ProjectCard<Kim, Hand>")
          // Hand the discount to Admin before the remaining payment is offered.
          selectTask("-8* Owed")
          doTask("-8 RobotUnit<Kim, Class<Mine>>")
          doTask("Ok") // The discount covers the full cost.
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("0 MC")

    kim.assertCounts(
        1 to "$Mine",
        0 to "RobotUnit<Class<$Mine>>",
    )
  }

  @Test
  internal fun `Playing one staged card discards only that cards resources`() {
    initialize(2)
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<TitaniumMine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<TitaniumMine>>")
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<Mine>>",
      )
      doTask("2 RobotUnit<Class<Mine>>")
    }

    kim.inTurn {
          doTask("PlayProject<Class<Mine>>")
          doTask("ProjectCard<Kim, Hand>")
          // Hand the discount to Admin before the remaining payment is offered.
          selectTask("-4* Owed")
          doTask("-4 RobotUnit<Kim, Class<Mine>>")
          doTask("Ok") // The discount covers the full cost.
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("0 MC")

    kim.assertCounts(
        1 to "$Mine",
        0 to "ProjectCard",
        0 to "RobotUnit<Class<$Mine>>",
        2 to "RobotUnit<Class<$TitaniumMine>>",
    )
  }

  @Test
  internal fun `Viron can stage a second card in the same generation`() {
    initialize(2)
    kim.exMachina("$Viron")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }

    kim.inTurn {
      doTasks(
          "UseCardAction<Viron, Action1>",
          "UseAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<TitaniumMine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<TitaniumMine>>")
    }

    kim.assertCounts(
        0 to "ProjectCard",
        2 to "RobotUnit<Class<$Mine>>",
        2 to "RobotUnit<Class<$TitaniumMine>>",
    )
  }

  @Test
  internal fun `Viron can stage and then double that card in the same generation`() {
    initialize(1)
    kim.exMachina("$Viron")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }

    kim.inTurn {
      doTasks("UseCardAction<Viron, Action1>", "UseAction<FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<Class<Mine>>")
      doTask("2 RobotUnit<Class<Mine>>")
    }

    kim.count("RobotUnit<Class<$Mine>>") shouldBe 4
  }

  @Test
  internal fun `Viron can double a staged card twice in one generation`() {
    initialize(1)
    kim.exMachina("$Viron")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTask("UseCardAction<FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<Class<Mine>>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    kim.inTurn {
      doTasks("UseCardAction<Viron, Action1>", "UseAction<FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<Class<Mine>>")
      doTask("4 RobotUnit<Class<Mine>>")
    }

    kim.count("RobotUnit<Class<$Mine>>") shouldBe 8
  }

  @Test
  internal fun `Viron can double two different staged cards`() {
    initialize(2)
    kim.exMachina("$Viron")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<TitaniumMine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<TitaniumMine>>")
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<Mine>>",
      )
      doTask("2 RobotUnit<Class<Mine>>")
    }
    kim.inTurn {
      doTasks(
          "UseCardAction<Viron, Action1>",
          "UseAction<FakeSelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<TitaniumMine>>",
      )
      doTask("2 RobotUnit<Class<TitaniumMine>>")
    }

    kim.assertCounts(
        4 to "RobotUnit<Class<$Mine>>",
        4 to "RobotUnit<Class<$TitaniumMine>>",
    )
  }

  private fun initialize(cards: Int) {
    newTestGame(addOptions = "fakes:cards")
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(cards, "ProjectCard")
  }

  private fun stage(card: String) {
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<$card>>",
          "-ProjectCard",
          "2 RobotUnit<Class<$card>>",
      )
    }
  }

  private fun replicateImmigrantCity() {
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<ImmigrantCity>>",
          "RobotUnit<Class<ImmigrantCity>> / RobotUnit<Class<ImmigrantCity>>",
      )
    }
  }

  @Test
  internal fun `Sponsored Academies cannot discard a card hosted on Self-Replicating Robots`() {
    initialize(2)
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    kim.count("ProjectCard") shouldBe 1

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<SponsoredAcademies>>")
        doTasks("9 Pay<> FROM MC", "SponsoredAcademies FROM ProjectCard")
        // Expand the reward into separate tasks for each other player.
        selectTask("EACH Other@Player(NOT Kim) { ProjectCard<Other@Player, Hand>! }")
        doTask("ProjectCard<Stan, Hand>")
        doTask("ProjectCard<Rob, Hand>")
        doTask("-ProjectCard<Kim, Hand>")
      }
    }
    kim.count("ProjectCard") shouldBe 1
    kim.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Mars University cannot discard a hosted card when a staged science card is played`() {
    initialize(2)
    kim.exMachina("$MarsUniversity")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<ResearchOutpost>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<ResearchOutpost>>")
    }

    kim.inTurn {
          doTask("PlayProject<Class<ResearchOutpost>>")
          doTask("ProjectCard<Kim, Hand>")
          // Hand the discount to Admin before the remaining payment is offered.
          selectTask("-2* Owed")
          doTask("-2 RobotUnit<Kim, Class<ResearchOutpost>>")
          doTask("16 Pay<> FROM MC")
          doTasks("ResearchOutpost FROM ProjectCard")
          // Resolve the exchange offer: the empty hand leaves no card to discard.
          selectTask("(-ProjectCard<Kim, Hand>! THEN ProjectCard<Kim, Hand>!) OR Ok")
          doTasks("CityTile<Tharsis_4_2>", "Plant")
        }
        .expect("0 ProjectCard, 0 RobotUnit<Class<$Mine>>")
    kim.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Ignore // Robot units do not expose the hosted Venus card as a resource destination.
  @Test
  internal fun `Corroder Suits adds a resource to a hosted Venus card`() {
    playCorroderWithHostedVenusCard().expect("RobotUnit<Class<$VenusWaystation>>")
  }

  @Test
  internal fun `BUG - Corroder Suits skips a hosted Venus card`() {
    playCorroderWithHostedVenusCard().expect("0 RobotUnit<Class<$VenusWaystation>>")
  }

  private fun playCorroderWithHostedVenusCard(): TaskResult {
    newTestGame(addOptions = "fakes:cards")
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<VenusWaystation>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<VenusWaystation>>")
    }
    return kim.inTurn {
      doTasks(
          "PlayProject<Class<CorroderSuits>>",
          "8 Pay<> FROM MC",
          "CorroderSuits FROM ProjectCard",
          "PROD[2 MC]",
      )
      // Resolve the resource offer: the hosted Venus card is not an eligible destination.
      selectTask("CardResource")
    }
  }

  @Ignore // RobotUnit is counted as a distinct resource type.
  @Test
  internal fun `Robot units do not satisfy Diversity Support's ninth resource type`() {
    shouldThrow<RequirementException> { playDiversityWithRobotUnits() }
    kim.assertCounts(10 to "MC", 1 to "ProjectCard", 0 to "$DiversitySupport", 2 to "RobotUnit")
  }

  @Test
  internal fun `BUG - Robot units satisfy Diversity Support's ninth resource type`() {
    playDiversityWithRobotUnits().expect("TerraformRating")
  }

  private fun setUpEightResourceTypesWithRobots() {
    newTestGame(addOptions = "AmazonisMap, fakes:cards")
    val standardResources = "MC, Steel, Titanium, Plant, Energy, Heat"
    kim.setToExMachina(2, "ProjectCard")
    kim.exMachina(
        "$FakeSelfReplicatingRobots, $standardResources, " +
            "$Pets, $Decomposers, Animal<$Pets>, Microbe<$Decomposers>"
    )
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<AerialMappers>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<AerialMappers>>")
    }
    kim.setToExMachina(10, "MC")
  }

  private fun playDiversityWithRobotUnits(): TaskResult {
    setUpEightResourceTypesWithRobots()
    return kim.inTurn {
      doTasks(
          "PlayProject<Class<DiversitySupport>>",
          "1 Pay<> FROM MC",
          "DiversitySupport FROM ProjectCard",
          "TerraformRating",
      )
    }
  }

  @Ignore // Collector counts the RobotUnit resource type.
  @Test
  internal fun `Robot units do not break a tie for Collector`() {
    scoreCollectorWithRobotUnits() shouldBe listOf(25, 26, 20)
  }

  @Test
  internal fun `BUG - Robot units break a tie for Collector`() {
    fundCollectorWithRobotUnits()
    val checkpoint = game.timeline.checkpoint()

    victoryPoints {
      kim.auditGainsSince(checkpoint) shouldBe 1
      stan.auditGainsSince(checkpoint) shouldBe 0
    } shouldBe listOf(25, 23, 20)
  }

  @Test
  internal fun `Second-place Collector scoring with robot units records an Audit`() {
    fundCollectorWithRobotUnits()
    kim.exMachina("-Plant, -Heat")
    val checkpoint = game.timeline.checkpoint()

    victoryPoints {
      kim.auditGainsSince(checkpoint) shouldBe 1
      stan.auditGainsSince(checkpoint) shouldBe 0
    } shouldBe listOf(22, 26, 20)
  }

  private fun scoreCollectorWithRobotUnits(): List<Int> {
    fundCollectorWithRobotUnits()
    return victoryPoints()
  }

  private fun fundCollectorWithRobotUnits() {
    setUpEightResourceTypesWithRobots()
    stan.exMachina(
        "MC, Steel, Titanium, Plant, Energy, Heat, $Predators, $RegolithEaters, " +
            "Animal<$Predators>, Microbe<$RegolithEaters>"
    )
    kim.inTurn {
      doTasks("FundAward<Class<Collector>>", "8 Pay<> FROM MC")
      doTask("Collector")
    }
  }

  @Ignore // Hosted cards are not resource destinations.
  @Test
  internal fun `Maxwell Base can add to a hosted Venus card`() {
    useMaxwellWithHostedVenusCard { doTask("RobotUnit<Class<$VenusWaystation>>") }
        .expect("RobotUnit<Class<$VenusWaystation>>, 0 Floater<$AerialMappers>")
  }

  @Test
  internal fun `BUG - Maxwell Base rejects a hosted Venus card`() {
    useMaxwellWithHostedVenusCard {
          shouldThrow<TaskException> { doTask("RobotUnit<Class<$VenusWaystation>>") }
          doTask("Floater<AerialMappers>")
        }
        .expect("0 RobotUnit, Floater<$AerialMappers>")
  }

  private fun useMaxwellWithHostedVenusCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "fakes:cards")
    kim.exMachina("$FakeSelfReplicatingRobots, " + "$AerialMappers, Floater<$AerialMappers>")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<VenusWaystation>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<VenusWaystation>>")
    }
    kim.exMachina("$MaxwellBase")
    return kim.inTurn {
      doTask("UseCardAction<MaxwellBase, Action1>")
      choice()
    }
  }

  @Ignore // Hosted cards are not resource destinations.
  @Test
  internal fun `CEO's Favorite Project can add to a hosted card`() {
    playCeosFavoriteWithHostedCard { doTask("RobotUnit<Class<$Mine>>") }
        .expect("RobotUnit<Class<$Mine>>, 0 Animal<$Pets>")
  }

  @Test
  internal fun `BUG - CEO's Favorite Project rejects a hosted card`() {
    playCeosFavoriteWithHostedCard {
          shouldThrow<NarrowingException> { doTask("RobotUnit<Class<$Mine>>") }
          doTask("Animal<Pets>")
        }
        .expect("0 RobotUnit, Animal<$Pets>")
  }

  private fun playCeosFavoriteWithHostedCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "fakes:cards")
    kim.exMachina("$FakeSelfReplicatingRobots, $Pets, Animal<$Pets>")
    kim.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    return kim.inTurn {
      doTasks(
          "PlayProject<Class<CeosFavoriteProject>>",
          "1 Pay<> FROM MC",
          "CeosFavoriteProject FROM ProjectCard",
      )
      // Resolve the resource offer: no eligible card already holds a resource.
      selectTask("CardResource<CardFront(HAS CardResource)>*")
      choice()
    }
  }

  @Ignore // Hosted cards are not resource destinations.
  @Test
  internal fun `Applied Science can add to a hosted card`() {
    useAppliedScienceWithHostedCard { doTask("RobotUnit<Class<$Mine>>") }
        .expect("RobotUnit<Class<$Mine>>")
  }

  @Test
  internal fun `BUG - Applied Science rejects a hosted card`() {
    useAppliedScienceWithHostedCard {
          shouldThrow<TaskException> { doTask("RobotUnit<Class<$Mine>>") }
          doTask("Steel")
        }
        .expect("0 RobotUnit")
  }

  private fun useAppliedScienceWithHostedCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "fakes:cards, AppliedScience")
    kim.exMachina("$FakeSelfReplicatingRobots, $AppliedScience, Science<$AppliedScience>")
    kim.setToExMachina(1, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    return kim.inTurn {
      doTask("UseCardAction<AppliedScience, Action1>")
      doTask("-Science<AppliedScience>")
      choice()
    }
  }

  @Ignore // Hosted cards are not resource destinations.
  @Test
  internal fun `L1 Trade Terminal can add to a hosted card`() {
    playTerminalWithHostedCard {
          doTasks("RobotUnit<Class<$Mine>>", "Animal<Pets>")
        }
        .expect("RobotUnit<Class<$Mine>>, Animal<$Pets>")
  }

  @Test
  internal fun `BUG - L1 Trade Terminal rejects a hosted card`() {
    playTerminalWithHostedCard {
          shouldThrow<TaskException> { doTask("RobotUnit<Class<$Mine>>") }
          doTask("Animal<Pets>")
        }
        .expect("0 RobotUnit, Animal<$Pets>")
  }

  private fun playTerminalWithHostedCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "prelude2:cards, ColoniesExpansion, fakes:cards")
    kim.exMachina("$FakeSelfReplicatingRobots, $Pets, Animal<$Pets>")
    kim.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<Mine>>")
    }
    return kim.inTurn {
      doTasks("PlayProject<Class<L1TradeTerminal>>", "25 Pay<> FROM MC")
      doTasks("L1TradeTerminal FROM ProjectCard")
      // Expand the bonus into a separate resource choice for each eligible card.
      selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
      choice()
    }
  }

  // https://boardgamegeek.com/thread/1861808/article/28914878#28914878
  @Ignore // Fake SRR delegates printed-tag eligibility to the caller.
  @Test
  internal fun `Cannot host a card without a Building or Space tag`() {
    shouldThrow<RequirementException> { stageCardWithoutEligibleTag() }
    kim.assertCounts(1 to "ProjectCard", 0 to "RobotUnit")
  }

  @Test
  internal fun `BUG - Can host a card without a Building or Space tag`() {
    stageCardWithoutEligibleTag().expect("2 RobotUnit<Class<$CeosFavoriteProject>>")
    kim.inTurn {
          doTask("PlayProject<Class<CeosFavoriteProject>>")
          doTask("ProjectCard<Kim, Hand>")
          // Hand the discount to Admin before the remaining payment is offered.
          selectTask("-2* Owed")
          doTask("-2 RobotUnit<Kim, Class<CeosFavoriteProject>>")
          doTask("Ok") // The discount covers the full cost.
          doTasks(
              "CeosFavoriteProject FROM ProjectCard",
          )
          // Select the resource offer before testing whether a required gain can be declined.
          selectTask("CardResource<CardFront(HAS CardResource)>*")
        }
        .expect("PlayedEvent<Class<$CeosFavoriteProject>>")
  }

  private fun stageCardWithoutEligibleTag(): TaskResult {
    newTestGame(addOptions = "fakes:cards")
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(1, "ProjectCard")
    return kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<CeosFavoriteProject>>",
      )
      doTask("-ProjectCard<Kim, Hand>")
      doTask("2 RobotUnit<Class<CeosFavoriteProject>>")
    }
  }

  internal class Gameplay : TfmGameplayTest() {
    // https://boardgamegeek.com/thread/2334454/article/33634574#33634574
    @Ignore // Hosted cards are not resource destinations.
    @Test
    internal fun `Sponsored Projects adds to a hosted card`() {
      resolveSponsoredProjectsWithHostedCard() shouldBe (3 to 2)
    }

    @Test
    internal fun `BUG - Sponsored Projects skips a hosted card`() {
      resolveSponsoredProjectsWithHostedCard() shouldBe (2 to 2)
    }

    private fun resolveSponsoredProjectsWithHostedCard(): Pair<Int, Int> {
      newTestGame(addOptions = "TurmoilExpansion, fakes:cards", playerCount = 2)
      kim.completeOperation {
        doTasks("PlayProject<Class<$Research>>")
        autoExecNow(EAGER)
      }
      kim.doTasks("PlayProject<Class<$FakeSelfReplicatingRobots>>")
      stan.doTask("Pass")
      kim.completeOperation {
        doTask("UseCardAction<$FakeSelfReplicatingRobots, Action1>")
        doTask("StageForReplicatedProject<Class<$Mine>>")
      }
      kim.doTasks("PlayProject<Class<$Pets>>")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("PartyLeader<Greens, Neutral>")
      admin.doTask("ScienceSummit")
      admin.doTask("PartyLeader<Scientists, Neutral>")
      kim.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("-4 ProjectCard<Selecting>")

      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      stan.doTask("OceanTile<Tharsis_1_5> BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("PartyLeader<Unity, Neutral>")
      admin.doTask("ExploreFirstDirective")
      admin.doTask("PartyLeader<Reds, Neutral>")
      kim.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("-4 ProjectCard<Selecting>")

      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("PartyLeader<Greens, Neutral>")
      admin.doTask("MoralMovement")
      admin.doTask("PartyFollower<Reds, Neutral>")
      kim.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("-4 ProjectCard<Selecting>")

      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // The game resolves the event after the world-government action returns.
      return kim.count("RobotUnit<Class<$Mine>>") to kim.count("Animal<$Pets>")
    }
  }
}
