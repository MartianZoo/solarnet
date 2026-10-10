package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

// Terraforming Mars Comprehensive FAQ 1.8, Self-Replicating Robots entry, pages 72–74.
internal class SelfReplicatingRobotsTest : CardTest() {
  @Test
  internal fun `Action can be used only once per generation`() {
    initialize(2)

    stage(Mine)
    shouldThrow<LimitsException> { stage(TitaniumMine) }

    nextGeneration()
    stage(TitaniumMine)
  }

  @Test
  internal fun `Action can stage a card with two resources or double its resources`() {
    initialize(1)

    stage(Mine)
    p1.assertCounts(2 to "RobotUnit<Class<$Mine>>")

    nextGeneration()
    replicate(Mine)
    p1.assertCounts(4 to "RobotUnit<Class<$Mine>>")
  }

  @Test
  internal fun `Staged cards retain independent robot unit counts`() {
    val cards = stagedCards.take(5)
    initialize(cards.size)

    cards.forEachIndexed { index, card ->
      stage(card)
      if (index != cards.lastIndex) nextGeneration()
    }

    p1.assertCounts(0 to "ProjectCard", 10 to "RobotUnit")
    cards.forEach { card ->
      p1.count("RobotUnit<Class<$card>>") shouldBe 2
    }
  }

  @Test
  internal fun `Doubling chooses one card rather than every card`() {
    initialize(2)
    stage(Mine)
    nextGeneration()
    stage(TitaniumMine)
    nextGeneration()

    replicate(Mine, select = true)

    p1.assertCounts(
        4 to "RobotUnit<Class<$Mine>>",
        2 to "RobotUnit<Class<$TitaniumMine>>",
    )
  }

  @Test
  internal fun `More than five cards can be staged`() {
    initialize(stagedCards.size)
    stagedCards.forEachIndexed { index, card ->
      stage(card)
      if (index != stagedCards.lastIndex) nextGeneration()
    }

    p1.assertCounts(0 to "ProjectCard", 12 to "RobotUnit")
  }

