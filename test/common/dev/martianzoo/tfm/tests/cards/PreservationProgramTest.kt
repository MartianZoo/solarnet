package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PreservationProgramTest : TfmGameplayTest() {
  @Test
  fun `Only the skipped first gain and real later gains can commit`() {
    newProgramGame()
    playPreludes()
    val initialRating = kim.count("TerraformRating")
    val initialMoney = kim.count("MC")
    val initialTemperature = admin.count("TemperatureStep")

    shouldThrow<DeadEndException> {
      kim.stdProject("AsteroidProject") { doTask("TerraformRating") }
    }
    kim.count("TerraformRating") shouldBe initialRating
    kim.count("MC") shouldBe initialMoney
    admin.count("TemperatureStep") shouldBe initialTemperature

    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
        .expect("TemperatureStep, 0 TerraformRating")
    val moneyAfterSkip = kim.count("MC")
    val temperatureAfterSkip = admin.count("TemperatureStep")
    shouldThrow<DeadEndException> {
      kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
    }
    kim.count("TerraformRating") shouldBe initialRating
    kim.count("MC") shouldBe moneyAfterSkip
    admin.count("TemperatureStep") shouldBe temperatureAfterSkip
    kim.stdProject("AsteroidProject") { doTask("TerraformRating") }
        .expect("TemperatureStep, TerraformRating")
    kim.count("TerraformRating") shouldBe initialRating + 1
  }

  @Test
  fun `Prelude benefit leaves the Action phase skip available`() {
    newProgramGame()
    kim.playPrelude(PreservationProgram).expect("5 TerraformRating")
    kim.playPrelude(Donation)
    playStansPreludes()

    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
        .expect("TemperatureStep, 0 TerraformRating")
    kim.stdProject("AsteroidProject") { doTask("TerraformRating") }
        .expect("TemperatureStep, TerraformRating")
  }

  @Test
  fun `Earlier Prelude TR leaves the Action phase skip available`() {
    newProgramGame()
    kim.playPrelude(UnmiContractor)
    kim.playPrelude(PreservationProgram).expect("5 TerraformRating")
    playStansPreludes()

    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
        .expect("TemperatureStep, 0 TerraformRating")
  }

  @Test
  fun `Prevented TR gain renews each generation rather than each turn`() {
    newProgramGame()
    playPreludes()
    kim.turn {
      stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }.expect("0 TerraformRating")
    }
    kim.count("SkippedTerraformRating") shouldBe 1
    stan.turn { sellPatents(1) }
    kim.turn {
      stdProject("AsteroidProject") { doTask("TerraformRating") }.expect("TerraformRating")
    }
    stan.pass()
    passGeneration()
    kim.count("SkippedTerraformRating") shouldBe 0
    stan.pass()

    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
        .expect("0 TerraformRating")
    kim.stdProject("AsteroidProject") { doTask("TerraformRating") }.expect("TerraformRating")
  }

  @Test
  fun `Earlier Prelude TR does not waive the skip when Valley Trust plays the Program`() {
    newProgramGame(ValleyTrust)
    playPreludes(UnmiContractor, Donation)

    kim.stdAction("RequiredActionsSignal") {
          kim.playPrelude(PreservationProgram) {
            doTask("SkippedTerraformRating")
            repeat(4) { doTask("TerraformRating") }
          }
        }
        .expect("4 TerraformRating")
  }

  @Test
  fun `Playing the Program as a first action skips part of its own benefit`() {
    newProgramGame(ValleyTrust)
    playPreludes(PowerGeneration, Donation)

    kim.stdAction("RequiredActionsSignal") {
          kim.playPrelude(PreservationProgram) {
            doTask("SkippedTerraformRating")
            repeat(4) { doTask("TerraformRating") }
          }
        }
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
            kim.playPrelude(PreservationProgram) {
              repeat(5) { doTask("TerraformRating") }
            }
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
            kim.playPrelude(PreservationProgram) {
              doTask("SkippedTerraformRating")
              repeat(4) { doTask("TerraformRating") }
            }
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
    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }

    shouldThrow<RequirementException> { kim.cardAction1(UnitedNationsMarsInitiative) }
    kim.count("TerraformRating") shouldBe 25
  }

  @Test
  fun `UNMI can act after an actual gain following a prevented gain`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    passGeneration()
    stan.pass()
    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
    kim.stdProject("AsteroidProject") { doTask("TerraformRating") }

    kim.cardAction1(UnitedNationsMarsInitiative) { doTask("TerraformRating") }
        .expect("TerraformRating")
  }

  @Test
  fun `Prelude benefit qualifies UNMI before the first Action phase gain`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()

    kim.cardAction1(UnitedNationsMarsInitiative) { doTask("SkippedTerraformRating") }
        .expect("0 TerraformRating")
  }

  @Test
  fun `Prelude benefit still qualifies UNMI after the first Action phase gain is prevented`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }

    kim.cardAction1(UnitedNationsMarsInitiative) { doTask("TerraformRating") }
        .expect("TerraformRating")
  }

  @Test
  fun `Prelude benefit still disqualifies Pristar after a prevented Action phase gain`() {
    newProgramGame(Pristar, extraOptions = "Pristar")
    playPreludes()
    kim.turn { stdProject("AsteroidProject") { doTask("SkippedTerraformRating") } }
    stan.pass()

    kim.pass()
    admin.count("VenusSolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 0
  }

  @Test
  fun `Pristar receives its production bonus after only a prevented gain`() {
    newProgramGame(Pristar, extraOptions = "Pristar")
    playPreludes()
    passGeneration()
    stan.pass()
    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
    val income = kim.count("TerraformRating") + kim.production(cn("MC")) + 6

    val money = kim.count("MC")
    kim.pass()

    kim.count("Preservation<$Pristar>") shouldBe 1
    kim.count("MC") shouldBe money + income
  }

  @Test
  fun `Pristar loses its production bonus after an actual gain`() {
    newProgramGame(Pristar, extraOptions = "Pristar")
    playPreludes()
    passGeneration()
    stan.pass()
    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
    kim.stdProject("AsteroidProject") { doTask("TerraformRating") }

    kim.pass()
    admin.count("VenusSolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 0
  }

  @Test
  fun `A fixed three-TR gain loses only a single step`() {
    newProgramGame()
    playPreludes(PreservationProgram, PowerGeneration)

    kim.playProject(MagneticFieldGeneratorsPromo, 22) {
          placeTile(2, 4)
          doTask("SkippedTerraformRating")
          repeat(2) { doTask("TerraformRating") }
        }
        .expect("2 TerraformRating")
  }

  @Test
  fun `Terraforming Ganymede skips a single computed TR step`() {
    newProgramGame(SaturnSystems)
    playPreludes(PreservationProgram, GalileanMining)
    passGeneration()
    stan.pass()

    kim.playProject(TerraformingGanymede, 33) {
          doTask("SkippedTerraformRating")
          repeat(2) { doTask("TerraformRating") }
        }
        .expect("2 TerraformRating")
  }

  @Test
  fun `Terraforming Ganymede awards all computed TR after the skip is used`() {
    newProgramGame(SaturnSystems)
    playPreludes(PreservationProgram, GalileanMining)
    passGeneration()
    stan.pass()
    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }

    kim.playProject(TerraformingGanymede, 21, titanium = 4) {
          repeat(3) { doTask("TerraformRating") }
        }
        .expect("3 TerraformRating")
  }

  @Test
  fun `Opponents gain TR without consuming the owners skip`() {
    newProgramGame()
    playPreludes()
    passGeneration()
    stan.turn { stdProject("AsteroidProject").expect("TerraformRating") }
    kim.turn {
      stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }.expect("0 TerraformRating")
    }
    stan.turn { stdProject("AsteroidProject").expect("TerraformRating") }
    kim.stdProject("AsteroidProject") { doTask("TerraformRating") }.expect("TerraformRating")
  }

  @Test
  fun `Double Down copies the Prelude benefit without adding another skip`() {
    newProgramGame()
    kim.playPrelude(PreservationProgram)
    kim.playPrelude(DoubleDown) { doTask("CopyPrelude<PreservationProgram>") }
        .expect("5 TerraformRating")
    playStansPreludes()

    kim.playProject(Comet, 21) {
          placeTile(1, 2)
          doTask("SkippedTerraformRating")
          doTask("TerraformRating")
        }
        .expect("TerraformRating")
  }

  @Test
  fun `Double Down copies four TR when the Action phase skip is unused`() {
    newProgramGame(ValleyTrust)
    playPreludes()

    kim.stdAction("RequiredActionsSignal") {
          kim.playPrelude(DoubleDown) {
            doTask("CopyPrelude<PreservationProgram>")
            doTask("SkippedTerraformRating")
            repeat(4) { doTask("TerraformRating") }
          }
        }
        .expect("4 TerraformRating")
  }

  @Test
  fun `Double Down copies five TR after the Action phase skip is used`() {
    newProgramGame(extraOptions = "BoardOfDirectors")
    playPreludes(PreservationProgram, BoardOfDirectors)
    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }

    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          kim.playPrelude(DoubleDown) {
            doTask("CopyPrelude<PreservationProgram>")
            repeat(5) { doTask("TerraformRating") }
          }
        }
        .expect("5 TerraformRating")
  }

  @Test
  fun `Pharmacy Union consumes a disease even when its TR gain is prevented`() {
    startPharmacyGame()
    playPreludes()

    kim.playProject(PhysicsComplex, 12) {
          doTask("SkippedTerraformRating FROM Disease<$PharmacyUnion>")
        }
        .expect("-Disease<$PharmacyUnion>, 0 TerraformRating")
  }

  @Test
  fun `World Government Advisor does not consume the skip`() {
    newProgramGame(extraOptions = "WorldGovernmentAdvisor")
    playPreludes(PreservationProgram, WorldGovernmentAdvisor)
    passGeneration()
    stan.pass()
    kim.cardAction1(WorldGovernmentAdvisor) { doTask("TemperatureStep BY Admin") }
        .expect("TemperatureStep, 0 TerraformRating")

    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
        .expect("0 TerraformRating")
  }

  @Test
  fun `Solar chairman TR does not consume the following generations skip`() {
    newProgramGame(extraOptions = "TurmoilExpansion")
    playPreludes()
    kim.turn {
      stdAction("LobbyAction<Action1>") { doTask("PartyDelegate<Scientists>") }
      stdAction("LobbyAction<Action2>", payment = { pay(5) }) {
        doTask("PartyDelegate<Scientists>")
      }
    }
    stan.pass()
    val rating = kim.count("TerraformRating")
    passGeneration(nextEvent = "ExploreFirstDirective")
    kim.count("Chairman") shouldBe 1
    // Turmoil's TR revision loses a step, then becoming chairman restores it.
    kim.count("TerraformRating") shouldBe rating
    stan.pass()

    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
        .expect("0 TerraformRating")
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
              kim.playPrelude(PreservationProgram) {
                repeat(5) { doTask("TerraformRating") }
              }
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
    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
    kim.stdProject("AsteroidProject") { doTask("TerraformRating") }
    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          kim.playPrelude(Merger) { kim.playCorp(Pristar) }
        }
        .expect("-2 TerraformRating")

    kim.stdProject("AsteroidProject") { doTask("TerraformRating") }.expect("TerraformRating")
  }

  @Test
  fun `A capped global parameter does not consume the skip`() {
    newProgramGame()
    playPreludes()
    repeat(14) { passGeneration("OxygenStep") }
    kim.turn { convertPlants { placeTile(2, 4) }.expect("0 OxygenStep, 0 TerraformRating") }
    stan.pass()

    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
        .expect("0 TerraformRating")
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
          doTask("SkippedTerraformRating")
          repeat(2) { doTask("TerraformRating") }
        }
        .expect("OxygenStep, TemperatureStep, OceanTile, 2 TerraformRating")
  }

  @Test
  fun `Pharmacy Union flip loses part of its TR and remains a played event`() {
    startPharmacyGame()
    playPreludes()
    kim.turn {
      playProject(Research, 11) {
        doTask("SkippedTerraformRating FROM Disease<$PharmacyUnion>")
        doTask("TerraformRating FROM Disease<$PharmacyUnion>")
      }
    }
    stan.pass()
    passGeneration()
    stan.pass()

    kim.playProject(PhysicsComplex, 12) {
          selectTask(
              "PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion THEN 3 TryTerraformRating"
          )
          doTask("SkippedTerraformRating")
          repeat(2) { doTask("TerraformRating") }
        }
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
          kim.playPrelude(PreservationProgram) {
            repeat(5) { doTask("TerraformRating") }
          }
        }
        .expect("5 TerraformRating")
  }

  @Test
  fun `Reds ruling bonus outside the Action phase leaves the next generations skip available`() {
    newProgramGame(extraOptions = "TurmoilExpansion")
    kim.playPrelude(PreservationProgram)
    kim.playPrelude(PowerGeneration)
    stan.playPrelude(UnmiContractor)
    stan.playPrelude(Donation)
    kim.turn {
      stdAction("LobbyAction<Action1>") { doTask("PartyDelegate<Reds>") }
      stdAction("LobbyAction<Action2>", payment = { pay(5) }) { doTask("PartyDelegate<Reds>") }
    }
    stan.turn { repeat(2) { stdProject("AsteroidProject") } }
    kim.pass()
    stan.stdProject("AsteroidProject")
    // Kim has the lower rating; Reds and the chairmanship each grant TR after revision.
    val rating = kim.count("TerraformRating")
    stan.pass()
    kim.wgt("VenusStep")
    kim.count("TerraformRating") shouldBe rating + 1
    admin.doTask("ExploreFirstDirective")
    players.forEach { it.buyCards(0) }
    stan.pass()

    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
        .expect("0 TerraformRating")
  }

  @Test
  fun `Venus threshold awards its additional TR while skipping a single step`() {
    newProgramGame()
    playPreludes()
    repeat(7) { passGeneration("VenusStep") }
    stan.pass()

    kim.stdProject("AirScrappingProject") {
          doTask("SkippedTerraformRating")
          doTask("TerraformRating")
        }
        .expect("VenusStep, TerraformRating")
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

    kim.stdProject("BufferGasProject") { doTask("SkippedTerraformRating") }
        .expect("0 TerraformRating")
    kim.stdProject("BufferGasProject") { doTask("TerraformRating") }.expect("TerraformRating")
  }

  private fun startPharmacyGame() {
    newTestGameBeforeCorporationPlay(
        addOptions = "PreludeExpansion, PreservationProgram",
        playerCount = 2,
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

  // Preservation Program first-TR timing:
  // https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // Terraforming Deal remains active:
  // https://boardgamegeek.com/thread/3343166/article/44660687#44660687
  // Reds payment:
  // https://boardgamegeek.com/thread/2196388/article/31885882#31885882
  @Test
  internal fun `Terraforming Deal pays only for the retained TR in a fixed gain`() {
    startPaymentDefectGame(deal = true)
    kim.playProject(MagneticFieldGeneratorsPromo, 22) {
          placeTile(2, 4)
          doTask("SkippedTerraformRating")
          repeat(2) { doTask("TerraformRating") }
        }
        .expect("2 TerraformRating, -18 MC")
  }

  @Test
  internal fun `Reds charges nothing for the prevented TR`() {
    startPaymentDefectGame(deal = false, rulingParty = "Reds")
    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
        .expect("0 TerraformRating, -14 MC")
  }

  @Test
  internal fun `Reds and Terraforming Deal both count only retained TR`() {
    startPaymentDefectGame(deal = true, rulingParty = "Reds")
    kim.playProject(MagneticFieldGeneratorsPromo, 22) {
          placeTile(2, 4)
          doTask("SkippedTerraformRating")
          repeat(2) { doTask("TerraformRating") }
        }
        .expect("2 TerraformRating, -24 MC")
  }

  @Test
  internal fun `Reds permits an affordable two-step gain when the first TR is prevented`() {
    prepareAffordableTwoStepGain()
    kim.playProject(ReleaseOfInertGases, 14) {
          doTask("SkippedTerraformRating")
          doTask("TerraformRating")
        }
        .expect("TerraformRating, -17 MC")
  }

  private fun prepareAffordableTwoStepGain() {
    startPaymentDefectGame(deal = false, rulingParty = "Reds")
    // Ordinary purchases leave exactly 17 MC and a card, without using the generation's TR skip.
    kim.turn {
      repeat(5) { stdProject("PowerPlantProject") }
      sellPatents(9)
    }
  }

  // Terraforming Deal outside Action:
  // https://boardgamegeek.com/thread/3343166/article/44660753#44660753
  @Test
  internal fun `Terraforming Deal ignores prevented TR after a chairman award`() {
    startPaymentDefectGame(deal = true, rulingParty = "Scientists")
    kim.stdProject("AsteroidProject") { doTask("SkippedTerraformRating") }
        .expect("0 TerraformRating, -14 MC")
  }

  private fun startPaymentDefectGame(deal: Boolean, rulingParty: String? = null) {
    val politics = if (rulingParty == null) "" else ", TurmoilExpansion"
    newProgramGame(extraOptions = "TerraformingDeal$politics")
    playPreludes(PreservationProgram, if (deal) TerraformingDeal else PowerGeneration)
    kim.turn {
      if (rulingParty != null) {
        stdAction("LobbyAction<Action1>") { doTask("PartyDelegate<$rulingParty>") }
        stdAction("LobbyAction<Action2>", payment = { pay(5) }) {
          doTask("PartyDelegate<$rulingParty>")
        }
      } else {
        playProject(GiantSpaceMirror, 17)
      }
    }
    stan.pass()
    if (deal && rulingParty != null) kim.turn { playProject(GiantSpaceMirror, 17) }
    passGeneration(nextEvent = if (rulingParty == null) null else "ExploreFirstDirective")
    stan.pass()
  }
}
