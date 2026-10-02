package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.fakeWildTags
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior. */
internal class BugsTest : CardTest() {
  // Resolved FAQ: Hired Raiders may steal less than its maximum, but must steal at least one.
  @Test
  internal fun `Hired Raiders incorrectly permits stealing nothing`() {
    newGame(CorporateEraExpansion)
    p1.runOperation("MC, ProjectCard")
    requireP2().runOperation("2 Steel, 3 MC")
    admin.phase("Action")

    p1.playProject(HiredRaiders, 1) {
          // The implementation incorrectly accepts declining the steal altogether.
          declineTask()
        }
        .expect("0 Steel<Player1>, 0 Steel<Player2>, 0 MC<Player2>")
    p1.count("PlayedEvent<Class<$HiredRaiders>>") shouldBe 1
  }

  // Resolved FAQ: an unavailable animal option permits declining even on a microbe card.
  @Test
  internal fun `Viral Enhancers incorrectly forces a bonus on a card that can hold microbes`() {
    newGame(CorporateEraExpansion)
    p1.runOperation("22 MC, 2 ProjectCard")
    admin.phase("Action")
    p1.playProject(ViralEnhancers, 9) { doTask("Plant") }

    p1.playProject(RegolithEaters, 13) {
          shouldThrow<NarrowingException> { declineTask() }
          p1.assertCounts(0 to "Microbe<$RegolithEaters>", 1 to "Plant")
          addCardResources(RegolithEaters)
        }
        .expect("Microbe<$RegolithEaters>, 0 Plant")
  }

  // BGG wild-tag ruling: https://boardgamegeek.com/thread/2030851/article/29611733#29611733
  @Test
  internal fun `A fake wild Earth tag incorrectly gives Point Luna an extra draw`() {
    newGame(PreludeExpansion, CorporateEraExpansion, FakeStuffBundle)
    p1.playCorp(PointLuna, 1)
    admin.phase("Action")
    p1.playProject(FakeResearchCoordination, 4)

    // Both the supplied tag and Cartel's printed tag trigger Point Luna. Only Cartel should draw;
    // after consuming its project card, the correct net card gain would be zero.
    with(p1) {
      runOperation("${fakeWildTags("EarthTag")}, NewTurn") {
            useStdAction("PlayCardFromHandAction", payment = {}) { playProject(Cartel, 8) }
          }
          .expect("ProjectCard, PROD[3 MC]")
    }
    p1.count("FakeWildTagUse") shouldBe 0
  }

  // Audit N27: the absent colony category should contribute zero, leaving city scoring available.
  // BGG exact Constructor ruling:
  // https://boardgamegeek.com/thread/3242831/article/43755615#43755615
  @Test
  internal fun `Constructor incorrectly cannot be funded without Colonies`() {
    newGame(Amazonis)
    p1.playCorp(CrediCor, 0)
    admin.phase("Action")
    p1.stdProject("CityProject") { placeTile(5, 1) }

    shouldThrow<DeadEndException> { p1.fundAward(cn("Constructor"), 8) }
    p1.count("MC") shouldBe 36
    admin.count("Award") shouldBe 0
  }

  // Resolved FAQ: a wild-resource area is eligible even when its chosen resource is not metal.
  // BGG exact Amazonis ruling: https://boardgamegeek.com/thread/3403085/article/45161764#45161764
  @Test
  internal fun `Mining Rights incorrectly cannot use a wild placement bonus`() {
    newGame(Amazonis, Unsafe)
    p1.runOperation("9 MC, ProjectCard")
    admin.phase("Action")

    shouldThrow<RequirementException> {
      p1.playProject(MiningRights, 9) {
        placeTile(5, 3)
        doTask("Steel")
        doTask("PROD[Steel]")
      }
    }
    p1.assertCounts(9 to "MC", 1 to "ProjectCard", 0 to "MiningRights_SpecialTile")
  }

