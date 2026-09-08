package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
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

    stage(1)
    shouldThrow<LimitsException> { stage(2) }

    nextGeneration()
    stage(2)
  }

  @Test
  internal fun `Action can stage a card with two resources or double an occupied berth`() {
    initialize(1)

    stage(1)
    p1.assertCounts(2 to "RobotUnit<SelfReplicatingRobotsBerth1>")

    nextGeneration()
    replicate(1)
    p1.assertCounts(4 to "RobotUnit<SelfReplicatingRobotsBerth1>")
  }

  @Test
  internal fun `Five cards can occupy independent berths with their own resources`() {
    initialize(5)

    (1..5).forEach { number ->
      stage(number)
      if (number != 5) nextGeneration()
    }

    p1.assertCounts(
        5 to "ProjectCard<StagedProject>",
        10 to "RobotUnit<StagedProject>",
    )
    (1..5).forEach { number ->
      p1.count("RobotUnit<SelfReplicatingRobotsBerth$number>") shouldBe 2
    }
  }

  @Test
  internal fun `Doubling chooses one card rather than every card`() {
    initialize(2)
    stage(1)
    nextGeneration()
    stage(2)
    nextGeneration()

    replicate(1)

    p1.assertCounts(
        4 to "RobotUnit<SelfReplicatingRobotsBerth1>",
        2 to "RobotUnit<SelfReplicatingRobotsBerth2>",
    )
  }

  @Test
  internal fun `Five named berths cap the number of staged cards`() {
    initialize(6)
    (1..5).forEach { number ->
      stage(number)
      nextGeneration()
    }

    p1.count("ProjectCard<Hand>") shouldBe 1
    shouldThrow<AbstractException> { p1.cardAction1(FakeSelfReplicatingRobots) }
  }

  @Test
  internal fun `Berthed cards remain outside hand for Planner`() {
    newGame(PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.manual("8 MC, $FakeSelfReplicatingRobots, 16 ProjectCard")
    stage(1)

    p1.count("ProjectCard<Hand>") shouldBe 15
    shouldThrow<RequirementException> { p1.claimMilestone(cn("Planner")) }
  }

  @Test
  internal fun `Berthed cards remain outside hand for Visionary`() {
    newGame(
        GameConfig(
            "PromoCardPack, FakeStuffBundle, Visionary, Landlord, Banker",
            "Player1",
            "Player2",
        )
    )
    val p2 = requireP2()
    admin.phase("Action")
    p1.manual("8 MC, $FakeSelfReplicatingRobots, 2 ProjectCard")
    p2.manual("2 ProjectCard")
    stage(1)

    p1.fundAward(cn("Visionary"), 8)
    admin.manual("End FROM Phase")

    p1.assertCounts(0 to "FirstPlace<Player1, Visionary>")
    p2.assertCounts(1 to "FirstPlace<Player2, Visionary>")
  }

  @Test
  internal fun `Scientific Community counts only cards actually in hand`() {
    initialize(2)
    stage(1)

    p1.manual("MC / ProjectCard<Hand>")

    p1.count("MC") shouldBe 1
    p1.count("ProjectCard<SelfReplicatingRobotsBerth1>") shouldBe 1
  }

  @Test
  internal fun `Paradigm Breakdown discards only cards actually in hand`() {
    initialize(3)
    stage(1)

    p1.manual("-2 ProjectCard<Hand>.")

    p1.count("ProjectCard<Hand>") shouldBe 0
    p1.count("ProjectCard<SelfReplicatingRobotsBerth1>") shouldBe 1
  }

  @Test
  internal fun `Sell Patents cannot sell a berthed card`() {
    initialize(2)
    stage(1)

    p1.sellPatents(1)

    p1.count("MC") shouldBe 1
    p1.count("ProjectCard<Hand>") shouldBe 0
    p1.count("ProjectCard<SelfReplicatingRobotsBerth1>") shouldBe 1
  }

  @Test
  internal fun `Excentric ignores resources on a card that is not in play`() {
    newGame(Hellas, PromoCardPack, FakeStuffBundle)
    val p2 = requireP2()
    admin.phase("Action")
    p1.manual("8 MC, $FakeSelfReplicatingRobots, ProjectCard")
    p2.manual("$SearchForLife, Science<$SearchForLife>")
    stage(1)

    p1.fundAward(cn("Excentric"), 8)
    admin.manual("End FROM Phase")

    p1.assertCounts(0 to "FirstPlace<Player1, Excentric>")
    p2.assertCounts(1 to "FirstPlace<Player2, Excentric>")
  }

  @Test
  internal fun `A card may be berthed before its play requirement is met`() {
    initialize(1)
    stage(1)

    shouldThrow<RequirementException> {
      p1.manual(
          "PlayCard<Class<ProjectCard>, Class<$DiversitySupport>, " + "SelfReplicatingRobotsBerth1>"
      )
    }

    p1.assertCounts(
        1 to "ProjectCard<SelfReplicatingRobotsBerth1>",
        2 to "RobotUnit<SelfReplicatingRobotsBerth1>",
    )
  }

  @Test
  internal fun `Staging a card does not fire its play effects or triggers`() {
    newGame(PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.manual("$FakeSelfReplicatingRobots, ProjectCard, PROD[2 MC, Energy]")
    stage(1)
    repeat(3) {
      nextGeneration()
      replicate(1)
    }

    p1.assertProds(2 to "MC", 1 to "Energy")
    p1.count("CityTile") shouldBe 0

    p1.manual(
        "PlayCard<Class<ProjectCard>, Class<$ImmigrantCity>, " + "SelfReplicatingRobotsBerth1>"
    ) {
      placeTile(7, 4)
    }

    p1.assertProds(1 to "MC", 0 to "Energy")
    p1.count("CityTile") shouldBe 1
  }

  @Test
  internal fun `Resources reduce a berthed cards play cost one MC each`() {
    initialize(1)
    p1.manual("2 MC")
    stage(1)

    p1.manual("PlayCard<Class<ProjectCard>, Class<$Mine>, SelfReplicatingRobotsBerth1>") {
      doTask("2 Pay<Class<MC>> FROM MC")
      doTask("Ok")
    }

    p1.assertCounts(0 to "MC", 1 to "$Mine")
  }

  @Test
  internal fun `A berth discount cannot reduce a card cost below zero`() {
    initialize(1)
    stage(1)
    nextGeneration()
    replicate(1)
    nextGeneration()
    replicate(1)

    p1.manual("PlayCard<Class<ProjectCard>, Class<$Mine>, SelfReplicatingRobotsBerth1>")

    p1.assertCounts(
        0 to "MC",
        1 to "$Mine",
        0 to "RobotUnit<SelfReplicatingRobotsBerth1>",
    )
  }

  @Test
  internal fun `Playing one berthed card discards only that cards resources`() {
    initialize(2)
    stage(1)
    nextGeneration()
    stage(2)
    nextGeneration()
    replicate(1)

    p1.manual("PlayCard<Class<ProjectCard>, Class<$Mine>, SelfReplicatingRobotsBerth1>")

    p1.assertCounts(
        1 to "$Mine",
        0 to "ProjectCard<SelfReplicatingRobotsBerth1>",
        0 to "RobotUnit<SelfReplicatingRobotsBerth1>",
        1 to "ProjectCard<SelfReplicatingRobotsBerth2>",
        2 to "RobotUnit<SelfReplicatingRobotsBerth2>",
    )
  }

  @Test
  internal fun `Viron can stage a second card in the same generation`() {
    initialize(2, VenusNextExpansion)
    p1.manual("$Viron")
    stage(1)

    p1.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action1>")
      doTask("StageForReplicatedProject<SelfReplicatingRobotsBerth2>")
      doTask("ProjectCard<SelfReplicatingRobotsBerth2 FROM Hand>")
    }

    p1.assertCounts(
        2 to "ProjectCard<StagedProject>",
        2 to "RobotUnit<SelfReplicatingRobotsBerth1>",
        2 to "RobotUnit<SelfReplicatingRobotsBerth2>",
    )
  }

  @Test
  internal fun `Viron can stage and then double that card in the same generation`() {
    initialize(1, VenusNextExpansion)
    p1.manual("$Viron")
    stage(1)

    p1.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<ProjectCard<SelfReplicatingRobotsBerth1>>")
    }

    p1.count("RobotUnit<SelfReplicatingRobotsBerth1>") shouldBe 4
  }

  @Test
  internal fun `Viron can double a berthed card twice in one generation`() {
    initialize(1, VenusNextExpansion)
    p1.manual("$Viron")
    stage(1)
    nextGeneration()

    replicate(1)
    p1.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<ProjectCard<SelfReplicatingRobotsBerth1>>")
    }

    p1.count("RobotUnit<SelfReplicatingRobotsBerth1>") shouldBe 8
  }

  @Test
  internal fun `Viron can double two different berthed cards`() {
    initialize(2, VenusNextExpansion)
    p1.manual("$Viron")
    stage(1)
    nextGeneration()
    stage(2)
    nextGeneration()

    replicate(1)
    p1.cardAction1(Viron) {
      doTask("UseAction<$FakeSelfReplicatingRobots, Action2>")
      doTask("ReplicateForStagedProject<ProjectCard<SelfReplicatingRobotsBerth2>>")
    }

    p1.assertCounts(
        4 to "RobotUnit<SelfReplicatingRobotsBerth1>",
        4 to "RobotUnit<SelfReplicatingRobotsBerth2>",
    )
  }

  private fun initialize(cards: Int, vararg options: dev.martianzoo.tfm.tests.TestOption) {
    newGame(PromoCardPack, FakeStuffBundle, *options)
    admin.phase("Action")
    p1.manual("$FakeSelfReplicatingRobots, $cards ProjectCard")
  }

  private fun stage(number: Int) {
    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<SelfReplicatingRobotsBerth$number>")
      doTask("ProjectCard<SelfReplicatingRobotsBerth$number FROM Hand>")
    }
  }

  private fun replicate(number: Int) {
    p1.cardAction2(FakeSelfReplicatingRobots) {
      doTask("ReplicateForStagedProject<ProjectCard<SelfReplicatingRobotsBerth$number>>")
    }
  }

  private fun nextGeneration() = admin.manual("Generation")
}