  @Test
  internal fun `Staged cards remain outside hand for Planner`() {
    newGame(PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("8 MC, $FakeSelfReplicatingRobots, 16 ProjectCard")
    stage(Mine)

    p1.count("ProjectCard") shouldBe 15
    shouldThrow<RequirementException> { p1.claimMilestone(cn("Planner")) }
  }

  @Test
  internal fun `Staged cards remain outside hand for Visionary`() {
    newGame(
        GameConfig(
            "PromoCardPack, FakeStuffBundle, Visionary, Landlord, Banker",
            "Player1",
            "Player2",
        )
    )
    val p2 = requireP2()
    admin.phase("Action")
    p1.runOperation("8 MC, $FakeSelfReplicatingRobots, 2 ProjectCard")
    p2.runOperation("2 ProjectCard")
    stage(Mine)

    p1.fundAward(cn("Visionary"), 8)
    admin.runOperation("End FROM Phase")

    p1.assertCounts(0 to "FirstPlace<Player1, Visionary>")
    p2.assertCounts(1 to "FirstPlace<Player2, Visionary>")
  }

  @Test
  internal fun `Scientific Community counts only cards actually in hand`() {
    initialize(2)
    stage(Mine)

    p1.runOperation("MC / ProjectCard")

    p1.count("MC") shouldBe 1
    p1.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Paradigm Breakdown discards only cards actually in hand`() {
    initialize(3)
    stage(Mine)

    p1.runOperation("-2 ProjectCard.")

    p1.count("ProjectCard") shouldBe 0
    p1.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Sell Patents cannot sell a staged card`() {
    initialize(2)
    stage(Mine)

    p1.sellPatents(1)

    p1.count("MC") shouldBe 1
    p1.count("ProjectCard") shouldBe 0
    p1.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Excentric ignores resources on a card that is not in play`() {
    newGame(Hellas, PromoCardPack, FakeStuffBundle)
    val p2 = requireP2()
    admin.phase("Action")
    p1.runOperation("8 MC, $FakeSelfReplicatingRobots, ProjectCard")
    p2.runOperation("$SearchForLife, Science<$SearchForLife>")
    stage(Mine)

    p1.fundAward(cn("Excentric"), 8)
    admin.runOperation("End FROM Phase")

    p1.assertCounts(0 to "FirstPlace<Player1, Excentric>")
    p2.assertCounts(1 to "FirstPlace<Player2, Excentric>")
  }

  @Test
  internal fun `A card may be staged before its play requirement is met`() {
    initialize(1)
    stage(DiversitySupport)

    shouldThrow<RequirementException> { p1.playProject(DiversitySupport, 0) }

    p1.assertCounts(0 to "$DiversitySupport", 2 to "RobotUnit<Class<$DiversitySupport>>")
  }

  @Test
  internal fun `Staging a card does not fire its play effects or triggers`() {
    newGame(PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, ProjectCard, PROD[2 MC, Energy]")
    stage(ImmigrantCity)
    repeat(3) {
      nextGeneration()
      replicate(ImmigrantCity)
    }

    p1.assertProds(2 to "MC", 1 to "Energy")
    p1.count("CityTile") shouldBe 0

    p1.playProject(ImmigrantCity, 0) {
      placeTile(7, 4)
    }

    p1.assertProds(1 to "MC", 0 to "Energy")
    p1.count("CityTile") shouldBe 1
  }

  @Test
  internal fun `Resources reduce a staged cards play cost one MC each`() {
    initialize(1)
    p1.runOperation("2 MC")
    stage(Mine)

    p1.playProject(Mine, 2)

    p1.assertCounts(0 to "MC", 0 to "ProjectCard", 1 to "$Mine")
  }

  @Test
  internal fun `A staged card discount cannot reduce its cost below zero`() {
    initialize(1)
    stage(Mine)
    nextGeneration()
    replicate(Mine)
    nextGeneration()
    replicate(Mine)

    p1.playProject(Mine, 0)

    p1.assertCounts(
        0 to "MC",
        1 to "$Mine",
        0 to "RobotUnit<Class<$Mine>>",
    )
  }

  @Test
  internal fun `Playing one staged card discards only that cards resources`() {
    initialize(2)
    stage(Mine)
    nextGeneration()
    stage(TitaniumMine)
    nextGeneration()
    replicate(Mine, select = true)

    p1.playProject(Mine, 0)

    p1.assertCounts(
        1 to "$Mine",
        0 to "ProjectCard",
        0 to "RobotUnit<Class<$Mine>>",
        2 to "RobotUnit<Class<$TitaniumMine>>",
    )
  }

  @Test
  internal fun `Viron can stage a second card in the same generation`() {
    initialize(2, VenusNextExpansion)
    p1.runOperation("$Viron")
    stage(Mine)

    p1.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action1>")
      doTask("StageForReplicatedProject<Class<$TitaniumMine>>")
    }

    p1.assertCounts(
        0 to "ProjectCard",
        2 to "RobotUnit<Class<$Mine>>",
        2 to "RobotUnit<Class<$TitaniumMine>>",
    )
  }

  @Test
  internal fun `Viron can stage and then double that card in the same generation`() {
    initialize(1, VenusNextExpansion)
    p1.runOperation("$Viron")
    stage(Mine)

    p1.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action2>")
    }

    p1.count("RobotUnit<Class<$Mine>>") shouldBe 4
  }

  @Test
  internal fun `Viron can double a staged card twice in one generation`() {
    initialize(1, VenusNextExpansion)
    p1.runOperation("$Viron")
    stage(Mine)
    nextGeneration()

    replicate(Mine)
    p1.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action2>")
    }

    p1.count("RobotUnit<Class<$Mine>>") shouldBe 8
  }

  @Test
  internal fun `Viron can double two different staged cards`() {
    initialize(2, VenusNextExpansion)
    p1.runOperation("$Viron")
    stage(Mine)
    nextGeneration()
    stage(TitaniumMine)
    nextGeneration()

    replicate(Mine, select = true)
    p1.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<Class<$TitaniumMine>>")
    }

    p1.assertCounts(
        4 to "RobotUnit<Class<$Mine>>",
        4 to "RobotUnit<Class<$TitaniumMine>>",
    )
  }

  private fun initialize(cards: Int, vararg options: dev.martianzoo.tfm.tests.TestOption) {
    newGame(PromoCardPack, FakeStuffBundle, *options)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, $cards ProjectCard")
  }

  private fun stage(card: ClassName) {
    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$card>>")
    }
  }

  private fun replicate(card: ClassName, select: Boolean = false) {
    p1.cardAction2(FakeSelfReplicatingRobots) {
      if (select) doTask("ReplicateForStagedProject<Class<$card>>")
    }
  }

  private fun nextGeneration() = admin.runOperation("Generation")

  private val stagedCards =
      listOf(Mine, TitaniumMine, MartianRails, SpaceStation, PowerPlant, VestaShipyard)

  @Test
  internal fun `Sponsored Academies cannot discard a card hosted on Self-Replicating Robots`() {
    initialize(2, VenusNextExpansion)
    p1.runOperation("9 MC")
    stage(Mine)
    p1.count("ProjectCard") shouldBe 1

    shouldThrow<LimitsException> { p1.playProject(SponsoredAcademies, 9) }
    p1.count("ProjectCard") shouldBe 1
    p1.count("RobotUnit<Class<$Mine>>") shouldBe 2
    p1.count("MC") shouldBe 9
  }

  @Test
  internal fun `Mars University cannot discard a hosted card when a staged science card is played`() {
    newGame(CorporateEraExpansion, PromoCardPack, FakeStuffBundle)
    p1.playCorp(CrediCor, 5)
    admin.phase("Action")
    p1.runOperation("20 MC")
    p1.playProject(MarsUniversity, 8) { declineTask() }
    p1.playProject(SearchForLife, 3) { declineTask() }
    p1.playProject(FakeSelfReplicatingRobots, 7)
    stage(Mine)
    nextGeneration()
    stage(ResearchOutpost)
    p1.stdProject("PowerPlantProject")
    p1.count("ProjectCard") shouldBe 0

    p1.playProject(ResearchOutpost, 16) { placeTile(4, 2) }
        .expect("0 ProjectCard, 0 RobotUnit<Class<$Mine>>")
    p1.count("RobotUnit<Class<$Mine>>") shouldBe 2
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
    newGame(VenusNextExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, 2 ProjectCard, 8 MC")
    stage(VenusWaystation)
    return p1.playProject(CorroderSuits, 8)
  }

  @Ignore // RobotUnit is counted as a distinct resource type.
  @Test
  internal fun `Robot units do not satisfy Diversity Support's ninth resource type`() {
    shouldThrow<RequirementException> { playDiversityWithRobotUnits() }
    p1.assertCounts(10 to "MC", 1 to "ProjectCard", 0 to "$DiversitySupport", 2 to "RobotUnit")
  }

  @Test
  internal fun `BUG - Robot units satisfy Diversity Support's ninth resource type`() {
    playDiversityWithRobotUnits().expect("TerraformRating")
  }

  private fun setUpEightResourceTypesWithRobots() {
    newGame(Amazonis, VenusNextExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    val standardResources = "MC, Steel, Titanium, Plant, Energy, Heat"
    p1.runOperation(
        "9 MC, 2 ProjectCard, $FakeSelfReplicatingRobots, $standardResources, " +
            "$Pets, $Decomposers, Animal<$Pets>, Microbe<$Decomposers>"
    )
    stage(AerialMappers)
  }

  private fun playDiversityWithRobotUnits(): TaskResult {
    setUpEightResourceTypesWithRobots()
    return p1.playProject(DiversitySupport, 1)
  }

  @Ignore // Collector counts the RobotUnit resource type.
  @Test
  internal fun `Robot units do not break a tie for Collector`() {
    scoreCollectorWithRobotUnits()
        .expect("FirstPlace<Player1, Collector>, FirstPlace<Player2, Collector>")
  }

  @Test
  internal fun `BUG - Robot units break a tie for Collector`() {
    scoreCollectorWithRobotUnits()
        .expect("FirstPlace<Player1, Collector>, 0 FirstPlace<Player2, Collector>")
  }

  private fun scoreCollectorWithRobotUnits(): TaskResult {
    setUpEightResourceTypesWithRobots()
    requireP2()
        .runOperation(
            "MC, Steel, Titanium, Plant, Energy, Heat, $Predators, $RegolithEaters, " +
                "Animal<$Predators>, Microbe<$RegolithEaters>"
        )
    p1.fundAward(cn("Collector"), 8)
    return admin.runOperation("End FROM Phase")
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
    newGame(VenusNextExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation(
        "$FakeSelfReplicatingRobots, ProjectCard, PROD[Energy], " +
            "$AerialMappers, Floater<$AerialMappers>"
    )
    stage(VenusWaystation)
    p1.runOperation("$MaxwellBase")
    return p1.cardAction1(MaxwellBase, choice)
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
    newGame(CorporateEraExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, 2 ProjectCard, MC, $Pets")
    stage(Mine)
    return p1.playProject(CeosFavoriteProject, 1, body = choice)
  }

  // https://boardgamegeek.com/thread/2334454/article/33634574#33634574
  @Ignore // Hosted cards are not resource destinations.
  @Test
  internal fun `Sponsored Projects adds to a hosted card`() {
    resolveSponsoredProjectsWithHostedCard()
        .expect("RobotUnit<Player1, Class<$Mine>>, Animal<Player1, $Pets>")
  }

  @Test
  internal fun `BUG - Sponsored Projects skips a hosted card`() {
    resolveSponsoredProjectsWithHostedCard()
        .expect("0 RobotUnit<Player1, Class<$Mine>>, Animal<Player1, $Pets>")
  }

  private fun resolveSponsoredProjectsWithHostedCard(): TaskResult {
    newGame(TurmoilExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, ProjectCard, $Pets")
    stage(Mine)
    admin.runOperation("ScienceSummit THEN Current<ScienceSummit>")
    return admin.runOperation("ResolveGlobalEvent")
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
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, ProjectCard, $FakeAppliedScience")
    stage(Mine)
    return p1.cardAction1(FakeAppliedScience, choice)
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
    newGame(Prelude2CardPack, ColoniesExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, 2 ProjectCard, 25 MC, $Pets")
    stage(Mine)
    return p1.playProject(L1TradeTerminal, 25, body = choice)
  }

  // https://boardgamegeek.com/thread/1861808/article/28914878#28914878
  @Ignore // Fake SRR delegates printed-tag eligibility to the caller.
  @Test
  internal fun `Cannot host a card without a Building or Space tag`() {
    shouldThrow<RequirementException> { stageCardWithoutEligibleTag() }
    p1.assertCounts(1 to "ProjectCard", 0 to "RobotUnit")
  }

  @Test
  internal fun `BUG - Can host a card without a Building or Space tag`() {
    stageCardWithoutEligibleTag().expect("2 RobotUnit<Class<$CeosFavoriteProject>>")
    p1.playProject(CeosFavoriteProject, 0).expect("PlayedEvent<Class<$CeosFavoriteProject>>")
  }

  private fun stageCardWithoutEligibleTag(): TaskResult {
    newGame(PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, ProjectCard")
    return p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$CeosFavoriteProject>>")
    }
  }
}
