package dev.martianzoo.tfm.tests.cards

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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<FakeSelfReplicatingRobots, Action1>")
      }
    }

    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<TitaniumMine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    kim.assertCounts(2 to "RobotUnit<Class<$Mine>>")

    advanceUntilActionPhase()
    kim.inTurn {
      doTask("UseCardAction<FakeSelfReplicatingRobots, Action2>")
      selectOnlyTask()
      selectOnlyTask()
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<TitaniumMine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<Mine>>",
      )
      selectOnlyTask()
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
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(16, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }

    kim.count("ProjectCard") shouldBe 15
    shouldThrow<RequirementException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("ClaimMilestone<Class<Planner>>")
        selectTask("-X MC?")
        doTask("-MC / Owed<Class<MC>>")
        selectOnlyTask()
      }
    }
  }

  @Test
  internal fun `Staged cards remain outside hand for Visionary`() {
    newTestGame(addOptions = "FakeStuffBundle, Visionary, Landlord, Banker", playerCount = 2)
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(2, "ProjectCard")
    stan.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }

    kim.inTurn {
      doTasks("FundAward<Class<Visionary>>", "-8 MC")
      selectOnlyTask()
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }

    kim.inTurn {
          doTasks("UseStandardProject<SellPatentsProject>", "1 MC FROM ProjectCard!")
        }
        .expect("0 RobotUnit")

    kim.count("ProjectCard") shouldBe 0
    kim.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Excentric ignores resources on a card that is not in play`() {
    newTestGame(addOptions = "HellasMap, FakeStuffBundle", playerCount = 2)
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(1, "ProjectCard")
    stan.exMachina("$SearchForLife, Science<$SearchForLife>")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }

    kim.inTurn {
      doTasks("FundAward<Class<Excentric>>", "-8 MC")
      selectOnlyTask()
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<DiversitySupport>>")
      }
    }

    kim.assertCounts(0 to "$DiversitySupport", 2 to "RobotUnit<Class<$DiversitySupport>>")
  }

  @Test
  internal fun `Staging a card does not fire its play effects or triggers`() {
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots, PROD[2 MC, Energy]")
    kim.setToExMachina(1, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<ImmigrantCity>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
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
      selectTask("ProjectCard<Kim, Hand>!")
      selectTask("-16 Owed.")
      selectTask("-16 RobotUnit<Kim, Class<ImmigrantCity>>!")
      doTasks("ImmigrantCity FROM ProjectCard", "PROD[-Energy]", "PROD[-2 MC]")
      selectTask("-X MC?")
      doTask("Ok")
      selectTask("-X Steel?")
      doTasks("Ok", "CityTile<Tharsis_7_4>", "PROD[MC]")
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }

    kim.inTurn {
      doTask("PlayProject<Class<Mine>>")
      selectTask("ProjectCard<Kim, Hand>!")
      selectTask("-2 Owed.")
      selectTask("-2 RobotUnit<Kim, Class<Mine>>!")
      doTask("-2 MC")
      selectTask("-X Steel?")
      doTasks("Ok", "Mine FROM ProjectCard", "PROD[Steel]")
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTask("UseCardAction<FakeSelfReplicatingRobots, Action2>")
      selectOnlyTask()
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTask("UseCardAction<FakeSelfReplicatingRobots, Action2>")
      selectOnlyTask()
      selectOnlyTask()
    }

    kim.inTurn {
          doTask("PlayProject<Class<Mine>>")
          selectTask("ProjectCard<Kim, Hand>!")
          selectTask("-8 Owed.")
          selectTask("-8 RobotUnit<Kim, Class<Mine>>!")
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
          selectTask("-X MC?")
          doTask("Ok")
          selectTask("-X Steel?")
          doTask("Ok")
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<TitaniumMine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<Mine>>",
      )
      selectOnlyTask()
    }

    kim.inTurn {
          doTask("PlayProject<Class<Mine>>")
          selectTask("ProjectCard<Kim, Hand>!")
          selectTask("-4 Owed.")
          selectTask("-4 RobotUnit<Kim, Class<Mine>>!")
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
          selectTask("-X MC?")
          doTask("Ok")
          selectTask("-X Steel?")
          doTask("Ok")
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }

    kim.inTurn {
      doTasks(
          "UseCardAction<Viron, Action1>",
          "UseAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<TitaniumMine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }

    kim.inTurn {
      doTasks("UseCardAction<Viron, Action1>", "UseAction<FakeSelfReplicatingRobots, Action2>")
      selectOnlyTask()
      selectOnlyTask()
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTask("UseCardAction<FakeSelfReplicatingRobots, Action2>")
      selectOnlyTask()
      selectOnlyTask()
    }
    kim.inTurn {
      doTasks("UseCardAction<Viron, Action1>", "UseAction<FakeSelfReplicatingRobots, Action2>")
      selectOnlyTask()
      selectOnlyTask()
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<TitaniumMine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<Mine>>",
      )
      selectOnlyTask()
    }
    kim.inTurn {
      doTasks(
          "UseCardAction<Viron, Action1>",
          "UseAction<FakeSelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<TitaniumMine>>",
      )
      selectOnlyTask()
    }

    kim.assertCounts(
        4 to "RobotUnit<Class<$Mine>>",
        4 to "RobotUnit<Class<$TitaniumMine>>",
    )
  }

  private fun initialize(cards: Int) {
    newTestGame(addOptions = "FakeStuffBundle")
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    kim.count("ProjectCard") shouldBe 1

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<SponsoredAcademies>>")
        selectTask("-X MC?")
        doTasks("-9 MC", "SponsoredAcademies FROM ProjectCard")
        selectTask("EACH Other@Player(NOT Kim) { ProjectCard<Other@Player, Hand>! }")
        selectTask("ProjectCard<Stan, Hand>!")
        selectTask("ProjectCard<Rob, Hand>!")
        selectTask("-ProjectCard<Kim, Hand>!")
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<ResearchOutpost>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }

    kim.inTurn {
          doTask("PlayProject<Class<ResearchOutpost>>")
          selectTask("ProjectCard<Kim, Hand>!")
          selectTask("-2 Owed.")
          selectTask("-2 RobotUnit<Kim, Class<ResearchOutpost>>!")
          doTask("-16 MC")
          selectTask("-X Steel?")
          doTasks("Ok", "ResearchOutpost FROM ProjectCard")
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
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<VenusWaystation>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    return kim.inTurn {
      doTasks(
          "PlayProject<Class<CorroderSuits>>",
          "-8 MC",
          "CorroderSuits FROM ProjectCard",
          "PROD[2 MC]",
      )
      selectOnlyTask()
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
    newTestGame(addOptions = "AmazonisMap, FakeStuffBundle")
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
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    kim.setToExMachina(10, "MC")
  }

  private fun playDiversityWithRobotUnits(): TaskResult {
    setUpEightResourceTypesWithRobots()
    return kim.inTurn {
      doTasks(
          "PlayProject<Class<DiversitySupport>>",
          "-1 MC",
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
    scoreCollectorWithRobotUnits() shouldBe listOf(25, 23, 20)
  }

  private fun scoreCollectorWithRobotUnits(): List<Int> {
    setUpEightResourceTypesWithRobots()
    stan.exMachina(
        "MC, Steel, Titanium, Plant, Energy, Heat, $Predators, $RegolithEaters, " +
            "Animal<$Predators>, Microbe<$RegolithEaters>"
    )
    kim.inTurn {
      doTasks("FundAward<Class<Collector>>", "-8 MC")
      selectOnlyTask()
    }
    return victoryPoints()
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
          shouldThrow<NarrowingException> { doTask("RobotUnit<Class<$VenusWaystation>>") }
          doTask("Floater<AerialMappers>")
        }
        .expect("0 RobotUnit, Floater<$AerialMappers>")
  }

  private fun useMaxwellWithHostedVenusCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots, " + "$AerialMappers, Floater<$AerialMappers>")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<VenusWaystation>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    kim.exMachina("$MaxwellBase")
    return kim.inTurn {
      doTask("UseCardAction<MaxwellBase, Action1>")
      selectOnlyTask()
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
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots, $Pets, Animal<$Pets>")
    kim.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    return kim.inTurn {
      doTasks(
          "PlayProject<Class<CeosFavoriteProject>>",
          "-1 MC",
          "CeosFavoriteProject FROM ProjectCard",
      )
      selectTask("CardResource<CardFront(HAS CardResource)>.")
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
          shouldThrow<NarrowingException> { doTask("RobotUnit<Class<$Mine>>") }
          doTask("Steel")
        }
        .expect("0 RobotUnit")
  }

  private fun useAppliedScienceWithHostedCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots, $FakeAppliedScience, Science<$FakeAppliedScience>")
    kim.setToExMachina(1, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    return kim.inTurn {
      doTask("UseCardAction<FakeAppliedScience, Action1>")
      selectOnlyTask()
      selectOnlyTask()
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
    newTestGame(addOptions = "prelude2:cards, ColoniesExpansion, FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots, $Pets, Animal<$Pets>")
    kim.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<Mine>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
    }
    return kim.inTurn {
      doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
      selectTask("-X Titanium?")
      doTasks("Ok", "L1TradeTerminal FROM ProjectCard")
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
          selectTask("ProjectCard<Kim, Hand>!")
          selectTask("-2 Owed.")
          selectTask("-2 RobotUnit<Kim, Class<CeosFavoriteProject>>!")
          doTask("CeosFavoriteProject FROM ProjectCard")
          selectTask("CardResource<CardFront(HAS CardResource)>.")
          selectTask("-X MC?")
          doTask("Ok")
        }
        .expect("PlayedEvent<Class<$CeosFavoriteProject>>")
  }

  private fun stageCardWithoutEligibleTag(): TaskResult {
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(1, "ProjectCard")
    return kim.inTurn {
      doTasks(
          "UseCardAction<FakeSelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<CeosFavoriteProject>>",
      )
      selectTask("-ProjectCard<Kim, Hand>!")
      selectOnlyTask()
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
      newTestGame(addOptions = "TurmoilExpansion, FakeStuffBundle", playerCount = 2)
      kim.turn {
        playProject(Research, 11)
        playProject(FakeSelfReplicatingRobots, 7)
      }
      stan.pass()
      kim.turn {
        cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
        playProject(Pets, 10)
        pass()
      }
      kim.wgt("VenusStep")
      admin.doTask("ScienceSummit")
      kim.buyCards(0)
      stan.buyCards(0)

      stan.pass()
      kim.pass()
      stan.wgt("VenusStep")
      stan.doTask("OceanTile<Tharsis_1_5> BY Admin")
      admin.doTask("ExploreFirstDirective")
      kim.buyCards(0)
      stan.buyCards(0)

      kim.pass()
      stan.pass()
      kim.wgt("VenusStep")
      admin.doTask("MoralMovement")
      kim.buyCards(0)
      stan.buyCards(0)

      stan.pass()
      kim.pass()
      stan.wgt("VenusStep")
      // The workflow resolves the event after the world-government action returns.
      return kim.count("RobotUnit<Class<$Mine>>") to kim.count("Animal<$Pets>")
    }
  }
}
