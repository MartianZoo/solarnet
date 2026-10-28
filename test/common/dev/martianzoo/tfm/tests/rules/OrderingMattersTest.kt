package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.state.Task
import dev.martianzoo.tfm.engine.TfmAutoExec
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * The runs of each scenario start with the same Canon cards and make the same card choices in a
 * single action. Only the position of the named concrete gain in that action changes, and the
 * outcomes differ; the Canon register must therefore refuse to claim that gain early. Setup
 * corrections, where used, establish resources or previously played Canon cards; no tasks are
 * inserted into the queue.
 */
internal class OrderingMattersTest : TfmSandboxTest() {
  @Test
  internal fun `MC gained before Pharmacy Union's loss is lost`() {
    moneyAfterTardigrades(gainFirst = true) shouldBe 0
    moneyAfterTardigrades(gainFirst = false) shouldBe 2
  }

  private fun moneyAfterTardigrades(gainFirst: Boolean): Int {
    newTestGame(
        addOptions = "TurmoilExpansion, GmoContract, PharmacyUnion, Tardigrades",
    )
    // These cards could have been played earlier, with GMO Contract's delegate requirement met.
    kim.exMachina("$PharmacyUnion, $GmoContract")
    kim.setToExMachina(4, "MC")
    kim.inTurn {
      doTasks("PlayProject<Class<Tardigrades>>", "4 Pay<> FROM MC", "Tardigrades FROM ProjectCard")
      kim.count("MC") shouldBe 0
      claimableNow("MC", 2) shouldBe false
      doWithAutoExecPolicy(NONE) {
        if (gainFirst) doTask("2 MC")
        doTasks("Disease<PharmacyUnion>", "-4* MC")
        if (!gainFirst) doTask("2 MC")
      }
    }
    return kim.count("MC")
  }

  @Test
  internal fun `the same MC gain is claimable without Pharmacy Union in play`() {
    newTestGame(addOptions = "TurmoilExpansion, GmoContract, PharmacyUnion, Tardigrades")
    kim.exMachina("$GmoContract")
    kim.setToExMachina(4, "MC")
    kim.inTurn {
      doTasks("PlayProject<Class<Tardigrades>>", "4 Pay<> FROM MC", "Tardigrades FROM ProjectCard")
      claimableNow("MC", 2) shouldBe true
      doTask("2 MC")
    }
  }

  @Test
  internal fun `MC gained before Mons Insurance settles an attack funds compensation`() {
    insuranceAfterHiredRaiders(gainFirst = true) shouldBe 0
    insuranceAfterHiredRaiders(gainFirst = false) shouldBe 3
  }

