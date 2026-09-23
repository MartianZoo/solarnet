package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
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

    p1.assertCounts(0 to "ProjectCard<Hand>", 10 to "RobotUnit")
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

    p1.assertCounts(0 to "ProjectCard<Hand>", 12 to "RobotUnit")
  }

  @Test
  internal fun `Staged cards remain outside hand for Planner`() {
    newGame(PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("8 MC, $FakeSelfReplicatingRobots, 16 ProjectCard")
    stage(Mine)

    p1.count("ProjectCard<Hand>") shouldBe 15
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

    p1.runOperation("MC / ProjectCard<Hand>")

    p1.count("MC") shouldBe 1
    p1.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Paradigm Breakdown discards only cards actually in hand`() {
    initialize(3)
    stage(Mine)

    p1.runOperation("-2 ProjectCard<Hand>.")

    p1.count("ProjectCard<Hand>") shouldBe 0
    p1.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Sell Patents cannot sell a staged card`() {
    initialize(2)
    stage(Mine)

    p1.sellPatents(1)

    p1.count("MC") shouldBe 1
    p1.count("ProjectCard<Hand>") shouldBe 0
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

    p1.assertCounts(0 to "MC", 0 to "ProjectCard<Hand>", 1 to "$Mine")
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
        0 to "ProjectCard<Hand>",
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
        0 to "ProjectCard<Hand>",
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
}
