package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class PreservationProgramTest : TfmGameplayTest() {
  @Test
  fun `Prelude benefit leaves the Action phase skip available`() {
    newProgramGame()
    kim.playPrelude(PreservationProgram).expect("5 TerraformRating")
    kim.playPrelude(Donation)
    playStansPreludes()

    kim.stdProject("AsteroidProject").expect("TemperatureStep, 0 TerraformRating")
    kim.stdProject("AsteroidProject").expect("TemperatureStep, TerraformRating")
  }

  @Test
  fun `Earlier Prelude TR leaves the Action phase skip available`() {
    newProgramGame()
    kim.playPrelude(UnmiContractor)
    kim.playPrelude(PreservationProgram).expect("5 TerraformRating")
    playStansPreludes()

    kim.stdProject("AsteroidProject").expect("TemperatureStep, 0 TerraformRating")
  }

  @Test
  fun `Prevented TR gain renews each generation rather than each turn`() {
    newProgramGame()
    playPreludes()
    kim.turn { stdProject("AsteroidProject").expect("0 TerraformRating") }
    stan.turn { sellPatents(1) }
    kim.turn { stdProject("AsteroidProject").expect("TerraformRating") }
    stan.pass()
    passGeneration()
    stan.pass()

    kim.stdProject("AsteroidProject").expect("0 TerraformRating")
    kim.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `Earlier Prelude TR does not waive the skip when Valley Trust plays the Program`() {
    newProgramGame(ValleyTrust)
    playPreludes(UnmiContractor, Donation)

    kim.stdAction("DoRequiredActionsAction") { kim.playPrelude(PreservationProgram) }
        .expect("4 TerraformRating")
  }

  @Test
  fun `Playing the Program as a first action skips part of its own benefit`() {
    newProgramGame(ValleyTrust)
    playPreludes(PowerGeneration, Donation)

    kim.stdAction("DoRequiredActionsAction") { kim.playPrelude(PreservationProgram) }
        .expect("4 TerraformRating")
  }

  @Test
  fun `Earlier TR in the same turn prevents a skip when Board of Directors plays the Program`() {
    newProgramGame(extraOptions = "BoardOfDirectors")
    playPreludes(BoardOfDirectors, Donation)

    kim.turn {
      stdProject("AsteroidProject")
      cardAction1(BoardOfDirectors) {
            doTask("-12 MC")
            kim.playPrelude(PreservationProgram)
          }
          .expect("5 TerraformRating")
    }
  }

  @Test
  fun `Earlier non-TR action leaves the skip available when the Program is played second`() {
    newProgramGame(extraOptions = "BoardOfDirectors")
    playPreludes(BoardOfDirectors, Donation)

    kim.turn {
      stdProject("PowerPlantProject")
      cardAction1(BoardOfDirectors) {
            doTask("-12 MC")
            kim.playPrelude(PreservationProgram)
          }
          .expect("4 TerraformRating")
    }
  }

  @Test
  fun `UNMI cannot use a prevented gain to satisfy its requirement`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    passGeneration()
    stan.pass()
    kim.stdProject("AsteroidProject")

    shouldThrow<RequirementException> { kim.cardAction1(UnitedNationsMarsInitiative) }
    kim.count("TerraformRating") shouldBe 25
  }

  @Test
  fun `UNMI can act after an actual gain following a prevented gain`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    passGeneration()
    stan.pass()
    kim.stdProject("AsteroidProject")
    kim.stdProject("AsteroidProject")

    kim.cardAction1(UnitedNationsMarsInitiative).expect("TerraformRating")
  }

  @Test
  fun `Prelude benefit qualifies UNMI before the first Action phase gain`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()

    kim.cardAction1(UnitedNationsMarsInitiative).expect("0 TerraformRating")
  }

  @Test
  fun `Prelude benefit still qualifies UNMI after the first Action phase gain is prevented`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    kim.stdProject("AsteroidProject")

    kim.cardAction1(UnitedNationsMarsInitiative).expect("TerraformRating")
  }

  @Test
  fun `Prelude benefit still disqualifies Pristar after a prevented Action phase gain`() {
    newProgramGame(Pristar)
    playPreludes()
    kim.turn { stdProject("AsteroidProject") }
    stan.pass()

    kim.pass()
    admin.count("VenusSolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 0
  }

  @Test
  fun `Pristar receives its production bonus after only a prevented gain`() {
    newProgramGame(Pristar)
    playPreludes()
    passGeneration()
    stan.pass()
    kim.stdProject("AsteroidProject")
    val income = kim.count("TerraformRating") + kim.production(cn("MC")) + 6

    val money = kim.count("MC")
    kim.pass()

    kim.count("Preservation<$Pristar>") shouldBe 1
    kim.count("MC") shouldBe money + income
  }

  @Test
  fun `Pristar loses its production bonus after an actual gain`() {
    newProgramGame(Pristar)
    playPreludes()
    passGeneration()
    stan.pass()
    kim.stdProject("AsteroidProject")
    kim.stdProject("AsteroidProject")

    kim.pass()
    admin.count("VenusSolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 0
  }

  @Test
  fun `A fixed three-TR gain loses only a single step`() {
    newProgramGame()
    playPreludes(PreservationProgram, PowerGeneration)

    kim.playProject(MagneticFieldGeneratorsPromo, 22) { placeTile(2, 4) }
        .expect("2 TerraformRating")
  }

  @Test
  fun `Terraforming Ganymede skips a single computed TR step`() {
    newProgramGame(SaturnSystems)
    playPreludes(PreservationProgram, GalileanMining)
    passGeneration()
    stan.pass()

    kim.playProject(TerraformingGanymede, 33).expect("2 TerraformRating")
  }

  @Test
  fun `Terraforming Ganymede awards all computed TR after the skip is used`() {
    newProgramGame(SaturnSystems)
    playPreludes(PreservationProgram, GalileanMining)
    passGeneration()
    stan.pass()
    kim.stdProject("AsteroidProject")

    kim.playProject(TerraformingGanymede, 21, titanium = 4).expect("3 TerraformRating")
  }

  @Test
  fun `Opponents gain TR without consuming the owners skip`() {
    newProgramGame()
    playPreludes()
    passGeneration()
    stan.turn { stdProject("AsteroidProject").expect("TerraformRating") }
    kim.turn { stdProject("AsteroidProject").expect("0 TerraformRating") }
    stan.turn { stdProject("AsteroidProject").expect("TerraformRating") }
    kim.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `Double Down copies the Prelude benefit without adding another skip`() {
    newProgramGame()
    kim.playPrelude(PreservationProgram)
    kim.playPrelude(DoubleDown) { doTask("CopyPrelude<PreservationProgram>") }
        .expect("5 TerraformRating")
    playStansPreludes()

    kim.playProject(Comet, 21) { placeTile(1, 2) }.expect("TerraformRating")
  }

  @Test
  fun `Double Down copies four TR when the Action phase skip is unused`() {
    newProgramGame(ValleyTrust)
    playPreludes()

    kim.stdAction("DoRequiredActionsAction") {
          kim.playPrelude(DoubleDown) { doTask("CopyPrelude<PreservationProgram>") }
        }
        .expect("4 TerraformRating")
  }

  @Test
  fun `Double Down copies five TR after the Action phase skip is used`() {
    newProgramGame(extraOptions = "BoardOfDirectors")
    playPreludes(PreservationProgram, BoardOfDirectors)
    kim.stdProject("AsteroidProject")

    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          kim.playPrelude(DoubleDown) { doTask("CopyPrelude<PreservationProgram>") }
        }
        .expect("5 TerraformRating")
  }

  @Test
  fun `Pharmacy Union consumes a disease even when its TR gain is prevented`() {
    startPharmacyGame()
    playPreludes()

    kim.playProject(PhysicsComplex, 12).expect("-Disease<$PharmacyUnion>, 0 TerraformRating")
  }

  @Test
  fun `World Government Advisor does not consume the skip`() {
    newProgramGame(extraOptions = "WorldGovernmentAdvisor")
    playPreludes(PreservationProgram, WorldGovernmentAdvisor)
    passGeneration()
    stan.pass()
    kim.cardAction1(WorldGovernmentAdvisor) { doTask("TemperatureStep BY Admin") }
        .expect("TemperatureStep, 0 TerraformRating")

    kim.stdProject("AsteroidProject").expect("0 TerraformRating")
  }

  @Test
  fun `Solar chairman TR does not consume the following generations skip`() {
    newProgramGame(extraOptions = "TurmoilExpansion, Unsafe")
    playPreludes()
    kim.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") }
      stdAction("LobbyAction", 2, payment = { pay(5) }) { doTask("PartyDelegate<Scientists>") }
    }
    stan.pass()
    val rating = kim.count("TerraformRating")
    passGeneration(nextEvent = "ScientificCommunity")
    kim.count("Chairman") shouldBe 1
    // Turmoil's TR revision loses a step, then becoming chairman restores it.
    kim.count("TerraformRating") shouldBe rating
    stan.pass()

    kim.stdProject("AsteroidProject").expect("0 TerraformRating")
  }

  @Test
  fun `A gain followed by a loss still prevents a skip when the Program is acquired`() {
    newProgramGame(extraOptions = "BoardOfDirectors, Pristar")
    playPreludes(BoardOfDirectors, Donation)
    kim.turn { stdProject("AsteroidProject") }
    stan.pass()
    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          kim.playPrelude(Merger) { kim.playCorp(Pristar) }
        }
        .expect("-2 TerraformRating")

    with(kim) {
      playProject(ProjectInspection, 0) {
            cardAction1(BoardOfDirectors) {
              doTask("-12 MC")
              kim.playPrelude(PreservationProgram)
            }
          }
          .expect("5 TerraformRating")
    }
  }

  @Test
  fun `A TR loss does not restore a consumed skip`() {
    newProgramGame(extraOptions = "BoardOfDirectors, Pristar")
    playPreludes(PreservationProgram, BoardOfDirectors)
    passGeneration()
    stan.pass()
    kim.stdProject("AsteroidProject")
    kim.stdProject("AsteroidProject")
    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          kim.playPrelude(Merger) { kim.playCorp(Pristar) }
        }
        .expect("-2 TerraformRating")

    kim.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `A capped global parameter does not consume the skip`() {
    newProgramGame()
    playPreludes()
    repeat(14) { passGeneration("OxygenStep") }
    kim.turn { convertPlants { placeTile(2, 4) }.expect("0 OxygenStep, 0 TerraformRating") }
    stan.pass()

    kim.stdProject("AsteroidProject").expect("0 TerraformRating")
  }

  @Test
  fun `Linked oxygen temperature and ocean gains skip a single TR`() {
    newProgramGame()
    playPreludes()
    repeat(7) { passGeneration("OxygenStep") }
    repeat(14) { passGeneration("TemperatureStep") }
    stan.pass()

    kim.convertPlants {
          placeTile(2, 4)
          placeTile(1, 2)
        }
        .expect("OxygenStep, TemperatureStep, OceanTile, 2 TerraformRating")
  }

  @Test
  fun `Pharmacy Union flip loses part of its TR and remains a played event`() {
    startPharmacyGame()
    playPreludes()
    kim.turn { playProject(Research, 11) }
    stan.pass()
    passGeneration()
    stan.pass()

    kim.playProject(PhysicsComplex, 12) { doTask("PlayedEvent FROM $PharmacyUnion") }
        .expect("2 TerraformRating")
    kim.count("$PharmacyUnion") shouldBe 0
    kim.count("PlayedEvent<Class<$PharmacyUnion>>") shouldBe 1
  }

  @Test
  fun `Earlier TR in a prior turn prevents a skip when the Program is acquired`() {
    newProgramGame(extraOptions = "BoardOfDirectors")
    playPreludes(BoardOfDirectors, Donation)
    kim.turn { stdProject("AsteroidProject") }
    stan.pass()

    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          kim.playPrelude(PreservationProgram)
        }
        .expect("5 TerraformRating")
  }

  @Test
  fun `Reds ruling bonus outside the Action phase leaves the next generations skip available`() {
    newProgramGame(extraOptions = "TurmoilExpansion, Unsafe")
    kim.playPrelude(PreservationProgram)
    kim.playPrelude(PowerGeneration)
    stan.playPrelude(UnmiContractor)
    stan.playPrelude(Donation)
    kim.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Reds>") }
      stdAction("LobbyAction", 2, payment = { pay(5) }) { doTask("PartyDelegate<Reds>") }
    }
    stan.turn { repeat(2) { stdProject("AsteroidProject") } }
    kim.pass()
    stan.stdProject("AsteroidProject")
    // Kim has the lower rating; Reds and the chairmanship each grant TR after revision.
    val rating = kim.count("TerraformRating")
    stan.pass()
    kim.wgt("VenusStep")
    kim.count("TerraformRating") shouldBe rating + 1
    admin.doTask("ScientificCommunity")
    players.forEach { it.buyCards(0) }
    stan.pass()

    kim.stdProject("AsteroidProject").expect("0 TerraformRating")
  }

  @Test
  fun `Venus threshold awards its additional TR while skipping a single step`() {
    newProgramGame()
    playPreludes()
    repeat(7) { passGeneration("VenusStep") }
    stan.pass()

    kim.stdProject("AirScrappingProject").expect("VenusStep, TerraformRating")
  }

  @Test
  fun `Solo Buffer Gas uses the skip while starting TR remains fourteen`() {
    newTestGame(
        addOptions = "PreludeExpansion, PreservationProgram, Tr63SoloObjective",
        playerCount = 1,
    )
    kim.count("TerraformRating") shouldBe 14
    kim.playPrelude(PreservationProgram).expect("5 TerraformRating")
    kim.playPrelude(Donation)

    kim.stdProject("BufferGasProject").expect("0 TerraformRating")
    kim.stdProject("BufferGasProject").expect("TerraformRating")
  }

  private fun startPharmacyGame() {
    newTestGame(
        addOptions = "PreludeExpansion, PreservationProgram",
        playerCount = 2,
        kimCorporation = PharmacyUnion,
        startAtCorporation = true,
    )
    kim.playCorp(PharmacyUnion) { repeat(2) { doTask("Disease<$PharmacyUnion>!") } }
    stan.inTurn {
      doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation2>, Hand>")
      stan.pay()
    }
  }

  private fun newProgramGame(kimCorporation: ClassName? = null, extraOptions: String = "") {
    newTestGame(
        addOptions = "PreludeExpansion, PreservationProgram, $extraOptions",
        playerCount = 2,
        kimCorporation = kimCorporation,
    )
  }

  private fun playPreludes(first: ClassName = PreservationProgram, second: ClassName = Donation) {
    kim.playPrelude(first)
    kim.playPrelude(second)
    playStansPreludes()
  }

  private fun playStansPreludes() {
    stan.playPrelude(Supplier)
    stan.playPrelude(MetalsCompany)
  }

  /** Called at the start of a generation, or after Stan has passed and Kim finishes play. */
  private fun passGeneration(worldGovernment: String = "VenusStep", nextEvent: String? = null) {
    val first = players.indexOfFirst { it.count("StartToken") == 1 }
    val order = players.drop(first) + players.take(first)
    order.filter { it.count("Pass") == 0 }.forEach { it.pass() }
    order.first().wgt(worldGovernment)
    if (nextEvent != null) admin.doTask(nextEvent)
    players.forEach { it.buyCards(0) }
  }

  internal class DefectCharacterization : TfmSandboxTest() {
    private fun setUpProgramGame(extraOptions: String = "") {
      newTestGame(
          addOptions =
              "PreludeExpansion, Prelude2CardPack, TurmoilExpansion, Unsafe, $extraOptions",
          playerCount = 2,
          startAtCorporation = true,
      )
    }

    private fun startLaterGeneration(corporation: String = "$PhoboLog") {
      setUpProgramGame()
      admin.phase("Prelude")
      kim.runOperation("$corporation, PreservationProgram, 100 MC")
      admin.phase("Action")
      admin.nextGeneration(0, 0)
    }

    // These pairs explicitly select the unsupported combinations with Unsafe.
    // Preservation Program first-TR timing:
    // https://boardgamegeek.com/thread/3353355/article/44740462#44740462
    // Terraforming Deal remains active:
    // https://boardgamegeek.com/thread/3343166/article/44660687#44660687
    // Reds payment:
    // https://boardgamegeek.com/thread/2196388/article/31885882#31885882
    @Ignore // The removed TR still triggers Terraforming Deal.
    @Test
    internal fun `Terraforming Deal pays only for the retained TR in a fixed gain`() {
      gainThreeRatingWithDeal().expect("2 TerraformRating, -18 MC")
    }

    @Test
    internal fun `BUG - Terraforming Deal pays for the reversed TR in a fixed gain`() {
      gainThreeRatingWithDeal().expect("2 TerraformRating, -16 MC")
    }

    private fun gainThreeRatingWithDeal(): TaskResult {
      startLaterGeneration()
      kim.runOperation("$TerraformingDeal, ProjectCard, PROD[4 Energy]")
      return kim.playProject(MagneticFieldGeneratorsPromo, 22) { placeTile(2, 4) }
    }

    @Ignore // Reds charges before Preservation Program removes the TR.
    @Test
    internal fun `Reds charges nothing for the prevented TR`() {
      raiseTemperatureUnderReds().expect("0 TerraformRating, -14 MC")
    }

    @Test
    internal fun `BUG - Reds charges for the prevented TR`() {
      raiseTemperatureUnderReds().expect("0 TerraformRating, -17 MC")
    }

    private fun startProgramUnderReds(deal: Boolean = false) {
      startLaterGeneration()
      if (deal) kim.runOperation("$TerraformingDeal")
      admin.runOperation("Ruling<Reds> FROM Ruling")
      admin.phase("Production")
      admin.phase("Action")
    }

    private fun raiseTemperatureUnderReds(): TaskResult {
      startProgramUnderReds()
      return kim.stdProject("AsteroidProject")
    }

    @Ignore // Both payments respond before Preservation Program removes the TR.
    @Test
    internal fun `Reds and Terraforming Deal both count only retained TR`() {
      gainThreeRatingUnderRedsWithDeal().expect("2 TerraformRating, -24 MC")
    }

    @Test
    internal fun `BUG - Reds and Terraforming Deal both count reversed TR`() {
      gainThreeRatingUnderRedsWithDeal().expect("2 TerraformRating, -25 MC")
    }

    private fun gainThreeRatingUnderRedsWithDeal(): TaskResult {
      startProgramUnderReds(deal = true)
      kim.runOperation("ProjectCard, PROD[4 Energy]")
      return kim.playProject(MagneticFieldGeneratorsPromo, 22) { placeTile(2, 4) }
    }

    @Ignore // The retry still charges for the prevented gain.
    @Test
    internal fun `Reds charges nothing for prevented TR after an unaffordable attempt`() {
      convertHeatAfterFailedRatingGain().expect("0 TerraformRating, 0 MC")
    }

    @Test
    internal fun `BUG - Reds charges for prevented TR after an unaffordable attempt`() {
      convertHeatAfterFailedRatingGain().expect("0 TerraformRating, -3 MC")
    }

    private fun convertHeatAfterFailedRatingGain(): TaskResult {
      startProgramUnderReds()
      kim.runOperation("-MC / MC")
      kim.runOperation("-Heat / Heat")
      kim.runOperation("14 MC, ProjectCard, 8 Heat")
      // Even with a TR prevented, the remaining TR would cost 3 MC after the card's 14 MC price.
      shouldThrow<LimitsException> { kim.playProject(ReleaseOfInertGases, 14) }
      kim.assertCounts(14 to "MC", 1 to "ProjectCard", 8 to "Heat")
      kim.runOperation("-11 MC")
      return kim.convertHeat()
    }

    @Ignore // Reds requires payment for both TR before the first is removed.
    @Test
    internal fun `Reds permits an affordable two-step gain when the first TR is prevented`() {
      playAffordableTwoStepGain().expect("TerraformRating, -17 MC")
    }

    @Test
    internal fun `BUG - Reds rejects an affordable two-step gain when the first TR is prevented`() {
      shouldThrow<LimitsException> { playAffordableTwoStepGain() }
      kim.assertCounts(
          17 to "MC",
          1 to "ProjectCard",
          0 to "PlayedEvent<Class<$ReleaseOfInertGases>>",
      )
    }

    private fun playAffordableTwoStepGain(): TaskResult {
      startProgramUnderReds()
      kim.runOperation("-MC / MC")
      kim.runOperation("17 MC, ProjectCard")
      return kim.playProject(ReleaseOfInertGases, 14)
    }

    @Ignore // The earlier Solar-phase gain does not prevent the incorrect payout.
    @Test
    internal fun `Terraforming Deal ignores prevented TR after a chairman award`() {
      raiseTemperatureAfterChairmanAward().expect("0 TerraformRating, -14 MC")
    }

    @Test
    internal fun `BUG - Terraforming Deal pays for prevented TR after a chairman award`() {
      raiseTemperatureAfterChairmanAward().expect("0 TerraformRating, -12 MC")
    }

    // Terraforming Deal outside Action:
    // https://boardgamegeek.com/thread/3343166/article/44660753#44660753
    private fun raiseTemperatureAfterChairmanAward(): TaskResult {
      startLaterGeneration()
      kim.runOperation("$TerraformingDeal, 2 PartyDelegate<Scientists>")
      admin.phase("Solar")
      admin.runOperation("FormGovernment").expect("TerraformRating<Kim>, 2 MC<Kim>")
      admin.phase("Research") {
        kim.buyCards(0)
        stan.buyCards(0)
      }
      admin.phase("Action")
      return kim.stdProject("AsteroidProject")
    }

    @Ignore // The earlier Solar-phase gain does not prevent the incorrect payout.
    @Test
    internal fun `Terraforming Deal ignores prevented TR after the Reds ruling bonus`() {
      raiseTemperatureAfterRedsBonus().expect("0 TerraformRating, -14 MC")
    }

    @Test
    internal fun `BUG - Terraforming Deal pays for prevented TR after the Reds ruling bonus`() {
      raiseTemperatureAfterRedsBonus().expect("0 TerraformRating, -12 MC")
    }

    private fun raiseTemperatureAfterRedsBonus(): TaskResult {
      startLaterGeneration()
      kim.runOperation("$TerraformingDeal, -10 TerraformRating")
      admin.phase("Solar")
      admin.runOperation("ApplyRulingBonus<Reds>").expect("TerraformRating<Kim>, 2 MC<Kim>")
      admin.phase("Action")
      return kim.stdProject("AsteroidProject")
    }

    @Ignore // Both payments include the TR reversed during Preservation Program's own play.
    @Test
    internal fun `Reds and Terraforming Deal count only retained TR when Valley Trust plays it`() {
      playProgramThroughValleyTrustWithBothPayments().expect("4 TerraformRating, -4 MC")
    }

    @Test
    internal fun `BUG - Reds and Terraforming Deal count reversed TR when Valley Trust plays it`() {
      playProgramThroughValleyTrustWithBothPayments().expect("4 TerraformRating, -5 MC")
    }

    private fun playProgramThroughValleyTrustWithBothPayments(): TaskResult {
      setUpProgramGame()
      kim.runOperation("$ValleyTrust")
      admin.phase("Prelude")
      kim.playPrelude(TerraformingDeal)
      kim.playPrelude(Donation)
      admin.runOperation("Ruling<Reds> FROM Ruling")
      admin.phase("Action")
      return kim.stdAction("DoRequiredActionsAction") { kim.playPrelude(PreservationProgram) }
    }

    @Ignore // The payout includes the reversed TR.
    @Test
    internal fun `Terraforming Deal counts only retained TR when Pharmacy Union flips`() {
      flipPharmacyWithDeal().expect("2 TerraformRating, -8 MC")
    }

    @Test
    internal fun `BUG - Terraforming Deal counts reversed TR when Pharmacy Union flips`() {
      flipPharmacyWithDeal().expect("2 TerraformRating, -6 MC")
    }

    private fun flipPharmacyWithDeal(): TaskResult {
      startLaterGeneration("$PharmacyUnion")
      kim.runOperation("$TerraformingDeal, ProjectCard, -2 Disease<$PharmacyUnion>")
      return kim.playProject(PhysicsComplex, 12) {
        doTask("PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion")
      }
    }

    @Ignore // The payout includes the reversed TR.
    @Test
    internal fun `Terraforming Deal counts only retained TR across greenery threshold gains`() {
      crossGreeneryThresholdsWithDeal()
          .expect("OxygenStep, TemperatureStep, OceanTile, 2 TerraformRating, 8 MC")
    }

    @Test
    internal fun `BUG - Terraforming Deal counts reversed TR across greenery threshold gains`() {
      crossGreeneryThresholdsWithDeal()
          .expect("OxygenStep, TemperatureStep, OceanTile, 2 TerraformRating, 10 MC")
    }

    private fun crossGreeneryThresholdsWithDeal(): TaskResult {
      startLaterGeneration()
      kim.runOperation("$TerraformingDeal, 8 Plant")
      admin.runOperation("7 OxygenStep, 14 TemperatureStep")
      return kim.convertPlants {
        doTask("GreeneryTile<Tharsis_2_4>")
        placeTile(1, 2)
      }
    }

    @Ignore // The payout includes the reversed TR.
    @Test
    internal fun `Terraforming Deal counts only retained TR at the Venus threshold`() {
      crossVenusThresholdWithDeal().expect("VenusStep, TerraformRating, -13 MC")
    }

    @Test
    internal fun `BUG - Terraforming Deal counts reversed TR at the Venus threshold`() {
      crossVenusThresholdWithDeal().expect("VenusStep, TerraformRating, -11 MC")
    }

    private fun crossVenusThresholdWithDeal(): TaskResult {
      setUpProgramGame(extraOptions = "VenusNextExpansion")
      admin.phase("Prelude")
      kim.runOperation("$PhoboLog, PreservationProgram, $TerraformingDeal, 100 MC")
      admin.phase("Action")
      admin.nextGeneration(0, 0)
      admin.runOperation("7 VenusStep")
      return kim.stdProject("AirScrappingProject")
    }
  }
}