  // The Audit records this unverified choice, but does not prevent the incorrect production.
  @Test
  internal fun `Mining Guild incorrectly gains steel production for a nonmetal wild bonus`() {
    newGame(Amazonis, Unsafe)
    p1.playCorp(MiningGuild, 0)
    admin.phase("Action")
    val checkpoint = game.timeline.checkpoint()

    p1.stdProject("GreeneryProject") {
          placeTile(5, 3)
          doTask("Plant")
        }
        .expect("Plant, PROD[Steel]")
    p1.auditGainsSince(checkpoint) shouldBe 1
  }

  // Audit S09: these cubes belong to the unplayed card hosted on SRR, which is eligible.
  // BGG Sponsored Projects/SRR ruling:
  // https://boardgamegeek.com/thread/2334454/article/33634574#33634574
  @Test
  internal fun `Sponsored Projects incorrectly misses resources on a card hosted by Fake SRR`() {
    newGame(TurmoilExpansion, CorporateEraExpansion, PromoCardPack, FakeStuffBundle)
    p1.playCorp(CrediCor, 3)
    admin.phase("Action")
    p1.playProject(Research, 11)
    p1.playProject(FakeSelfReplicatingRobots, 7)
    p1.playProject(Pets, 10)
    p1.cardAction1(FakeSelfReplicatingRobots) {
      doTask("StageForReplicatedProject<Class<$Mine>>")
    }
    admin.runOperation("SponsoredProjects")

    admin
        .runOperation("ResolveGlobalEvent<Class<SponsoredProjects>>")
        .expect("0 RobotUnit<Player1, Class<$Mine>>, Animal<Player1, $Pets>")
    p1.count("RobotUnit<Class<$Mine>>") shouldBe 2
  }

  // BGG Ecology Experts tag timing:
  // https://boardgamegeek.com/thread/2096075/article/30501757#30501757
  @Test
  internal fun `Ecology Experts incorrectly does not trigger Viral Enhancers with its own tags`() {
    newGame(
        GameConfig(
            "PreludeExpansion, CorporateEraExpansion, EcologyExperts, Unsafe",
            "Player1",
            "Player2",
        )
    )
    admin.phase("Prelude")
    p1.runOperation("9 MC, ProjectCard, PreludeCard")

    with(p1) {
      playPrelude(EcologyExperts) { playProject(ViralEnhancers, 9) { doTask("Plant") } }
    }

    p1.assertCounts(1 to "Plant")
  }

  // BGG Ecology Experts tag timing:
  // https://boardgamegeek.com/thread/2096075/article/30501757#30501757
  @Test
  internal fun `Ecology Experts incorrectly does not trigger Ecological Zone with its plant tag`() {
    newGame(GameConfig("PreludeExpansion, EcologyExperts, Unsafe", "Player1", "Player2"))
    admin.phase("Prelude")
    p1.runOperation("12 MC, ProjectCard, PreludeCard, GreeneryTile<Tharsis_4_4>")

    with(p1) {
      playPrelude(EcologyExperts) {
        playProject(EcologicalZone, 12) { placeTile(4, 5) }
      }
    }

    p1.assertCounts(2 to "Animal<$EcologicalZone>")
  }

  // BGG Head Start action separation:
  // https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Test
  internal fun `Head Start incorrectly allows its two actions to interleave`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.phase("Prelude")
    p1.runOperation("4 MC, 10 ProjectCard, PreludeCard, 10 Heat")
    val checkpoint = game.timeline.checkpoint()

