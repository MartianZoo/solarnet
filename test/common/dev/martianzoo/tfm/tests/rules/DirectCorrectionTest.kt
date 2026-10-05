package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.exMachina
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.state.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.CrashSiteCleanup
import dev.martianzoo.tfm.tests.cards.cardnames.DevelopmentCenter
import dev.martianzoo.tfm.tests.cards.cardnames.LawSuit
import dev.martianzoo.tfm.tests.cards.cardnames.StJosephOfCupertinoMission
import dev.martianzoo.tfm.tests.cards.cardnames.UnitedNationsMarsInitiative
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DirectCorrectionTest : CardTest() {
  @Test
  internal fun correctedOpponentPlantLossDoesNotQualifyCrashSiteCleanup() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("4 MC, ProjectCard")
    requireP2().runOperation("2 Plant")

    agents.exMachina(p1.actor, "-Plant<Player2>")

    shouldThrow<RequirementException> {
      p1.playProject(CrashSiteCleanup, 4) { doTask("Titanium") }
    }
    p1.runOperation("-Plant<Player2>")
    p1.playProject(CrashSiteCleanup, 4) { doTask("Titanium") }.expect("Titanium")
  }

  @Test
  internal fun correctedOpponentProductionLossDoesNotQualifyLawSuit() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("3 MC, ProjectCard, PROD[2 Plant]")
    val p2 = requireP2()
    p2.runOperation("5 MC")

    agents.exMachina(p2.actor, "PROD[-Plant<Player1>]")

    shouldThrow<NarrowingException> {
      p1.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Player2>") }
    }
    p2.runOperation("PROD[-Plant<Player1>]")
    p1.playProject(LawSuit, 2) {
          doTask("3 MC FROM MC<Player2>")
        }
        .expect("MC<Player1>, -3 MC<Player2>")
  }

  @Test
  internal fun correctedSoloReservesKeepResourceAndProductionAdjustments() {
    newGame(players = 1)

    agents.exMachina(
        p1.actor,
        "-5 Plant<SoloOpponent>, PROD[-5 Plant<SoloOpponent>], " +
            "-5 Animal<SoloOpponent, SoloCardResourceReserve<Class<Animal>>>",
    )

    admin.assertCounts(
        37 to "Plant<SoloOpponent>",
        37 to "Production<SoloOpponent, Class<Plant>>",
        37 to "Animal<SoloOpponent, SoloCardResourceReserve<Class<Animal>>>",
    )
    agents.exMachina(
        p1.actor,
        "3 Plant<SoloOpponent>, PROD[3 Plant<SoloOpponent>], " +
            "3 Animal<SoloOpponent, SoloCardResourceReserve<Class<Animal>>>",
    )
    admin.assertCounts(
        40 to "Plant<SoloOpponent>",
        40 to "Production<SoloOpponent, Class<Plant>>",
        40 to "Animal<SoloOpponent, SoloCardResourceReserve<Class<Animal>>>",
    )
  }

  @Test
  internal fun correctedEventsCanMoveBetweenPositionsAndRemoveTheirPositionsWithThem() {
    newGame(TurmoilExpansion)
    val delegates = admin.count("PartyDelegate<Neutral>")

    agents.exMachina(p1.actor, "Current<DryDeserts> FROM Distant<DryDeserts>")
    agents.exMachina(p1.actor, "Distant<DryDeserts> FROM Current<DryDeserts>")
    admin.assertCounts(1 to "DryDeserts", 1 to "Distant<DryDeserts>")

    agents.exMachina(
        p1.actor,
        "Coming<DryDeserts> FROM Distant<DryDeserts>, " +
            "Distant<AquiferReleasedByPublicCouncil> FROM Coming<AquiferReleasedByPublicCouncil>",
    )
    admin.assertCounts(1 to "Coming<DryDeserts>", 1 to "Distant<AquiferReleasedByPublicCouncil>")
    admin.count("PartyDelegate<Neutral>") shouldBe delegates

    agents.exMachina(p1.actor, "-DryDeserts")
    admin.assertCounts(0 to "DryDeserts", 0 to "Coming")
    shouldThrow<DependencyException> { agents.exMachina(p1.actor, "Coming<DryDeserts>") }

    agents.exMachina(p1.actor, "DryDeserts, Coming<DryDeserts>")
    admin.assertCounts(1 to "DryDeserts", 1 to "Coming<DryDeserts>")
    admin.count("PartyDelegate<Neutral>") shouldBe delegates
  }

  @Test
  internal fun partiesCannotBeRemovedEvenByCorrection() {
    newGame(TurmoilExpansion)

    shouldThrow<LimitsException> { agents.exMachina(p1.actor, "-Kelvinists") }

    admin.assertCounts(1 to "Kelvinists", 1 to "PartyDistance<Kelvinists, Reds>")
  }

  @Test
  internal fun correctedTrDoesNotQualifyUnmi() {
    newGame()
    p1.playCorp(UnitedNationsMarsInitiative, 0)
    admin.phase("Action")

    agents.exMachina(p1.actor, "TerraformRating")

    shouldThrow<RequirementException> { p1.cardAction1(UnitedNationsMarsInitiative) }
    p1.stdProject("AsteroidProject").expect("TerraformRating")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("TerraformRating, -3 MC")
  }

  @Test
  internal fun correctedTrDoesNotDisqualifyPristar() {
    newGame(TurmoilExpansion)
    p1.runOperation("Pristar")

    agents.exMachina(p1.actor, "TerraformRating")
    admin.phase("Production")

    p1.count("Preservation") shouldBe 1
  }

  @Test
  internal fun correctedProductionLeavesSuitableInfrastructureBonusForTheActualAction() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("SuitableInfrastructure, 50 MC")
    admin.phase("Action")

    p1.stdProject(
        "PowerPlantProject",
        payment = {
          val money = p1.count("MC")
          agents.exMachina(p1.actor, "Production<Class<Steel>>")
          p1.assertCounts(money to "MC", 1 to "SuitableInfrastructureBonus")
          p1.pay(11)
        },
    )

    p1.assertCounts(41 to "MC", 0 to "SuitableInfrastructureBonus")
  }

  @Test
  internal fun correctedPlacementLeavesFrontierTownBonusForItsActualCity() {
    newGame(PreludeExpansion, Prelude2CardPack, TurmoilExpansion)
    p1.runOperation("PROD[Energy]")

    p1.runOperation("FrontierTown") {
      agents.exMachina(p1.actor, "NormalCityTile<Tharsis_8_8>")
      p1.assertCounts(1 to "FrontierTownBonus", 0 to "Plant")
      placeTile(4, 2)
    }

    p1.assertCounts(3 to "Plant", 0 to "FrontierTownBonus")
  }

  @Test
  internal fun correctedCardsHaveTagsCapabilitiesAndAutomaticButNotQueuedRewards() {
    newGame(PromoCardPack)

    agents.exMachina(p1.actor, "PharmacyUnion, PhoboLog, AdvancedAlloys")

    p1.assertCounts(
        2 to "MicrobeTag<PharmacyUnion>",
        1 to "SpaceTag<PhoboLog>",
        1 to "GrantedResourceValue<Class<Titanium>, PhoboLog>",
        1 to "GrantedResourceValue<Class<Titanium>, AdvancedAlloys>",
        1 to "GrantedResourceValue<Class<Steel>, AdvancedAlloys>",
        54 to "MC",
        0 to "ProjectCard",
        0 to "Disease",
    )
    shouldThrow<LimitsException> { agents.exMachina(p1.actor, "-MicrobeTag<PharmacyUnion>") }

    agents.exMachina(p1.actor, "-PharmacyUnion, -PhoboLog")

    p1.assertCounts(
        0 to "MicrobeTag",
        0 to "SpaceTag",
        1 to "GrantedResourceValue<Class<Titanium>>",
        1 to "GrantedResourceValue<Class<Steel>>",
        54 to "MC",
    )
  }

  @Test
  internal fun correctingAnOvercountOnPetsDoesNotPerformAForbiddenAnimalAttack() {
    newGame()
    p1.runOperation("Pets, Animal<Pets>")

    shouldThrow<DeadEndException> { p1.runOperation("-Animal<Pets>") }
    agents.exMachina(p1.actor, "-Animal<Pets>")

    p1.assertCounts(1 to "Animal<Pets>", 1 to "AnimalTag<Pets>")
  }

  @Test
  internal fun correctedTilesMaintainAdjacencyWithoutTerraformingOrPlacementRewards() {
    newGame()
    val rating = p1.count("TerraformRating")

    agents.exMachina(p1.actor, "GreeneryTile<Tharsis_3_4>, NormalCityTile<Tharsis_3_3>")

    p1.assertCounts(2 to "Adjacency", 0 to "OxygenStep", rating to "TerraformRating", 0 to "Plant")
    agents.exMachina(p1.actor, "-GreeneryTile<Tharsis_3_4>")
    p1.assertCounts(0 to "Adjacency", 1 to "CityTile")
  }

  @Test
  internal fun correctedParametersMaintainCompletionAndDoNotOfferThresholdRewards() {
    newGame()
    val rating = p1.count("TerraformRating")

    admin.sneak("15 TemperatureStep")
    agents.exMachina(p1.actor, "4 TemperatureStep")

    admin.assertCounts(
        1 to "GpComplete<Class<TemperatureStep>>",
        0 to "GpIncomplete<Class<TemperatureStep>>",
        0 to "GpGameEndBarrier<Class<TemperatureStep>>",
        0 to "AdminOceanPlacement",
    )
    p1.count("TerraformRating") shouldBe rating
    shouldThrow<DeadEndException> { agents.exMachina(p1.actor, "-TemperatureStep") }
    admin.count("TemperatureStep") shouldBe 19
  }

  @Test
  internal fun correctedColoniesRaiseTheSharedTrackWithoutPlacementBonuses() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2, "Luna"))
    val p2 = requireP2()
    val p1Production = p1.count("PROD[MC]")
    val p2Production = p2.count("PROD[MC]")

    agents.exMachina(p1.actor, "Colony<Luna>")
    agents.exMachina(p2.actor, "Colony<Luna>")

    admin.count("ColonyProduction<Luna>") shouldBe 2
    p1.count("PROD[MC]") shouldBe p1Production
    p2.count("PROD[MC]") shouldBe p2Production
  }

  @Test
  internal fun aCorrectionMayLowerTheColonyTrackBelowTheColonyCount() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2, "Luna"))
    p1.runOperation("Colony<Luna>")
    requireP2().runOperation("Colony<Luna>")

    agents.exMachina(p1.actor, "-ColonyProduction<Luna>")

    // The normal placement floor is a gameplay rule, not a correction invariant.
    admin.assertCounts(2 to "Colony<Luna, Anyone>", 1 to "ColonyProduction<Luna>")
  }

  @Test
  internal fun callersCanCorrectLeadershipAfterPartiallyRemovingDelegates() {
    newGame(TurmoilExpansion)
    p1.runOperation("3 PartyDelegate<Scientists>")
    val p2 = requireP2()
    p2.runOperation("2 PartyDelegate<Scientists>")

    agents.exMachina(p1.actor, "-2 PartyDelegate<Scientists>")

    p1.assertCounts(1 to "PartyDelegate<Scientists>", 1 to "PartyLeader<Scientists>")
    p2.assertCounts(2 to "PartyDelegate<Scientists>", 0 to "PartyLeader<Scientists>")

    agents.exMachina(p2.actor, "PartyLeader<Scientists>")

    p1.count("PartyLeader<Scientists>") shouldBe 0
    p2.count("PartyLeader<Scientists>") shouldBe 1
  }

  @Test
  internal fun correctedAridorRetainsItsFutureTagRewards() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    agents.exMachina(p1.actor, "Aridor")
    p1.assertCounts(0 to "MC", 0 to "RequiredAction")

    p1.runOperation("$DevelopmentCenter").expect("PROD[2 MC]")

    agents.exMachina(p1.actor, "-Aridor")
    p1.count("AridorTagWatcher") shouldBe 0
  }

  @Test
  internal fun correctedStJosephRetainsItsActionWithoutAPlayEffect() {
    newGame(PromoCardPack)
    agents.exMachina(p1.actor, "StJosephOfCupertinoMission, NormalCityTile<Tharsis_3_3>, 7 MC")
    admin.phase("Action")

    p1.cardAction1(StJosephOfCupertinoMission) {
          p1.pay(5)
          doTask("Cathedral<NormalCityTile<Tharsis_3_3>>")
          doTask("UseAction<CathedralOption, Action1>")
          p1.pay(2)
        }
        .expect("Cathedral, ProjectCard, -7 MC")

    agents.exMachina(p1.actor, "-StJosephOfCupertinoMission")
    p1.assertCounts(1 to "Cathedral", 0 to "CathedralOption")
    admin
        .runOperation("End FROM Phase")
        .expect("${p1.count("TerraformRating")} VictoryPoint<Player1>")
  }

  @Test
  internal fun correctedBoomTownInstallsAndRemovesItsOngoingModifier() {
    newGame(PromoCardPack)
    agents.exMachina(p1.actor, "BoomTown")
    p1.assertCounts(2 to "BaseResourceValue<Class<Titanium>>", 0 to "MC", 0 to "CityTile")

    agents.exMachina(p1.actor, "-BoomTown")
    p1.count("BaseResourceValue<Class<Titanium>>") shouldBe 3
  }

  @Test
  internal fun correctedBriberDoesNotChargeForARecordedClaim() {
    newGame(GameConfig("Briber, Builder, Engineer", "Player1", "Player2"))

    agents.exMachina(p1.actor, "Briber")

    p1.assertCounts(1 to "Briber", 0 to "MC")
  }

  @Test
  internal fun preservationProgramCancelsTheFirstCorrectedTrAndRecordsItsUse() {
    newGame(GameConfig("PreservationProgram", "Player1", "Player2"))
    agents.exMachina(p1.actor, "PreservationProgram")
    admin.phase("Action")
    val rating = p1.count("TerraformRating")

    agents.exMachina(p1.actor, "TerraformRating")
    p1.count("TerraformRating") shouldBe rating
    agents.exMachina(p1.actor, "TerraformRating")
    p1.count("TerraformRating") shouldBe rating + 1
  }

  @Test
  internal fun correctedRulingReplacesTheCurrentPolicyAndItsDependentValues() {
    newGame(TurmoilExpansion)
    admin.phase("Action")

    admin.sneak("Ruling<Unity> FROM Ruling<Greens>")
    admin.assertCounts(1 to "UnityPolicy", 0 to "GreensPolicy")
    p1.count("UnityTitaniumValue") shouldBe 1
    requireP2().count("UnityTitaniumValue") shouldBe 1

    admin.sneak("Ruling<Scientists> FROM Ruling<Unity>")
    admin.assertCounts(1 to "ScientistsPolicy", 0 to "UnityPolicy")
    p1.count("UnityTitaniumValue") shouldBe 0
  }
}
