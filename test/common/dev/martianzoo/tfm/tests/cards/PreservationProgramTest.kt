package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestOption
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PreservationProgramTest :
    CardTest(
        parseClasses(
                """
        CLASS TrAttributionProbe : Owned<Player> {
          TerraformRating BY Admin:: Plant
          TerraformRating BY Me@Player:: Heat
        }
        """
            )
            .toSet()
    ) {
  private fun setUpProgramGame(
      baseOnly: Boolean = false,
      players: Int = 2,
      extraOptions: Array<TestOption> = emptyArray(),
  ) {
    newGame(
        *if (baseOnly) emptyArray()
        else arrayOf(PreludeExpansion, Prelude2CardPack, TurmoilExpansion, PromoCardPack, Unsafe),
        *extraOptions,
        players = players,
    )
  }

  private fun startPrelude() {
    setUpProgramGame()
    p1.playCorp(CrediCor, 2)
    admin.phase("Prelude")
  }

  private fun startLaterGeneration(corporation: String = "$PhoboLog") {
    setUpProgramGame()
    admin.phase("Prelude")
    p1.runOperation("$corporation, PreservationProgram, 100 MC")
    admin.phase("Action")
    admin.nextGeneration(0, 0)
  }

  @Test
  fun `prelude benefit is five TR and does not consume the action phase skip`() {
    startPrelude()
    p1.playPrelude(PreservationProgram).expect("5 TerraformRating")
    admin.phase("Action")
    p1.stdProject("AsteroidProject").expect("TemperatureStep, 0 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TemperatureStep, TerraformRating")
  }

  @Test
  fun `earlier prelude TR does not consume the action phase skip`() {
    startPrelude()
    p1.playPrelude(UnmiContractor)
    p1.playPrelude(PreservationProgram).expect("5 TerraformRating")
    admin.phase("Action")
    p1.stdProject("AsteroidProject").expect("TemperatureStep, 0 TerraformRating")
  }

  @Test
  fun `prelude TR does not waive the skip when Valley Trust plays PP`() {
    setUpProgramGame()
    p1.playCorp(ValleyTrust, 0)
    admin.phase("Prelude")
    p1.playPrelude(UnmiContractor)
    p1.playPrelude(Donation)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") {
          p1.playPrelude(PreservationProgram, location = cn("Selecting"))
        }
        .expect("4 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `playing PP as a first action skips one of its own five TR`() {
    setUpProgramGame()
    p1.playCorp(ValleyTrust, 0)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") {
          p1.playPrelude(PreservationProgram, location = cn("Selecting"))
        }
        .expect("4 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `earlier TR in the same turn prevents a skip when Board of Directors plays PP`() {
    startPrelude()
    p1.playPrelude(BoardOfDirectors)
    p1.playPrelude(Donation)
    admin.phase("Action")
    p1.turn {
      stdProject("AsteroidProject").expect("TerraformRating")
      cardAction1(BoardOfDirectors) {
            doTask("-12 MC")
            p1.playPrelude(PreservationProgram)
          }
          .expect("5 TerraformRating")
    }
    p1.stdProject("AsteroidProject").expect("TerraformRating")
    admin.nextGeneration(0, 0)
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
  }

  @Test
  fun `an earlier non TR action leaves the skip available when PP is played second`() {
    startPrelude()
    p1.playPrelude(BoardOfDirectors)
    p1.playPrelude(Donation)
    admin.phase("Action")
    p1.turn {
      stdProject("PowerPlantProject").expect("0 TerraformRating")
      cardAction1(BoardOfDirectors) {
            doTask("-12 MC")
            p1.playPrelude(PreservationProgram)
          }
          .expect("4 TerraformRating")
    }
  }

  @Test
  fun `UNMI cannot use a prevented gain to satisfy its gate`() {
    startLaterGeneration("$UnitedNationsMarsInitiative")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    shouldThrow<RequirementException> { p1.cardAction1(UnitedNationsMarsInitiative) }
    p1.stdProject("AsteroidProject").expect("TerraformRating")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("TerraformRating, -3 MC")
  }

  @Test
  fun `PP prelude benefit still qualifies UNMI in generation one`() {
    setUpProgramGame()
    p1.playCorp(UnitedNationsMarsInitiative, 0)
    admin.phase("Prelude")
    p1.playPrelude(PreservationProgram)
    admin.phase("Action")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("0 TerraformRating, -3 MC")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `PP prelude benefit still qualifies UNMI after the first Action phase gain is reversed`() {
    setUpProgramGame()
    p1.playCorp(UnitedNationsMarsInitiative, 0)
    admin.phase("Prelude")
    p1.playPrelude(PreservationProgram)
    admin.phase("Action")

    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    p1.cardAction1(UnitedNationsMarsInitiative).expect("TerraformRating, -3 MC")
  }

  @Test
  fun `PP prelude benefit still disqualifies Pristar after the first Action phase gain is reversed`() {
    setUpProgramGame()
    p1.playCorp(Pristar, 0)
    admin.phase("Prelude")
    p1.playPrelude(PreservationProgram)
    admin.phase("Action")

    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    admin.phase("Production")
    p1.count("Preservation") shouldBe 0
  }

  @Test
  fun `Pristar retains its production bonus after only a prevented gain`() {
    startLaterGeneration("$Pristar")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    val rating = p1.count("TerraformRating")
    admin
        .runOperation("ProductionPhase FROM Phase")
        .expect(
            "Preservation<$Pristar<Player1>>, ${rating + p1.production(cn("MC")) + 6} MC<Player1>"
        )
  }

  @Test
  fun `Pristar loses its production bonus after an actual gain`() {
    startLaterGeneration("$Pristar")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
    admin.phase("Production")
    p1.count("Preservation") shouldBe 0
  }

  @Test
  fun `a fixed three TR gain loses only one step`() {
    startLaterGeneration()
    p1.runOperation("ProjectCard, PROD[4 Energy]")
    p1.playProject(MagneticFieldGeneratorsPromo, 22) { placeTile(2, 4) }
        .expect("2 TerraformRating, -22 MC")
    p1.stdProject("AsteroidProject").expect("TerraformRating, -14 MC")
  }

  @Test
  fun `Terraforming Ganymede skips only one of its computed TR steps`() {
    setUpProgramGame()
    p1.playCorp(SaturnSystems, 1)
    admin.phase("Prelude")
    p1.playPrelude(PreservationProgram)
    p1.playPrelude(GalileanMining)
    admin.phase("Action")
    p1.runOperation("100 MC")

    p1.playProject(TerraformingGanymede, 33).expect("2 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `Terraforming Ganymede awards all its computed TR after the skip is used`() {
    setUpProgramGame()
    p1.playCorp(SaturnSystems, 1)
    admin.phase("Prelude")
    p1.playPrelude(PreservationProgram)
    p1.playPrelude(GalileanMining)
    admin.phase("Action")
    p1.runOperation("100 MC")

    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    p1.playProject(TerraformingGanymede, 33).expect("3 TerraformRating")
  }

  @Test
  fun `opponents gain TR without consuming the owners skip`() {
    startLaterGeneration()
    requireP2().runOperation("100 MC")
    requireP2().stdProject("AsteroidProject").expect("TerraformRating")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    requireP2().stdProject("AsteroidProject").expect("TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `skip resets each generation and not at turn boundaries`() {
    startLaterGeneration()
    p1.turn { stdProject("AsteroidProject").expect("0 TerraformRating") }
    p1.turn { stdProject("AsteroidProject").expect("TerraformRating") }
    admin.nextGeneration(0, 0)
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `Double Down copies the five TR without adding another skip`() {
    startPrelude()
    p1.playPrelude(PreservationProgram)
    p1.playPrelude(DoubleDown) { doTask("CopyPrelude<PreservationProgram>") }
        .expect("5 TerraformRating")
    admin.phase("Action")
    p1.playProject(Comet, 21) { placeTile(1, 2) }.expect("TerraformRating")
  }

  @Test
  fun `Double Down copies only four TR when the Action phase skip is unused`() {
    setUpProgramGame()
    p1.playCorp(ValleyTrust, 0)
    admin.phase("Prelude")
    p1.playPrelude(PreservationProgram)
    p1.playPrelude(Donation)
    admin.phase("Action")

    p1.stdAction("DoRequiredActionsAction") {
          p1.playPrelude(DoubleDown, location = cn("Selecting")) {
            doTask("CopyPrelude<PreservationProgram>")
          }
        }
        .expect("4 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `Double Down copies five TR after the Action phase skip is used`() {
    setUpProgramGame()
    p1.playCorp(CrediCor, 0)
    admin.phase("Prelude")
    p1.playPrelude(PreservationProgram)
    p1.playPrelude(BoardOfDirectors)
    admin.phase("Action")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")

    p1.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          p1.playPrelude(DoubleDown) { doTask("CopyPrelude<PreservationProgram>") }
        }
        .expect("5 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `Pharmacy Union consumes a disease even when its TR gain is prevented`() {
    startLaterGeneration("$PharmacyUnion")
    p1.runOperation("Disease<$PharmacyUnion>")
    p1.playProject(PhysicsComplex, 12).expect("-Disease<$PharmacyUnion>, 0 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `World Government Advisor does not consume the skip`() {
    startLaterGeneration()
    p1.runOperation("$WorldGovernmentAdvisor")
    admin.nextGeneration(0, 0)
    p1.cardAction1(WorldGovernmentAdvisor) { doTask("TemperatureStep BY Admin") }
        .expect("TemperatureStep, 0 TerraformRating")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
  }

  @Test
  fun `solar phase chairman TR is awarded and does not consume the next generations skip`() {
    startLaterGeneration()
    p1.runOperation("2 PartyDelegate<Scientists>")
    admin.phase("Solar")
    admin.runOperation("FormGovernment").expect("TerraformRating<Player1>, 0 MC<Player1>")
    admin.phase("Research") {
      p1.buyCards(0)
      requireP2().buyCards(0)
    }
    admin.phase("Action")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating, -14 MC")
  }

  @Test
  fun `base game TR sources work without PP or any Prelude options`() {
    setUpProgramGame(baseOnly = true)
    p1.playCorp(PhoboLog, 0)
    p1.runOperation("10 MC")
    admin.phase("Action")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `earlier gain and subsequent loss still prevents a skip when PP is played`() {
    startPrelude()
    p1.playPrelude(BoardOfDirectors)
    p1.playPrelude(Donation)
    admin.phase("Action")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
    p1.runOperation("-TerraformRating")
    p1.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          p1.playPrelude(PreservationProgram)
        }
        .expect("5 TerraformRating")
  }

  @Test
  fun `a TR loss does not restore a consumed skip`() {
    startLaterGeneration()
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
    p1.runOperation("-TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `a capped global parameter does not consume the skip`() {
    startLaterGeneration()
    admin.runOperation("14 OxygenStep")
    p1.runOperation("8 Plant")
    p1.convertPlants { placeTile(2, 4) }.expect("0 OxygenStep, 0 TerraformRating")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating")
    p1.stdProject("AsteroidProject").expect("TerraformRating")
  }

  @Test
  fun `oxygen temperature and ocean threshold gains skip exactly one TR`() {
    startLaterGeneration()
    admin.runOperation("7 OxygenStep, 14 TemperatureStep")
    p1.runOperation("8 Plant")
    p1.convertPlants {
          doTask("GreeneryTile<Tharsis_2_4>")
          placeTile(1, 2)
        }
        .expect("OxygenStep, TemperatureStep, OceanTile, 2 TerraformRating, 4 MC")
  }

  @Test
  fun `Pharmacy Union flip loses one of its three TR and remains a played event`() {
    startLaterGeneration("$PharmacyUnion")
    p1.runOperation("ProjectCard, -2 Disease<$PharmacyUnion>")
    p1.playProject(PhysicsComplex, 12) {
          doTask("PlayedEvent FROM $PharmacyUnion")
        }
        .expect("2 TerraformRating, -12 MC")
    p1.count("$PharmacyUnion") shouldBe 0
    p1.count("PlayedEvent<Class<$PharmacyUnion>>") shouldBe 1
  }

  @Test
  fun `earlier TR in a prior turn prevents a skip when PP is acquired`() {
    startPrelude()
    p1.playPrelude(BoardOfDirectors)
    p1.playPrelude(Donation)
    admin.phase("Action")
    p1.turn { stdProject("AsteroidProject").expect("TerraformRating") }
    p1.turn {
      cardAction1(BoardOfDirectors) {
            doTask("-12 MC")
            p1.playPrelude(PreservationProgram)
          }
          .expect("5 TerraformRating")
    }
  }

  @Test
  fun `chairman TR retains Admin attribution`() {
    startLaterGeneration()
    p1.runOperation("TrAttributionProbe, 2 PartyDelegate<Scientists>")
    admin.phase("Solar")
    admin
        .runOperation("FormGovernment")
        .expect("TerraformRating<Player1>, Plant<Player1>, 0 Heat<Player1>")
  }

  @Test
  fun `Reds ruling bonus outside the Action phase does not consume the skip`() {
    startLaterGeneration()
    p1.runOperation("-10 TerraformRating")
    admin.phase("Solar")
    admin.runOperation("ApplyRulingBonus<Reds>").expect("TerraformRating<Player1>, 0 MC<Player1>")
    admin.phase("Action")
    p1.stdProject("AsteroidProject").expect("0 TerraformRating, -14 MC")
  }

  @Test
  fun `Venus threshold awards its additional TR while skipping exactly one`() {
    setUpProgramGame(extraOptions = arrayOf(VenusNextExpansion))
    admin.phase("Prelude")
    p1.runOperation("$PhoboLog, PreservationProgram, 100 MC")
    admin.phase("Action")
    admin.nextGeneration(0, 0)
    admin.runOperation("7 VenusStep")
    p1.stdProject("AirScrappingProject").expect("VenusStep, TerraformRating, -15 MC")
    p1.stdProject("AsteroidProject").expect("TerraformRating, -14 MC")
  }

  @Test
  fun `solo Buffer Gas uses the skip while starting TR remains fourteen`() {
    setUpProgramGame(players = 1, extraOptions = arrayOf(Tr63SoloObjective))
    p1.count("TerraformRating") shouldBe 14
    p1.playCorp(PhoboLog, 0)
    admin.phase("Prelude")
    p1.playPrelude(PreservationProgram).expect("5 TerraformRating")
    p1.playPrelude(Donation)
    admin.phase("Action")
    p1.stdProject("BufferGasProject").expect("0 TerraformRating, -16 MC")
    p1.stdProject("BufferGasProject").expect("TerraformRating, -16 MC")
  }
}