    p1.playPrelude(FakeHeadStart) {
      p1.assertCounts(2 to "Steel", 24 to "MC")
      doTask("UseAction<ConvertHeatAction, Action1>")
      doTask("8 Pay<Class<Heat>> FROM Heat")
      doTask("UseAction<UseStandardProjectAction, Action1>")
      doTask("UseAction<AquiferProject, Action1>")
      doTask("18 Pay<Class<MC>> FROM MC")
      placeTile(5, 5)
    }
    p1.auditGainsSince(checkpoint) shouldBe 1
  }

  // These pairings are unfixable under PP's gain-then-remove model.
  // These characterizations explicitly opt into the unsupported pairings.
  private fun startPreservationGeneration(
      corporation: String = "$PhoboLog",
      deal: Boolean = false,
  ) {
    newGame(PreludeExpansion, Prelude2CardPack, TurmoilExpansion, PromoCardPack, Unsafe)
    admin.phase("Prelude")
    p1.runOperation(
        "$corporation, PreservationProgram, 100 MC" + if (deal) ", $TerraformingDeal" else ""
    )
    admin.phase("Action")
    admin.nextGeneration(0, 0)
  }

  // BGG PP first-TR timing: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG TD effect is always active:
  // https://boardgamegeek.com/thread/3343166/article/44660687#44660687
  @Test
  internal fun `Terraforming Deal incorrectly pays for the TR reversed by Preservation Program`() {
    startPreservationGeneration(deal = true)
    p1.runOperation("ProjectCard, PROD[4 Energy]")
    p1.playProject(MagneticFieldGeneratorsPromo, 22) { placeTile(2, 4) }
        .expect("2 TerraformRating, -16 MC")
    p1.stdProject("AsteroidProject").expect("TerraformRating, -12 MC")
  }

  // BGG PP first-TR timing: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG Reds payment ruling: https://boardgamegeek.com/thread/2196388/article/31885882#31885882
  @Test
  internal fun `Reds incorrectly charges for the TR reversed by Preservation Program`() {
    startPreservationGeneration()
    admin.runOperation("Ruling<Reds> FROM Ruling")
    admin.phase("Production")
    admin.phase("Action")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating, -17 MC")
    p1.stdProject("AsteroidProject").expect("TerraformRating, -17 MC")
  }

  // BGG PP first-TR timing: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG Reds payment ruling: https://boardgamegeek.com/thread/2196388/article/31885882#31885882
  // BGG TD effect is always active:
  // https://boardgamegeek.com/thread/3343166/article/44660687#44660687
  @Test
  internal fun `Reds and Terraforming Deal incorrectly count PP's reversed TR for both payments`() {
    startPreservationGeneration(deal = true)
    admin.runOperation("Ruling<Reds> FROM Ruling")
    admin.phase("Production")
    admin.phase("Action")
    p1.runOperation("ProjectCard, PROD[4 Energy]")
    p1.playProject(MagneticFieldGeneratorsPromo, 22) { placeTile(2, 4) }
        .expect("2 TerraformRating, -25 MC")
  }

  // BGG PP first-TR timing: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG Reds payment ruling: https://boardgamegeek.com/thread/2196388/article/31885882#31885882
  @Test
  internal fun `Reds incorrectly requires money for PP's reversed TR after an unaffordable attempt`() {
    startPreservationGeneration()
    admin.runOperation("Ruling<Reds> FROM Ruling")
    admin.phase("Production")
    admin.phase("Action")
    p1.runOperation("-MC / MC")
    shouldThrow<LimitsException> { p1.runOperation("TerraformRating") }
    p1.runOperation("3 MC")
    p1.runOperation("TerraformRating").expect("0 TerraformRating, -3 MC")
    shouldThrow<LimitsException> { p1.runOperation("TerraformRating") }
    p1.runOperation("3 MC")
    p1.runOperation("TerraformRating").expect("TerraformRating, -3 MC")
  }

  // BGG PP first-TR timing: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG Reds payment ruling: https://boardgamegeek.com/thread/2196388/article/31885882#31885882
  @Test
  internal fun `Reds incorrectly rejects an affordable two step gain when PP reverses one`() {
    startPreservationGeneration()
    admin.runOperation("Ruling<Reds> FROM Ruling")
    admin.phase("Production")
    admin.phase("Action")
    p1.runOperation("-MC / MC")
    p1.runOperation("3 MC")
    val rating = p1.count("TerraformRating")
    shouldThrow<LimitsException> { p1.runOperation("2 TerraformRating") }
    p1.count("TerraformRating") shouldBe rating
    p1.count("MC") shouldBe 3
    p1.runOperation("TerraformRating").expect("0 TerraformRating, -3 MC")
    p1.runOperation("3 MC")
    p1.runOperation("TerraformRating").expect("TerraformRating, -3 MC")
  }

  // BGG PP first-TR timing: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG TD effect outside Action:
  // https://boardgamegeek.com/thread/3343166/article/44660753#44660753
  @Test
  internal fun `Terraforming Deal incorrectly pays for PP's reversed TR after a chairman award`() {
    startPreservationGeneration(deal = true)
    p1.runOperation("2 PartyDelegate<Scientists>")
    admin.phase("Solar")
    admin.runOperation("FormGovernment").expect("TerraformRating<Player1>, 2 MC<Player1>")
    admin.phase("Research") {
      p1.buyCards(0)
      requireP2().buyCards(0)
    }
    admin.phase("Action")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating, -12 MC")
  }

  // BGG PP first-TR timing: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG TD effect outside Action:
  // https://boardgamegeek.com/thread/3343166/article/44660753#44660753
  @Test
  internal fun `Terraforming Deal incorrectly pays for PP's reversed TR after the Reds ruling bonus`() {
    startPreservationGeneration(deal = true)
    p1.runOperation("-10 TerraformRating")
    admin.phase("Solar")
    admin.runOperation("ApplyRulingBonus<Reds>").expect("TerraformRating<Player1>, 2 MC<Player1>")
    admin.phase("Action")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating, -12 MC")
  }

  // BGG PP acquired later: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG Reds payment ruling: https://boardgamegeek.com/thread/2196388/article/31885882#31885882
  // BGG TD effect is always active:
  // https://boardgamegeek.com/thread/3343166/article/44660687#44660687
  @Test
  internal fun `Reds and Terraforming Deal incorrectly count PP's reversed TR when Valley Trust plays it`() {
    newGame(PreludeExpansion, Prelude2CardPack, TurmoilExpansion, PromoCardPack, Unsafe)
    p1.playCorp(ValleyTrust, 0)
    admin.phase("Prelude")
    p1.playPrelude(TerraformingDeal)
    p1.playPrelude(Donation)
    admin.runOperation("Ruling<Reds> FROM Ruling")
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(PreservationProgram) }
        .expect("4 TerraformRating, -5 MC")
    p1.stdProject("AsteroidProject").expect("TerraformRating, -15 MC")
  }

  // BGG PP first-TR timing: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG TD effect is always active:
  // https://boardgamegeek.com/thread/3343166/article/44660687#44660687
  @Test
  internal fun `Terraforming Deal incorrectly pays for PP's reversed TR when Pharmacy Union flips`() {
    startPreservationGeneration("$PharmacyUnion", deal = true)
    p1.runOperation("ProjectCard, -2 Disease<$PharmacyUnion>")
    p1.playProject(PhysicsComplex, 12) {
          doTask("PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion")
        }
        .expect("2 TerraformRating, -6 MC")
    p1.count("$PharmacyUnion") shouldBe 0
    p1.count("PlayedEvent<Class<$PharmacyUnion>>") shouldBe 1
  }

  // BGG PP first-TR timing: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG TD effect is always active:
  // https://boardgamegeek.com/thread/3343166/article/44660687#44660687
  @Test
  internal fun `Terraforming Deal incorrectly pays for PP's reversed TR in a multiple gain`() {
    startPreservationGeneration(deal = true)
    admin.runOperation("7 OxygenStep, 14 TemperatureStep")
    p1.runOperation("8 Plant")
    p1.convertPlants {
          doTask("GreeneryTile<Tharsis_2_4>")
          placeTile(1, 2)
        }
        .expect("OxygenStep, TemperatureStep, OceanTile, 2 TerraformRating, 10 MC")
  }

  // BGG PP first-TR timing: https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // BGG TD effect is always active:
  // https://boardgamegeek.com/thread/3343166/article/44660687#44660687
  @Test
  internal fun `Terraforming Deal incorrectly pays for PP's reversed TR from Venus`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        TurmoilExpansion,
        PromoCardPack,
        VenusNextExpansion,
        Unsafe,
    )
    admin.phase("Prelude")
    p1.runOperation("$PhoboLog, PreservationProgram, $TerraformingDeal, 100 MC")
    admin.phase("Action")
    admin.nextGeneration(0, 0)
    admin.runOperation("7 VenusStep")
    p1.stdProject("AirScrappingProject").expect("VenusStep, TerraformRating, -11 MC")
    p1.stdProject("AsteroidProject").expect("TerraformRating, -12 MC")
  }

  // BGG exact nested Sagitta ruling:
  // https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Test
  internal fun `Sagitta incorrectly misses Merger in Head Start's nested action`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, FakeStuffBundle, Unsafe)
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

  // BGG Sagitta/Merger ruling: https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Test
  internal fun `Sagitta misses Merger without Head Start too`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, Unsafe)
    p1.runOperation("54 MC")
    admin.phase("Prelude")

    p1.turn { playPrelude(Merger) { playCorp(SagittaFrontierServices) } }

    // Jacob rules that Sagitta earns 4 MC for the tagless Merger as well as for itself.
    p1.count("MC") shouldBe 47
  }

  // BGG SRR eligibility ruling: https://boardgamegeek.com/thread/1861808/article/28914878#28914878
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

  // Resolved FAQ: Viral Enhancers offers animals or microbes, never disease resources.
  @Test
  internal fun `Viral Enhancers incorrectly adds diseases to Pharmacy Union acquired through Merger`() {
    newGame(PreludeExpansion, PromoCardPack)
    playCorporationWithoutStartingProjects(p1, CrediCor)
    p1.runOperation("$ViralEnhancers") { doTask("Plant") }
    admin.phase("Prelude")
    p1.runOperation("PreludeCard")

    p1.playPrelude(Merger) {
          p1.playCorp(PharmacyUnion) {
            doTask("CardResource<$PharmacyUnion>")
            doTask("CardResource<$PharmacyUnion>")
          }
        }
        .expect("4 Disease<$PharmacyUnion>, 0 Plant")
  }

  // Resolved FAQ: Advisor may choose a completed parameter to do nothing.
  // Earlier discussion: https://boardgamegeek.com/thread/3348438/article/44693194#44693194
  @Test
  internal fun `World Government Advisor incorrectly rejects a completed parameter while another is available`() {
    newGame(PreludeExpansion, Prelude2CardPack, VenusNextExpansion)
    p1.runOperation("$WorldGovernmentAdvisor")
    admin.runOperation("15 VenusStep")
    admin.phase("Action")
    val trBefore = p1.count("TerraformRating")

    shouldThrow<LimitsException> {
      p1.cardAction1(WorldGovernmentAdvisor) { wgt("VenusStep") }
    }
    p1.count("ActionUsedMarker<$WorldGovernmentAdvisor>") shouldBe 0
    p1.count("TerraformRating") shouldBe trBefore
    admin.count("VenusStep") shouldBe 15
    p1.cardAction1(WorldGovernmentAdvisor) { wgt("TemperatureStep") }
    admin.count("TemperatureStep") shouldBe 1
  }

  // Resolved FAQ: a corporation acquired after Prelude must take its first action immediately.
  // Earlier discussion:
  // https://boardgamegeek.com/thread/2886401/article/44823945#44823945
  @Test
  internal fun `Board Merger Tharsis incorrectly defers its city until the next action`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack)
    p1.playCorp(CrediCor, 0)
    admin.phase("Prelude")
    p1.playPrelude(BoardOfDirectors)
    p1.playPrelude(Donation)
    admin.phase("Action")

    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(Merger) { p1.playCorp(TharsisRepublic) }
    }
    p1.count("CityTile") shouldBe 0
    p1.stdAction("DoRequiredActionsAction") { placeTile(3, 3) }
    p1.count("CityTile<Tharsis_3_3>") shouldBe 1
  }
}