  private fun insuranceAfterHiredRaiders(gainFirst: Boolean): Int {
    newTestGame(addOptions = "MonsInsurance, MediaGroup, HiredRaiders")
    kim.exMachina("$MonsInsurance, $MediaGroup")
    kim.setToExMachina(1, "MC")
    stan.setToExMachina(2, "Steel")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<HiredRaiders>>",
          "1 Pay<> FROM MC",
          "HiredRaiders FROM ProjectCard",
      )
      kim.count("MC") shouldBe 0
      claimableNow("MC", 3) shouldBe false
      doWithAutoExecPolicy(NONE) {
        if (gainFirst) doTask("3 MC")
        doTask("2 Steel<Kim FROM Stan>")
        doTask(if (gainFirst) "3 MC<Stan FROM Kim>" else "0 MC<Stan FROM Kim>")
        if (!gainFirst) doTask("3 MC")
      }
    }
    return kim.count("MC")
  }

  @Test
  internal fun `MC gained while a nested project is pending can be lost to its later Prelude`() {
    moneyAfterNestedRecession(gainFirst = true) shouldBe 0
    moneyAfterNestedRecession(gainFirst = false) shouldBe 2
  }

  private fun moneyAfterNestedRecession(gainFirst: Boolean): Int {
    newTestGame(
        addOptions =
            "PreludeExpansion, TurmoilExpansion, WgProject, BoardOfDirectors, EcologyExperts, " +
                "ProjectInspection, Recession, SpliceTacticalGenomics, -ViralEnhancers, " +
                "-EcologicalZone, -Decomposers, -GmoContract",
    )
    // Splice could have been played earlier. Its starting OR benefit is irrelevant here.
    kim.exMachina("$SpliceTacticalGenomics")
    stan.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
          "4 Director<BoardOfDirectors>",
      )
    }
    advanceUntilActionPhase()
    stan.setToExMachina(100, "MC")
    stan.inTurn {
      doTasks(
          "UseCardAction<BoardOfDirectors, Action1>",
          "PreludeCard<Stan, Hand>",
          "-12 MC",
          "-Director<BoardOfDirectors>",
          "PlayCard<Class<PreludeCard>, Class<Donation>>",
          "Donation FROM PreludeCard",
          "21 MC",
      )
    }
    stan.exMachina("-Chairman<Neutral>, Chairman")
    kim.setToExMachina(0, "MC")

    stan.inTurn {
      doTasks(
          "PlayProject<Class<WgProject>>",
          "9 Pay<> FROM MC",
          "WgProject FROM ProjectCard",
          "3 PreludeCard<Stan, Selecting>",
          "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
          "EcologyExperts FROM PreludeCard<Selecting>",
      )
      // Splice grants Kim a fixed two MC for Ecology Experts' microbe tag. The same Prelude
      // leaves a project play pending, so SAFE must leave Kim's reward in the queue.
      claimableNow("MC", 2) shouldBe false
      doTask("PROD[Plant]")
      doTask("PlayCard<Class<ProjectCard>, Class<ProjectInspection>>")
      // The PlayCard signal has been claimed, but its payment/placement continuation can still
      // produce Project Inspection's action use, Board of Directors, and Recession.
      claimableNow("MC", 2) shouldBe false
      if (gainFirst) kim.doTask("2 MC")
      doTask("ProjectInspection FROM ProjectCard")
      doTasks(
          "UseAction<BoardOfDirectors, Action1>",
          "PreludeCard<Stan, Hand>",
          "-12 MC",
          "-Director<BoardOfDirectors>",
          "PlayCard<Class<PreludeCard>, Class<Recession>>",
          "Recession FROM PreludeCard",
          "10 MC",
      )
      selectTask("Ok OR 2! MC<Stan>")
      narrowTask("Ok") // Decline Splicer's separate reward to Stan.
      selectTask("-Required* / Required") // Finish Ecology Experts' requirement waiver.
      selectOnlyTask() // Expand Recession's mandatory effects on Kim and Rob.
      if (!gainFirst) claimableNow("MC", 2) shouldBe false
      doTask("-5* MC<Kim>")
      doTask("PROD[-MC<Kim>]")
      doTask("-5* MC<Rob>")
      doTask("PROD[-MC<Rob>]")
      if (!gainFirst && pendingGain("MC", 2)) kim.doTask("2 MC")
    }
    return kim.count("MC")
  }

  @Test
  internal fun `Aridor MC production gained before Manutech enters misses its payout`() {
    val early = mcProductionAfterWgProject(gainFirst = true)
    val late = mcProductionAfterWgProject(gainFirst = false)
    early.first shouldBe late.first
    late.second - early.second shouldBe 1
  }

  private fun mcProductionAfterWgProject(gainFirst: Boolean): Pair<Int, Int> {
    newTestGame(
        addOptions =
            "PreludeExpansion, TurmoilExpansion, WgProject, Merger, Manutech, -QuickStartVariant",
        kimCorporation = Aridor,
    )
    advanceUntilActionPhase()
    kim.inTurn {
      doTask("DoRequiredActions")
      selectTask("ColonyTileSelection!")
      selectTask("Europa!")
    }
    // An earlier Building card means Manutech's tag will not queue another Aridor production gain.
    kim.exMachina("$Mine")
    kim.exMachina("-Chairman<Neutral>, Chairman")
    kim.setToExMachina(100, "MC")
    val productionBeforeWgProject = kim.count("PROD[MC]") - kim.count("ProdOffset<Class<MC>>")
    kim.inTurn {
      // WG's first Earth tag queues Aridor's gain beside WG's immediate Prelude play.
      doTasks(
          "PlayProject<Class<WgProject>>",
          "9 Pay<> FROM MC",
          "WgProject FROM ProjectCard",
      )
      (kim.count("PROD[MC]") - kim.count("ProdOffset<Class<MC>>")) shouldBe
          productionBeforeWgProject
      claimableNow("PROD[MC]") shouldBe false
      doWithAutoExecPolicy(NONE) {
        if (gainFirst) doTask("PROD[MC]")
        doTask("3 PreludeCard<Selecting>")
        doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard<Selecting>")
        doTasks("4 CorporationCard<Selecting>", "-42 MC")
        doTasks(
            "PlayCard<Class<CorporationCard>, Class<Manutech>>",
            "Manutech FROM CorporationCard<Selecting>",
            "35 MC",
            "PROD[Steel]",
            "Steel",
        )
        if (!gainFirst) doTasks("PROD[MC]", "MC")
      }
    }
    return (kim.count("PROD[MC]") - kim.count("ProdOffset<Class<MC>>")) to kim.count("MC")
  }

  @Test
  internal fun `Plant production gained before Manutech enters misses its payout`() {
    val early = plantProductionAfterEcologyExperts(gainFirst = true)
    val late = plantProductionAfterEcologyExperts(gainFirst = false)
    early.first shouldBe late.first
    late.second - early.second shouldBe 1
  }

  private fun plantProductionAfterEcologyExperts(gainFirst: Boolean): Pair<Int, Int> {
    // These exclusions admit Ecology Experts without the Unsafe option.
    newTestGame(
        addOptions =
            "PreludeExpansion, TurmoilExpansion, BoardOfDirectors, EcologyExperts, WgProject, " +
                "Merger, Manutech, -ViralEnhancers, -EcologicalZone, -Decomposers, -GmoContract",
    )
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
          "4 Director<BoardOfDirectors>",
      )
    }
    advanceUntilActionPhase()
    kim.exMachina("-Chairman<Neutral>, Chairman")
    kim.setToExMachina(100, "MC")
    val productionBeforeEcologyExperts =
        kim.count("PROD[Plant]") - kim.count("ProdOffset<Class<Plant>>")
    kim.inTurn {
      doTasks(
          "UseCardAction<BoardOfDirectors, Action1>",
          "PreludeCard<Kim, Hand>",
          "-12 MC",
          "-Director<BoardOfDirectors>",
          "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
          "EcologyExperts FROM PreludeCard",
      )
      (kim.count("PROD[Plant]") - kim.count("ProdOffset<Class<Plant>>")) shouldBe
          productionBeforeEcologyExperts
      claimableNow("PROD[Plant]") shouldBe false
      doWithAutoExecPolicy(NONE) {
        if (gainFirst) doTask("PROD[Plant]")
        doTask("PlayCard<Class<ProjectCard>, Class<WgProject>>")
        selectTask("-Required* / Required")
        doTasks("9 Pay<> FROM MC", "WgProject FROM ProjectCard")
        doTask("3 PreludeCard<Selecting>")
        doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard<Selecting>")
        doTasks("4 CorporationCard<Selecting>", "-42 MC")
        doTasks(
            "PlayCard<Class<CorporationCard>, Class<Manutech>>",
            "Manutech FROM CorporationCard<Selecting>",
            "35 MC",
            "PROD[Steel]",
            "Steel",
        )
        if (!gainFirst) doTasks("PROD[Plant]", "Plant")
      }
    }
    return (kim.count("PROD[Plant]") - kim.count("ProdOffset<Class<Plant>>")) to kim.count("Plant")
  }

  @Test
  internal fun `Steel production gained before Cyberia copies Industrial Complex loses a second increase`() {
    steelProductionAfterCyberia(gainFirst = true) shouldBe 1
    steelProductionAfterCyberia(gainFirst = false) shouldBe 2
  }

  private fun steelProductionAfterCyberia(gainFirst: Boolean): Int {
    prepareUtopiaWithLoanAndComplex()
    kim.inTurn {
      doTasks(
          "PlayProject<Class<TitaniumMine>>",
          "7 Pay<> FROM MC",
          "TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
    kim.inTurn {
      doTasks("UseCardAction<UtopiaInvest, Action1>", "PROD[-Steel] THEN 4 Steel", "4 Steel")
    }
    val productionBeforeCyberia = kim.count("PROD[Steel]") - kim.count("ProdOffset<Class<Steel>>")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CyberiaSystems>>",
          "16 Pay<> FROM MC",
          "CyberiaSystems FROM ProjectCard",
      )
      (kim.count("PROD[Steel]") - kim.count("ProdOffset<Class<Steel>>")) shouldBe
          productionBeforeCyberia
      claimableNow("PROD[Steel]") shouldBe false
      doWithAutoExecPolicy(NONE) {
        if (gainFirst) doTask("PROD[Steel]")
        doTask("CopyProductionBox<IndustrialComplex>")
        selectTask(COMPLEX_PRODUCTION_BOX)
        // The selected IC box must finish before the matching Cyberia gain can be claimed.
        if (!gainFirst) doTasks("PROD[Steel]", "PROD[Steel]")
        doTasks("CopyProductionBox<TitaniumMine>", "PROD[Titanium]")
      }
    }
    return kim.count("PROD[Steel]") - kim.count("ProdOffset<Class<Steel>>")
  }

  // The next three cases characterize the current model. UnknownRulesTest records the unresolved
  // question whether Cyberia's first copied box may leave production work pending while its second
  // copied box resolves. They are not proofs of an adjudicated physical-game order.
  @Test
  internal fun `Titanium production currently gained before Cyberia copies Industrial Complex loses a second increase`() {
    titaniumProductionAfterCyberia(gainFirst = true) shouldBe 1
    titaniumProductionAfterCyberia(gainFirst = false) shouldBe 2
  }

  private fun titaniumProductionAfterCyberia(gainFirst: Boolean): Int {
    prepareUtopiaWithLoanAndComplex()
    kim.inTurn {
      doTasks(
          "PlayProject<Class<TitaniumMine>>",
          "7 Pay<> FROM MC",
          "TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
    repeat(2) { step ->
      if (step > 0) advanceUntilActionPhase()
      useUtopiaToLowerProduction("Titanium")
    }
    val productionBeforeCyberia =
        kim.count("PROD[Titanium]") - kim.count("ProdOffset<Class<Titanium>>")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CyberiaSystems>>",
          "16 Pay<> FROM MC",
          "CyberiaSystems FROM ProjectCard",
          "PROD[Steel]",
          "CopyProductionBox<TitaniumMine>",
      )
      (kim.count("PROD[Titanium]") - kim.count("ProdOffset<Class<Titanium>>")) shouldBe
          productionBeforeCyberia
      claimableNow("PROD[Titanium]") shouldBe false
      doWithAutoExecPolicy(NONE) {
        if (gainFirst) doTask("PROD[Titanium]")
        doTask("CopyProductionBox<IndustrialComplex>")
        selectTask(COMPLEX_PRODUCTION_BOX)
        if (!gainFirst) doTasks("PROD[Titanium]", "PROD[Titanium]")
      }
    }
    return kim.count("PROD[Titanium]") - kim.count("ProdOffset<Class<Titanium>>")
  }

  @Test
  internal fun `Energy production currently gained before Cyberia copies Industrial Complex loses a second increase`() {
    energyProductionAfterCyberia(gainFirst = true) shouldBe 1
    energyProductionAfterCyberia(gainFirst = false) shouldBe 2
  }

  private fun energyProductionAfterCyberia(gainFirst: Boolean): Int {
    prepareUtopiaWithLoanAndComplex()
    kim.inTurn {
      doTasks(
          "PlayProject<Class<IndustrialMicrobes>>",
          "12 Pay<> FROM MC",
          "IndustrialMicrobes FROM ProjectCard",
          "PROD[Energy]",
          "PROD[Steel]",
      )
    }
    repeat(2) { step ->
      if (step > 0) advanceUntilActionPhase()
      useUtopiaToLowerProduction("Energy")
    }
    val productionBeforeCyberia = kim.count("PROD[Energy]") - kim.count("ProdOffset<Class<Energy>>")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CyberiaSystems>>",
          "16 Pay<> FROM MC",
          "CyberiaSystems FROM ProjectCard",
          "PROD[Steel]",
          "CopyProductionBox<IndustrialMicrobes>",
          "PROD[Steel]",
      )
      (kim.count("PROD[Energy]") - kim.count("ProdOffset<Class<Energy>>")) shouldBe
          productionBeforeCyberia
      claimableNow("PROD[Energy]") shouldBe false
      doWithAutoExecPolicy(NONE) {
        if (gainFirst) doTask("PROD[Energy]")
        doTask("CopyProductionBox<IndustrialComplex>")
        selectTask(COMPLEX_PRODUCTION_BOX)
        if (!gainFirst) doTasks("PROD[Energy]", "PROD[Energy]")
      }
    }
    return kim.count("PROD[Energy]") - kim.count("ProdOffset<Class<Energy>>")
  }

  @Test
  internal fun `Heat production currently gained before Cyberia copies Industrial Complex loses a second increase`() {
    heatProductionAfterCyberia(gainFirst = true) shouldBe 4
    heatProductionAfterCyberia(gainFirst = false) shouldBe 5
  }

  private fun heatProductionAfterCyberia(gainFirst: Boolean): Int {
    prepareUtopiaWithLoanAndComplex()
    kim.inTurn {
      doTasks(
          "PlayProject<Class<MoholeArea>>",
          "20 Pay<> FROM MC",
          "MoholeArea FROM ProjectCard",
          "PROD[4 Heat]",
          "MoholeArea_SpecialTile<Tharsis_1_5>",
      )
    }
    repeat(5) { step ->
      if (step > 0) advanceUntilActionPhase()
      useUtopiaToLowerProduction("Heat")
    }
    val productionBeforeCyberia = kim.count("PROD[Heat]") - kim.count("ProdOffset<Class<Heat>>")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CyberiaSystems>>",
          "16 Pay<> FROM MC",
          "CyberiaSystems FROM ProjectCard",
          "PROD[Steel]",
          "CopyProductionBox<MoholeArea>",
      )
      (kim.count("PROD[Heat]") - kim.count("ProdOffset<Class<Heat>>")) shouldBe
          productionBeforeCyberia
      claimableNow("PROD[Heat]", 4) shouldBe false
      doWithAutoExecPolicy(NONE) {
        if (gainFirst) doTask("PROD[4 Heat]")
        doTask("CopyProductionBox<IndustrialComplex>")
        selectTask(COMPLEX_PRODUCTION_BOX)
        if (!gainFirst) doTasks("PROD[4 Heat]", "PROD[Heat]")
      }
    }
    return kim.count("PROD[Heat]") - kim.count("ProdOffset<Class<Heat>>")
  }

  private fun prepareUtopiaWithLoanAndComplex() {
    newTestGame(
        addOptions = "PreludeExpansion, IndustrialComplex, UtopiaInvest, -QuickStartVariant",
        kimCorporation = UtopiaInvest,
    )
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<Loan>>",
          "Loan FROM PreludeCard",
          "PROD[-2 MC]",
          "30 MC",
      )
    }
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>",
          "IndustrialComplex FROM PreludeCard",
          "-18 MC",
      )
      selectOnlyTask()
      doTasks("PROD[Plant]", "PROD[3 MC]", "PROD[Energy]", "PROD[Heat]")
    }
    advanceUntilActionPhase()
  }

  private fun useUtopiaToLowerProduction(resource: String) {
    kim.inTurn {
      doTasks(
          "UseCardAction<UtopiaInvest, Action1>",
          "PROD[-$resource] THEN 4 $resource",
          "4 $resource",
      )
    }
  }

  private fun pendingGainTask(type: String, amount: Int = 1): Task? {
    val target = kim.reader.resolve((kim.parse<Instruction>(type) as Gain).gaining)
    return kim.tasks
        .extract { it }
        .firstOrNull {
          val gain = it.instruction as? Gain
          gain != null &&
              gain.count == ActualScalar(amount) &&
              kim.reader.resolve(gain.gaining) == target
        }
  }

  private fun pendingGain(type: String, amount: Int = 1): Boolean =
      pendingGainTask(type, amount) != null

  /** Whether SAFE may claim the pending gain now; the gain must be pending. */
  private fun claimableNow(type: String, amount: Int = 1): Boolean =
      TfmAutoExec.safeTask(game, checkNotNull(pendingGainTask(type, amount)) { "no pending $type" })

  // No Industrial Complex-free Canon same-action harmful-early witness has been established for
  // concrete production gains of Steel, Titanium, Energy, or Heat. Aridor with WG Project and
  // Ecology Experts provide the nested-play routes above for MC and Plant production, respectively.
  // Nor has one been established for CardResource types Animal, Microbe, Science, Asteroid,
  // Floater, Disease, Camp, Director, Fighter, Graphene, Hydroelectric, Preservation, or RobotUnit.
  // L1 Trade Terminal currently counts an unrelated pending Animal gain toward its gift quota;
  // that documented model shortcut cannot establish a Canon Animal counterexample.
  // Pharmacy Union's Disease is inside an OR task until chosen; selecting it locks out competing
  // ordinary work until the choice finishes. It is not a pending concrete Disease gain.
  // Preservation Program cancels only one step of a multi-TR gain; swapping that step with its
  // reaction to Nitrogen-Rich Asteroid does not make an early TerraformRating gain harmful.
  // No verified witness yet for TerraformRating, VictoryPoint, Victory, ProjectCard,
  // CorporationCard, PreludeCard, PlayedEvent, StagedProject, GreeneryTile, CityTile,
  // SpecialTile, Community, NomadsMarker, Colony, TradeFleet, Trade, PartyDelegate, Delegate,
  // LobbyDelegate, Influence, Tag, WildTag, WildTagElection, Milestone, FirstPlace,
  // SecondPlace, HydrologistCredit, ThawerCredit, FrontierTownBonus, NextCardEffect,
  // ResourceValue, Cathedral, CathedralOffer, ActionPhaseTrGain, L1Gift, ActionUsedMarker,
  // RoundStatus, or Pass.
  // Sponsored Academies can distribute physical ProjectCard draws in different orders, but this
  // World tracks anonymous cards; no fixed-deck optimal-play test has been established here.
  // Tile placement is usually abstract until its area is chosen; selection locks the task.
  // No separate harmful-early concrete-gain witness was established for these other explicitly
  // Owned Canon declarations. Some, including PlayCard, DoRequiredActions, and CopyProductionBox,
  // are claimable Player tasks; the absence of a witness does not make them safe to claim eagerly.
  // Base: Accepting, AcceptingFromCard, AllocateRequiredWildTags, Baton, Billing,
  // BuySelectedCards, Card, Cardbound, CheckRequirement, ClaimCardReward, CorporationOption,
  // DoRequiredActions, OfferAction, Owed, OwnedOccupant, PassBaton, Pay, PayFromCard,
  // PayingFor, PlaceGreeneryTile, PlaceNeutralTiles, PlaceOceanForAdmin, Placement, PlayCard,
  // PlayPrelude, PriceCard, ProdOffset, Production, Required, RequiredAction, Resource,
  // ResourceHolder, ScoreEventVps, SearchForCard, SecondAction, SoloCardResourceReserve,
  // SoloStandardResourceReserve, StandardAction, StartToken, TagHolder, TakeSelectedCard,
  // UseAction, WorldGovernmentTerraforming.
  // Turmoil: GlobalEffect, MeasureInfluence, ScientistsUsedMarker, TrWasRaised.
  // Colonies: FinishTrade, GainColonyBonus, GainColonyBonuses, PlutoLock, SoloColoniesSetup.
  // Promos: ChooseOceanArea, CopyPrelude, CopyProductionBox, ProductionWasAttacked,
  // ReplicateForStagedProject, ResourceWasAttacked, StageForReplicatedProject, TakeEventCard.
  // Prelude 2: AllocateCloudTourismWildTags, L1GiftWatcher, RepeatPlacementBonus.
  // Vastitas: TileInLargestGroup. Venus: AddResourceToVenusCard.
  // The named explicit roots and all inherited subclasses, including generated card faces and
  // action subclasses, are covered by the missing-witness comments above. They are not safe-list
  // entries merely because no counterexample has been found.

  private companion object {
    private const val COMPLEX_PRODUCTION_BOX =
        "EACH Class<@StandardResource> { PROD[@StandardResource / (Class<@StandardResource> OR QuickStartVariant OR ProdOffset<Class<@StandardResource>>) - Production<Class<@StandardResource>>] }"
  }
}
