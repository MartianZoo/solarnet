package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior. */
internal class BugsTest : CardTest() {
  @Test
  internal fun `Ecology Experts incorrectly does not trigger Viral Enhancers with its own tags`() {
    newGame(PreludeExpansion, CorporateEraExpansion)
    admin.phase("Prelude")
    p1.runOperation("9 MC, ProjectCard, PreludeCard")

    with(p1) {
      playPrelude(EcologyExperts) { playProject(ViralEnhancers, 9) }
    }

    p1.assertCounts(1 to "Plant")
  }

  @Test
  internal fun `Ecology Experts incorrectly does not trigger Ecological Zone with its plant tag`() {
    newGame(PreludeExpansion)
    admin.phase("Prelude")
    p1.runOperation("12 MC, ProjectCard, PreludeCard, GreeneryTile<Tharsis_4_4>")

    with(p1) {
      playPrelude(EcologyExperts) {
        playProject(EcologicalZone, 12) { placeTile(4, 5) }
      }
    }

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
  internal fun `Fake Preservation Program incorrectly enables UNMI after reversing its TR gain`() {
    newGame(PreludeExpansion, Prelude2CardPack, FakeStuffBundle)
    p1.phase("Prelude")
    p1.runOperation("$UnitedNationsMarsInitiative, FakePreservationProgram")
    admin.phase("Action")

    // The printed Preservation Program prevents this gain, so it should not satisfy UNMI's gate.
    p1.runOperation("TerraformRating").expect("0 TerraformRating")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("-3 MC, TerraformRating")
  }

  @Test
  internal fun `Fake Preservation Program incorrectly triggers Terraforming Deal on reversed TR`() {
    newGame(PreludeExpansion, Prelude2CardPack, FakeStuffBundle)
    p1.phase("Prelude")
    p1.runOperation("FakePreservationProgram, TerraformingDeal")
    admin.phase("Action")

    // The printed Preservation Program prevents the gain, and therefore this rebate.
    p1.runOperation("TerraformRating").expect("0 TerraformRating, 2 MC")
  }

  @Test
  internal fun `Fake Thawer incorrectly retains credits after temperature reductions`() {
    newGame(GameConfig("FakeStuffBundle, FakeThawer, Builder, Engineer", "Player1", "Player2"))
    p1.runOperation("8 MC, 5 TemperatureStep")
    admin.runOperation("-TemperatureStep")
    admin.phase("Action")
    // Unlike markers on the printed track, these credits cannot identify the removed step.
    p1.claimMilestone(cn("FakeThawer")).expect("-8 MC, FakeThawer")
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

  // An unplayable Prelude should fizzle: discard it and gain 15 MC.
  // https://boardgamegeek.com/thread/2993276/article/41533232#41533232
  @Test
  internal fun `Valley Trust incorrectly refuses to fizzle a selected unplayable Prelude`() {
    newGame(PreludeExpansion, Prelude2CardPack, retainedStartingProjects = 5)
    val p2 = requireP2()
    p2.runOperation("PROD[-5 MC]")
    p1.playCorp(ValleyTrust, 5)
    admin.phase("Action")
    val moneyBefore = p1.count("MC")

    shouldThrowAny {
      p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(Recession) }
    }

    p1.count("RequiredAction") shouldBe 1
    p1.count("MC") shouldBe moneyBefore
    p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(DomeFarming) }
    p1.assertCounts(0 to "RequiredAction", 1 to "$DomeFarming")
  }

  @Test
  internal fun `Early Colonization incorrectly remains playable when a track cannot advance twice`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    admin.phase("Prelude")
    p1.runOperation("PreludeCard")
    admin.runOperation("5 ColonyProduction<Luna>")

    p1.playPrelude(EarlyColonization) { doTask("Colony<Ceres>") }

    p1.assertCounts(1 to "$EarlyColonization", 3 to "Energy")
    admin.count("ColonyProduction<Luna>") shouldBe 6
  }

  // https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Test
  internal fun `Sagitta incorrectly misses Merger in Head Start's nested action`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, FakeStuffBundle)
    p1.runOperation("$BoardOfDirectors, 54 MC, 8 Heat")
    admin.phase("Prelude")
    p1.runOperation("2 PreludeCard")

    p1.turn {
      playPrelude(FakeHeadStart) {
        useStdAction("UseActionOnCardAction", payment = {}) {
          doTask("ActionUsedMarker<$BoardOfDirectors>")
          doTask("UseAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          playPrelude(Merger) { playCorp(SagittaFrontierServices) }
        }
        useStdAction("ConvertHeatAction", payment = { doTask("8 Pay<Class<Heat>> FROM Heat") })
      }
    }

    // Correct is 39: four more for the tagless Merger played in Sagitta's enclosing action.
    p1.count("MC") shouldBe 35
  }

  // https://boardgamegeek.com/thread/2877214/rule-clarifications-sought-for-multiple-corporatio
  @Test
  internal fun `Fake Helion incorrectly cannot spend Stormcraft floaters on a Mons payout`() {
    newGame(
        ColoniesExpansion,
        PromoCardPack,
        FakeStuffBundle,
        VenusNextExpansion,
        colonyTiles = testColonyTiles(2),
    )
    val p2 = requireP2()
    p1.runOperation(
        "$MonsInsurance, $FakeHelion, $StormcraftIncorporated, " +
            "Floater<$StormcraftIncorporated>"
    )
    p1.runOperation("-${p1.count("MC")} MC")
    p2.runOperation("Plant")

    shouldThrowAny {
      p1.runOperation("-Plant<Player2>") {
        doTask("PayFromCard<$StormcraftIncorporated> FROM Floater<$StormcraftIncorporated>")
      }
    }

    // The real owner may elect to turn this floater into a 2 MC payment to the victim, but the
    // rejected choice rolls the attack back.
    p1.count("Floater<$StormcraftIncorporated>") shouldBe 1
    p2.count("Plant") shouldBe 1
    p2.count("MC") shouldBe 0
  }

  @Test
  internal fun `Mixed-metal payment incorrectly accepts a tender that wastes one steel`() {
    newGame()
    admin.phase("Action")
    p1.runOperation("10 Steel, 10 Titanium, ProjectCard")

    // Space Elevator merely supplies a 27 MC debt paid with both kinds of metal.
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
  internal fun `Astra Mechanica incorrectly takes back Lava Flows and Deimos Down promo`() {
    newGameWithAutoWorkflow(PromoCardPack)
    playUntilFirstActionPhase()

    p1.turn {
      playProject(LavaFlows, 18) { placeTile(2, 2) }
      playProject(DeimosDownPromo, 31) { placeTile(4, 5) }
    }
    requireP2().pass()

    p1.playProject(AstraMechanica, 7) {
          doWithoutAutoExec(p1) {
            doTask("ProjectCard FROM PlayedEvent<Class<$LavaFlows>>")
            doTask("ProjectCard FROM PlayedEvent<Class<$DeimosDownPromo>>")
          }
        }
        .expect(
            "$AstraMechanica, ProjectCard, " +
                "-PlayedEvent<Class<$LavaFlows>>, -PlayedEvent<Class<$DeimosDownPromo>>"
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
