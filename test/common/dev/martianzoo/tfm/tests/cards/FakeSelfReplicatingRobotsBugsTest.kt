package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect Fake SRR interactions. */
internal class FakeSelfReplicatingRobotsBugsTest : CardTest() {
  @Test
  internal fun `Corroder Suits incorrectly ignores a Venus card staged on Fake SRR`() {
    newGame(VenusNextExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, ProjectCard")
    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$VenusWaystation>>")
    }

    p1.runOperation("$CorroderSuits")

    p1.assertCounts(
        1 to "$CorroderSuits",
        2 to "RobotUnit<Class<$VenusWaystation>>",
    )
  }

  @Test
  internal fun `Fake SRR robot units incorrectly count as resource types and for Collector`() {
    newGame(Amazonis, VenusNextExpansion, PromoCardPack, FakeStuffBundle)
    val p2 = requireP2()
    admin.phase("Action")
    val standardResources = "MC, Steel, Titanium, Plant, Energy, Heat"
    p1.runOperation(
        "9 MC, 2 ProjectCard, $FakeSelfReplicatingRobots, $standardResources, " +
            "$Pets, $Decomposers, Animal<$Pets>, Microbe<$Decomposers>"
    )
    p2.runOperation(
        "$standardResources, $Predators, $RegolithEaters, " +
            "Animal<$Predators>, Microbe<$RegolithEaters>"
    )

    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$AerialMappers>>")
    }
    p1.playProject(DiversitySupport, 1).expect("TerraformRating")
    p1.fundAward(cn("Collector"), 8)
    val checkpoint = game.timeline.checkpoint()
    admin.runOperation("End FROM Phase")

    p1.assertCounts(1 to "FirstPlace<Player1, Collector>")
    p2.assertCounts(0 to "FirstPlace<Player2, Collector>")
    p1.auditGainsSince(checkpoint) shouldBe 1
  }

  @Test
  internal fun `Maxwell Base incorrectly cannot add to a staged Venus card`() {
    newGame(VenusNextExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation(
        "$FakeSelfReplicatingRobots, ProjectCard, PROD[Energy], " +
            "$AerialMappers, Floater<$AerialMappers>"
    )
    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$VenusWaystation>>")
    }
    p1.runOperation("$MaxwellBase")

    p1.cardAction1(MaxwellBase) {
          shouldThrowAny { doTask("RobotUnit<Class<$VenusWaystation>>") }
          addCardResources(AerialMappers)
        }
        .expect("0 RobotUnit")
  }

  @Test
  internal fun `CEO's Favorite Project incorrectly cannot add to a card staged on Fake SRR`() {
    newGame(CorporateEraExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, ProjectCard, $Pets")
    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$Mine>>")
    }

    p1.runOperation("$CeosFavoriteProject") {
          shouldThrowAny { doTask("RobotUnit<Class<$Mine>>") }
          addCardResources(Pets)
        }
        .expect("0 RobotUnit")
  }

  @Test
  internal fun `Sponsored Projects incorrectly skips resources on cards staged on Fake SRR`() {
    newGame(TurmoilExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, ProjectCard, $Pets")
    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$Mine>>")
    }
    admin.runOperation("SponsoredProjects")

    admin.runOperation("ResolveGlobalEvent<Class<SponsoredProjects>>")

    p1.count("Animal<$Pets>") shouldBe 2
    p1.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  @Test
  internal fun `Applied Science incorrectly cannot add to a card staged on Fake SRR`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, ProjectCard, FakeAppliedScience")
    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$Mine>>")
    }

    p1.cardAction1(cn("FakeAppliedScience")) {
          shouldThrowAny { doTask("RobotUnit<Class<$Mine>>") }
          doTask("Steel")
        }
        .expect("0 RobotUnit")
  }

  @Test
  internal fun `L1 Trade Terminal incorrectly skips cards staged on Fake SRR`() {
    newGame(Prelude2CardPack, ColoniesExpansion, PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, 2 ProjectCard, 25 MC, $Pets")
    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$Mine>>")
    }

    p1.playProject(L1TradeTerminal, 25) {
          shouldThrowAny { doTask("RobotUnit<Class<$Mine>>") }
          addCardResources(Pets)
        }
        .expect("0 RobotUnit")
  }
}
