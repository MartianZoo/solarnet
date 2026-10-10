package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
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

    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    shouldThrow<LimitsException> {
      kim.cardAction1(FakeSelfReplicatingRobots) {
        doTask("StageForReplicatedProject<Class<$TitaniumMine>>")
      }
    }

    nextGeneration()
    kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$TitaniumMine>>")
    }
  }

  @Test
  internal fun `Action can stage a card with two resources or double its resources`() {
    initialize(1)

    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    kim.assertCounts(2 to "RobotUnit<Class<$Mine>>")

    nextGeneration()
    kim.cardAction2(FakeSelfReplicatingRobots)
    kim.assertCounts(4 to "RobotUnit<Class<$Mine>>")
  }

  @Test
  internal fun `Staged cards retain independent robot unit counts`() {
    val cards = stagedCards.take(5)
    initialize(cards.size)

    cards.forEachIndexed { index, card ->
      kim.cardAction1(FakeSelfReplicatingRobots) {
        doTask("StageForReplicatedProject<Class<$card>>")
      }
      if (index != cards.lastIndex) nextGeneration()
    }

    kim.assertCounts(0 to "ProjectCard", 10 to "RobotUnit")
    cards.forEach { card ->
      kim.count("RobotUnit<Class<$card>>") shouldBe 2
    }
  }

  @Test
  internal fun `Doubling chooses one card rather than every card`() {
    initialize(2)
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    nextGeneration()
    kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$TitaniumMine>>")
    }
    nextGeneration()

    kim.cardAction2(FakeSelfReplicatingRobots) { doTask("ReplicateForStagedProject<Class<$Mine>>") }

    kim.assertCounts(
        4 to "RobotUnit<Class<$Mine>>",
        2 to "RobotUnit<Class<$TitaniumMine>>",
    )
  }

  @Test
  internal fun `More than five cards can be staged`() {
    initialize(stagedCards.size)
    stagedCards.forEachIndexed { index, card ->
      kim.cardAction1(FakeSelfReplicatingRobots) {
        doTask("StageForReplicatedProject<Class<$card>>")
      }
      if (index != stagedCards.lastIndex) nextGeneration()
    }

    kim.assertCounts(0 to "ProjectCard", 12 to "RobotUnit")
  }

  @Test
  internal fun `Staged cards remain outside hand for Planner`() {
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(16, "ProjectCard")
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }

    kim.count("ProjectCard") shouldBe 15
    shouldThrow<RequirementException> { kim.claimMilestone(cn("Planner")) }
  }

  @Test
  internal fun `Staged cards remain outside hand for Visionary`() {
    newTestGame(addOptions = "FakeStuffBundle, Visionary, Landlord, Banker", playerCount = 2)
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(2, "ProjectCard")
    stan.setToExMachina(2, "ProjectCard")
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }

    kim.fundAward(cn("Visionary"), 8)
    victoryPoints() shouldBe listOf(20, 25)
  }

  @Test
  internal fun `Sell Patents cannot sell a staged card`() {
    initialize(2)
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }

    kim.sellPatents(1).expect("MC, -ProjectCard, 0 RobotUnit")

    kim.count("ProjectCard") shouldBe 0
    kim.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Excentric ignores resources on a card that is not in play`() {
    newTestGame(addOptions = "HellasMap, FakeStuffBundle", playerCount = 2)
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(1, "ProjectCard")
    stan.exMachina("$SearchForLife, Science<$SearchForLife>")
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }

    kim.fundAward(cn("Excentric"), 8)
    victoryPoints() shouldBe listOf(20, 28)
  }

  @Test
  internal fun `A card may be staged before its play requirement is met`() {
    initialize(1)
    kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$DiversitySupport>>")
    }

    shouldThrow<RequirementException> { kim.playProject(DiversitySupport, 0) }

    kim.assertCounts(0 to "$DiversitySupport", 2 to "RobotUnit<Class<$DiversitySupport>>")
  }

  @Test
  internal fun `Staging a card does not fire its play effects or triggers`() {
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots, PROD[2 MC, Energy]")
    kim.setToExMachina(1, "ProjectCard")
    kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$ImmigrantCity>>")
    }
    repeat(3) {
      nextGeneration()
      kim.cardAction2(FakeSelfReplicatingRobots)
    }

    kim.assertProds(3 to "MC", 2 to "Energy")
    kim.count("CityTile") shouldBe 0

    kim.playProject(ImmigrantCity, 0) {
      placeTile(7, 4)
    }

    kim.assertProds(2 to "MC", 1 to "Energy")
    kim.count("CityTile") shouldBe 1
  }

  @Test
  internal fun `Resources reduce a staged cards play cost one MC each`() {
    initialize(1)
    kim.setToExMachina(2, "MC")
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }

    kim.playProject(Mine, 2).expect("-2 MC")

    kim.assertCounts(0 to "ProjectCard", 1 to "$Mine")
  }

  @Test
  internal fun `A staged card discount cannot reduce its cost below zero`() {
    initialize(1)
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    nextGeneration()
    kim.cardAction2(FakeSelfReplicatingRobots)
    nextGeneration()
    kim.cardAction2(FakeSelfReplicatingRobots)

    kim.playProject(Mine, 0).expect("0 MC")

    kim.assertCounts(
        1 to "$Mine",
        0 to "RobotUnit<Class<$Mine>>",
    )
  }

  @Test
  internal fun `Playing one staged card discards only that cards resources`() {
    initialize(2)
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    nextGeneration()
    kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$TitaniumMine>>")
    }
    nextGeneration()
    kim.cardAction2(FakeSelfReplicatingRobots) { doTask("ReplicateForStagedProject<Class<$Mine>>") }

    kim.playProject(Mine, 0).expect("0 MC")

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
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }

    kim.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action1>")
      doTask("StageForReplicatedProject<Class<$TitaniumMine>>")
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
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }

    kim.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action2>")
    }

    kim.count("RobotUnit<Class<$Mine>>") shouldBe 4
  }

  @Test
  internal fun `Viron can double a staged card twice in one generation`() {
    initialize(1)
    kim.exMachina("$Viron")
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    nextGeneration()

    kim.cardAction2(FakeSelfReplicatingRobots)
    kim.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action2>")
    }

    kim.count("RobotUnit<Class<$Mine>>") shouldBe 8
  }

  @Test
  internal fun `Viron can double two different staged cards`() {
    initialize(2)
    kim.exMachina("$Viron")
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    nextGeneration()
    kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$TitaniumMine>>")
    }
    nextGeneration()

    kim.cardAction2(FakeSelfReplicatingRobots) { doTask("ReplicateForStagedProject<Class<$Mine>>") }
    kim.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<Class<$TitaniumMine>>")
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

  private val stagedCards =
      listOf(Mine, TitaniumMine, MartianRails, SpaceStation, PowerPlant, VestaShipyard)

  @Test
  internal fun `Sponsored Academies cannot discard a card hosted on Self-Replicating Robots`() {
    initialize(2)
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    kim.count("ProjectCard") shouldBe 1

    shouldThrow<LimitsException> { kim.playProject(SponsoredAcademies, 9) }
    kim.count("ProjectCard") shouldBe 1
    kim.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Mars University cannot discard a hosted card when a staged science card is played`() {
    initialize(2)
    kim.exMachina("$MarsUniversity")
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    nextGeneration()
    kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$ResearchOutpost>>")
    }

    kim.playProject(ResearchOutpost, 16) { placeTile(4, 2) }
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
    kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$VenusWaystation>>")
    }
    return kim.playProject(CorroderSuits, 8)
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
    kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$AerialMappers>>")
    }
    kim.setToExMachina(10, "MC")
  }

  private fun playDiversityWithRobotUnits(): TaskResult {
    setUpEightResourceTypesWithRobots()
    return kim.playProject(DiversitySupport, 1)
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
    kim.fundAward(cn("Collector"), 8)
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
          addCardResources(AerialMappers)
        }
        .expect("0 RobotUnit, Floater<$AerialMappers>")
  }

  private fun useMaxwellWithHostedVenusCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots, " + "$AerialMappers, Floater<$AerialMappers>")
    kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$VenusWaystation>>")
    }
    kim.exMachina("$MaxwellBase")
    return kim.cardAction1(MaxwellBase, choice)
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
          addCardResources(Pets)
        }
        .expect("0 RobotUnit, Animal<$Pets>")
  }

  private fun playCeosFavoriteWithHostedCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots, $Pets, Animal<$Pets>")
    kim.setToExMachina(2, "ProjectCard")
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    return kim.playProject(CeosFavoriteProject, 1, body = choice)
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
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    return kim.cardAction1(FakeAppliedScience, choice)
  }

  @Ignore // Hosted cards are not resource destinations.
  @Test
  internal fun `L1 Trade Terminal can add to a hosted card`() {
    playTerminalWithHostedCard {
          doTask("RobotUnit<Class<$Mine>>")
          addCardResources(Pets)
        }
        .expect("RobotUnit<Class<$Mine>>, Animal<$Pets>")
  }

  @Test
  internal fun `BUG - L1 Trade Terminal rejects a hosted card`() {
    playTerminalWithHostedCard {
          shouldThrow<NarrowingException> { doTask("RobotUnit<Class<$Mine>>") }
          addCardResources(Pets)
        }
        .expect("0 RobotUnit, Animal<$Pets>")
  }

  private fun playTerminalWithHostedCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "Prelude2CardPack, ColoniesExpansion, FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots, $Pets, Animal<$Pets>")
    kim.setToExMachina(2, "ProjectCard")
    kim.cardAction1(FakeSelfReplicatingRobots) { doTask("StageForReplicatedProject<Class<$Mine>>") }
    return kim.playProject(L1TradeTerminal, 25, body = choice)
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
    kim.playProject(CeosFavoriteProject, 0).expect("PlayedEvent<Class<$CeosFavoriteProject>>")
  }

  private fun stageCardWithoutEligibleTag(): TaskResult {
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeSelfReplicatingRobots")
    kim.setToExMachina(1, "ProjectCard")
    return kim.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$CeosFavoriteProject>>")
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
      admin.doTask("SponsoredProjects")
      kim.buyCards(0)
      stan.buyCards(0)

      stan.pass()
      kim.pass()
      stan.wgt("VenusStep")
      stan.doTask("OceanTile<Tharsis_1_5> BY Admin")
      admin.doTask("ScientificCommunity")
      kim.buyCards(0)
      stan.buyCards(0)

      kim.pass()
      stan.pass()
      kim.wgt("VenusStep")
      admin.doTask("StrongSociety")
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
