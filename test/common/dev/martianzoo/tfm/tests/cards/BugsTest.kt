package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior. */
internal class BugsTest : CardTest() {
  @Test
  internal fun `Ecology Experts incorrectly does not trigger Viral Enhancers with its own tags`() {
    newGame(PreludeExpansion, CorporateEraExpansion)
    admin.phase("Prelude")
    p1.runOperation("9 MC, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) { p1.playProject(ViralEnhancers, 9) }

    p1.assertCounts(1 to "Plant")
  }

  @Test
  internal fun `Ecology Experts incorrectly does not trigger Ecological Zone with its plant tag`() {
    newGame(PreludeExpansion)
    admin.phase("Prelude")
    p1.runOperation("12 MC, ProjectCard, PreludeCard, GreeneryTile<Tharsis_4_4>")

    p1.playPrelude(EcologyExperts) { p1.playProject(EcologicalZone, 12) { placeTile(4, 5) } }

    p1.assertCounts(2 to "Animal<$EcologicalZone>")
  }

  @Test
  internal fun `Mars University incorrectly allows two discards before either draw`() {
    newGame(CorporateEraExpansion)
    p1.runOperation(
        "5 ProjectCard, $MarsUniversity"
    ) { /* Decline Mars University's discard-and-draw effect. */
      declineTask()
    }
    val manual = p1.also { it.autoExecPolicy = NONE }

    manual
        .runOperation("$Research") {
          doTask("2 ProjectCard")
          doTask("-ProjectCard")
          doTask("-ProjectCard")
          doTask("ProjectCard")
          doTask("ProjectCard")
        }
        .expect("2 ProjectCard")
  }

  // https://boardgamegeek.com/thread/3361875/questions-about-the-head-start
  @Test
  internal fun `Head Start incorrectly allows its two actions to interleave`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.phase("Prelude")
    p1.runOperation("4 MC, 10 ProjectCard, PreludeCard, 10 Heat")

    p1.playPrelude(FakeHeadStart) {
      p1.assertCounts(2 to "Steel", 24 to "MC")
      doTask("UseAction<ConvertHeatAction, Action1>")
      doTask("8 Pay<Class<Heat>> FROM Heat")
      doTask("UseAction<UseStandardProjectAction, Action1>")
      doTask("UseAction<AquiferProject, Action1>")
      doTask("18 Pay<Class<MC>> FROM MC")
      placeTile(5, 5)
    }
  }

  @Test
  internal fun `Prelude incorrectly allows discarding a playable card`() {
    newGame(PreludeExpansion)
    admin.phase("Prelude")
    val moneyBefore = p1.count("MC")

    p1.startTurn()
    p1.doTask("-PreludeCard")
    p1.startTurn()
    p1.playPrelude(DomeFarming)

    p1.assertCounts(1 to "$DomeFarming", 0 to "PreludeCard")
    p1.count("MC") shouldBe moneyBefore + 15
  }

  @Test
  internal fun `Space Elevator incorrectly accepts payment that wastes one steel`() {
    newGame()
    admin.phase("Action")
    p1.runOperation("10 Steel, 10 Titanium, ProjectCard")

    p1.inTurn {
      doTask("UseAction<PlayCardFromHandAction, Action1>")
      doTask("PlayCard<Class<ProjectCard>, Class<$SpaceElevator>, Hand>")
      doTask("7 Pay<Class<Steel>> FROM Steel")
      doTask("5 Pay<Class<Titanium>> FROM Titanium")
      doTask("Ok")
    }

    p1.assertCounts(
        3 to "Steel",
        5 to "Titanium",
        0 to "ProjectCard",
        1 to "$SpaceElevator",
    )
  }

  @Test
  internal fun `Fake SRR incorrectly accepts a card without a Building or Space tag`() {
    newGame(PromoCardPack, FakeStuffBundle)
    admin.phase("Action")
    p1.runOperation("$FakeSelfReplicatingRobots, ProjectCard")

    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$CeosFavoriteProject>>")
    }
    p1.playProject(CeosFavoriteProject, 0)

    p1.assertCounts(1 to "PlayedEvent<Class<$CeosFavoriteProject>>")
  }

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
    admin.runOperation("End FROM Phase")

    p1.assertCounts(1 to "FirstPlace<Player1, Collector>")
    p2.assertCounts(0 to "FirstPlace<Player2, Collector>")
  }
}
