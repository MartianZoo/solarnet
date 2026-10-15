package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class PreservationProgramTest : TfmGameplayTest() {
  @Test
  fun `Prelude benefit leaves the Action phase skip available`() {
    newProgramGame()
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
        .expect("5 TerraformRating")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    playStansPreludes()

    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("TemperatureStep, 0 TerraformRating")
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("TemperatureStep, TerraformRating")
  }

  @Test
  fun `Earlier Prelude TR leaves the Action phase skip available`() {
    newProgramGame()
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>")
    kim.autoExecNow(EAGER)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
        .expect("5 TerraformRating")
    playStansPreludes()

    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("TemperatureStep, 0 TerraformRating")
  }

  @Test
  fun `Prevented TR gain renews each generation rather than each turn`() {
    newProgramGame()
    playPreludes()
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating")
    kim.doTask("0 StandardAction") // Decline the second action.
    stan.doTasks("UseStandardProject<SellPatentsProject>", "1 MC FROM ProjectCard")
    stan.doTask("0 StandardAction") // Decline the second action.
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    kim.doTask("0 StandardAction") // Decline the second action.
    stan.doTask("Pass")
    passGeneration()
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating")
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
  }

  @Test
  fun `Earlier Prelude TR does not waive the skip when Valley Trust plays the Program`() {
    newProgramGame(ValleyTrust)
    playPreludes(UnmiContractor, Donation)

    kim.completeOperation {
          doTask("RequiredActionsSignal")
          doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
          autoExecNow(EAGER)
        }
        .expect("4 TerraformRating")
  }

  @Test
  fun `Playing the Program as a first action skips part of its own benefit`() {
    newProgramGame(ValleyTrust)
    playPreludes(PowerGeneration, Donation)

    kim.completeOperation {
          doTask("RequiredActionsSignal")
          doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
          autoExecNow(EAGER)
        }
        .expect("4 TerraformRating")
  }

  @Test
  fun `Earlier TR in the same turn prevents a skip when Board of Directors plays the Program`() {
    newProgramGame(extraOptions = "BoardOfDirectors")
    playPreludes(BoardOfDirectors, Donation)

    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
          doTask("UseCardAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          autoExecNow(EAGER)
          doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
          autoExecNow(EAGER)
        }
        .expect("5 TerraformRating")
  }

  @Test
  fun `Earlier non-TR action leaves the skip available when the Program is played second`() {
    newProgramGame(extraOptions = "BoardOfDirectors")
    playPreludes(BoardOfDirectors, Donation)

    kim.doTasks("UseStandardProject<PowerPlantProject>")
    kim.completeOperation {
          doTask("UseCardAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          autoExecNow(EAGER)
          doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
          autoExecNow(EAGER)
        }
        .expect("4 TerraformRating")
  }

  @Test
  fun `UNMI cannot use a prevented gain to satisfy its requirement`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    passGeneration()
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }

    shouldThrow<RequirementException> {
      kim.doTasks("UseCardAction<$UnitedNationsMarsInitiative, Action1>")
    }
    kim.count("TerraformRating") shouldBe 25
  }

  @Test
  fun `UNMI can act after an actual gain following a prevented gain`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    passGeneration()
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }

    kim.doTasks("UseCardAction<$UnitedNationsMarsInitiative, Action1>").expect("TerraformRating")
  }

  @Test
  fun `Prelude benefit qualifies UNMI before the first Action phase gain`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()

    kim.doTasks("UseCardAction<$UnitedNationsMarsInitiative, Action1>").expect("0 TerraformRating")
  }

  @Test
  fun `Prelude benefit still qualifies UNMI after the first Action phase gain is prevented`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }

    kim.doTasks("UseCardAction<$UnitedNationsMarsInitiative, Action1>").expect("TerraformRating")
  }

  @Test
  fun `Prelude benefit still disqualifies Pristar after a prevented Action phase gain`() {
    newProgramGame(Pristar, extraOptions = "Pristar")
    playPreludes()
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    kim.doTask("0 StandardAction") // Decline the second action.
    stan.doTask("Pass")

    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    admin.count("SolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 0
  }

  @Test
  fun `Pristar receives its production bonus after only a prevented gain`() {
    newProgramGame(Pristar, extraOptions = "Pristar")
    playPreludes()
    passGeneration()
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }

    expectChanges("30 MC<Kim>, Preservation<Pristar<Kim>>") {
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  fun `Pristar loses its production bonus after an actual gain`() {
    newProgramGame(Pristar, extraOptions = "Pristar")
    playPreludes()
    passGeneration()
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }

    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    admin.count("SolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 0
  }

  @Test
  fun `A fixed three-TR gain loses only a single step`() {
    newProgramGame()
    playPreludes(PreservationProgram, PowerGeneration)

    kim.completeOperation {
          doTasks(
              "PlayProject<Class<$MagneticFieldGeneratorsPromo>>",
          )
          doTask("MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_2_4>")
          autoExecNow(EAGER)
        }
        .expect("2 TerraformRating")
  }

  @Test
  fun `Terraforming Ganymede skips a single computed TR step`() {
    newProgramGame(SaturnSystems)
    playPreludes(PreservationProgram, GalileanMining)
    passGeneration()
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks(
              "PlayProject<Class<$TerraformingGanymede>>",
              "Ok", /* No Titanium payment. */
          )
          autoExecNow(EAGER)
        }
        .expect("2 TerraformRating")
  }

  @Test
  fun `Terraforming Ganymede awards all computed TR after the skip is used`() {
    newProgramGame(SaturnSystems)
    playPreludes(PreservationProgram, GalileanMining)
    passGeneration()
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }

    kim.completeOperation {
          doTasks(
              "PlayProject<Class<$TerraformingGanymede>>",
              "4 Pay<Class<Titanium>> FROM Titanium",
          )
          autoExecNow(EAGER)
        }
        .expect("3 TerraformRating")
  }

  @Test
  fun `Opponents gain TR without consuming the owners skip`() {
    newProgramGame()
    playPreludes()
    passGeneration()
    stan
        .completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    stan.doTask("0 StandardAction") // Decline the second action.
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating")
    kim.doTask("0 StandardAction") // Decline the second action.
    stan
        .completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    stan.doTask("0 StandardAction") // Decline the second action.
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
  }

  @Test
  fun `Double Down copies the Prelude benefit without adding another skip`() {
    newProgramGame()
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
    kim.completeOperation {
          doTask("PlayCard<Class<PreludeCard>, Class<$DoubleDown>>")
          doTask("CopyPrelude<PreservationProgram>")
          autoExecNow(EAGER)
        }
        .expect("5 TerraformRating")
    playStansPreludes()

    kim.completeOperation {
          doTasks("PlayProject<Class<$Comet>>", "Ok" /* No Titanium payment. */)
          doTask("OceanTile<Tharsis_1_2>")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
  }

  @Test
  fun `Double Down copies four TR when the Action phase skip is unused`() {
    newProgramGame(ValleyTrust)
    playPreludes()

    kim.completeOperation {
          doTask("RequiredActionsSignal")
          doTask("PlayCard<Class<PreludeCard>, Class<$DoubleDown>>")
          doTask("CopyPrelude<PreservationProgram>")
          autoExecNow(EAGER)
        }
        .expect("4 TerraformRating")
  }

  @Test
  fun `Double Down copies five TR after the Action phase skip is used`() {
    newProgramGame(extraOptions = "BoardOfDirectors")
    playPreludes(PreservationProgram, BoardOfDirectors)
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }

    kim.completeOperation {
          doTask("UseCardAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          autoExecNow(EAGER)
          doTask("PlayCard<Class<PreludeCard>, Class<$DoubleDown>>")
          doTask("CopyPrelude<PreservationProgram>")
          autoExecNow(EAGER)
        }
        .expect("5 TerraformRating")
  }

  @Test
  fun `Pharmacy Union consumes a disease even when its TR gain is prevented`() {
    startPharmacyGame()
    playPreludes()

    kim.doTasks("PlayProject<Class<$PhysicsComplex>>", "Ok" /* No Steel payment. */)
        .expect("-Disease<$PharmacyUnion>, 0 TerraformRating")
  }

  @Test
  fun `World Government Advisor does not consume the skip`() {
    newProgramGame(extraOptions = "WorldGovernmentAdvisor")
    playPreludes(PreservationProgram, WorldGovernmentAdvisor)
    passGeneration()
    stan.doTask("Pass")
    kim.completeOperation {
          doTask("UseCardAction<$WorldGovernmentAdvisor, Action1>")
          doTask("TemperatureStep BY Admin")
        }
        .expect("TemperatureStep, 0 TerraformRating")

    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating")
  }

  @Test
  fun `Solar chairman TR does not consume the following generations skip`() {
    newProgramGame(extraOptions = "TurmoilExpansion, Unsafe")
    playPreludes()
    kim.completeOperation {
      doTask("LobbyAction")
      doTask("PartyLeader<Scientists> FROM LobbyDelegate")
    }
    kim.completeOperation {
      doTask("PaidLobbyAction")

      doTask("PartyFollower<Scientists>")
    }
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("0 TerraformRating<Kim>") {
      passGeneration(
          nextEvent = "ExploreFirstDirective",
          neutralPlacements =
              listOf("PartyLeader<Greens, Neutral>", "PartyFollower<Reds, Neutral>"),
      )
      kim.count("Chairman") shouldBe 1
      // Turmoil's TR revision loses a step, then becoming chairman restores it.
    }
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating")
  }

  @Test
  fun `A gain followed by a loss still prevents a skip when the Program is acquired`() {
    newProgramGame(extraOptions = "BoardOfDirectors, Pristar")
    playPreludes(BoardOfDirectors, Donation)
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    kim.doTask("0 StandardAction") // Decline the second action.
    stan.doTask("Pass")
    kim.completeOperation {
          doTask("UseCardAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          autoExecNow(EAGER)
          doTask("PlayCard<Class<PreludeCard>, Class<$Merger>>")
          doTask("PlayCard<Class<CorporationCard>, Class<$Pristar>>")
          autoExecNow(EAGER)
        }
        .expect("-2 TerraformRating")

    kim.completeOperation {
          doTask("PlayProject<Class<$ProjectInspection>>")
          doTask("UseAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          autoExecNow(EAGER)
          doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
          autoExecNow(EAGER)
        }
        .expect("5 TerraformRating")
  }

  @Test
  fun `A TR loss does not restore a consumed skip`() {
    newProgramGame(extraOptions = "BoardOfDirectors, Pristar")
    playPreludes(PreservationProgram, BoardOfDirectors)
    passGeneration()
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
          doTask("UseCardAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          autoExecNow(EAGER)
          doTask("PlayCard<Class<PreludeCard>, Class<$Merger>>")
          doTask("PlayCard<Class<CorporationCard>, Class<$Pristar>>")
          autoExecNow(EAGER)
        }
        .expect("-2 TerraformRating")

    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
  }

  @Test
  fun `A capped global parameter does not consume the skip`() {
    newProgramGame()
    playPreludes()
    repeat(14) { passGeneration("OxygenStep") }
    kim.completeOperation {
          doTasks("ConvertPlants")
          doTask("GreeneryTile<Tharsis_2_4>")
        }
        .expect("0 OxygenStep, 0 TerraformRating")
    kim.doTask("0 StandardAction") // Decline the second action.
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating")
  }

  @Test
  fun `Linked oxygen temperature and ocean gains skip a single TR`() {
    newProgramGame()
    playPreludes()
    repeat(7) { passGeneration("OxygenStep") }
    repeat(14) { passGeneration("TemperatureStep") }
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks("ConvertPlants")
          doTask("GreeneryTile<Tharsis_2_4>")
          autoExecNow(EAGER)
          doTask("OceanTile<Tharsis_1_2>")
          autoExecNow(EAGER)
        }
        .expect("OxygenStep, TemperatureStep, OceanTile, 2 TerraformRating")
  }

  @Test
  fun `Pharmacy Union flip loses part of its TR and remains a played event`() {
    startPharmacyGame()
    playPreludes()
    kim.completeOperation {
      doTasks("PlayProject<Class<$Research>>")
      autoExecNow(EAGER)
    }
    kim.doTask("0 StandardAction") // Decline the second action.
    stan.doTask("Pass")
    passGeneration()
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks("PlayProject<Class<$PhysicsComplex>>")
          doTask("Ok") // Keep the steel before paying the exact MC remainder.
          autoExecNow(EAGER)
          doTask("PlayedEvent FROM $PharmacyUnion")
          autoExecNow(EAGER)
        }
        .expect("2 TerraformRating")
    kim.count("$PharmacyUnion") shouldBe 0
    kim.count("PlayedEvent<Class<$PharmacyUnion>>") shouldBe 1
  }

  @Test
  fun `Earlier TR in a prior turn prevents a skip when the Program is acquired`() {
    newProgramGame(extraOptions = "BoardOfDirectors")
    playPreludes(BoardOfDirectors, Donation)
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    kim.doTask("0 StandardAction") // Decline the second action.
    stan.doTask("Pass")

    kim.completeOperation {
          doTask("UseCardAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          autoExecNow(EAGER)
          doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
          autoExecNow(EAGER)
        }
        .expect("5 TerraformRating")
  }

  @Test
  fun `Reds ruling bonus outside the Action phase leaves the next generations skip available`() {
    newProgramGame(extraOptions = "TurmoilExpansion, Unsafe")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$PowerGeneration>>")
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>")
    stan.autoExecNow(EAGER)
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    kim.completeOperation {
      doTask("LobbyAction")
      doTask("PartyFollower<Reds> FROM LobbyDelegate")
    }
    kim.completeOperation {
      doTask("PaidLobbyAction")

      doTask("PartyLeader<Reds>")
    }
    stan.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    stan.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    kim.doTask("Pass")
    stan.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    // Kim has the lower rating; Reds and the chairmanship each grant TR after revision.
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("1 TerraformRating<Kim>") {
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
    admin.doTasks(
        "PartyLeader<Greens, Neutral>",
        "ExploreFirstDirective",
        "PartyLeader<Reds, Neutral>",
    )
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating")
  }

  @Test
  fun `Venus threshold awards its additional TR while skipping a single step`() {
    newProgramGame()
    playPreludes()
    repeat(7) { passGeneration("VenusStep") }
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks("UseStandardProject<AirScrappingProject>")
          autoExecNow(EAGER)
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
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
        .expect("5 TerraformRating")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")

    kim.completeOperation {
          doTasks("UseStandardProject<BufferGasProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating")
    kim.completeOperation {
          doTasks("UseStandardProject<BufferGasProject>")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
  }

  private fun startPharmacyGame() {
    newTestGameBeforeCorporationPlay(
        addOptions = "PreludeExpansion, PreservationProgram",
        playerCount = 2,
    )
    kim.completeOperation {
      doTask("PlayCard<Class<CorporationCard>, Class<$PharmacyUnion>>")
      autoExecNow(EAGER)
    }
    stan.completeOperation {
      doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation2>, Hand>")
      autoExecNow(EAGER)
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
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$first>>")
    kim.autoExecNow(EAGER)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$second>>")
    kim.autoExecNow(EAGER)
    playStansPreludes()
  }

  private fun playStansPreludes() {
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
    stan.autoExecNow(EAGER)
  }

  /** Called at the start of a generation, or after Stan has passed and Kim finishes play. */
  private fun passGeneration(
      worldGovernment: String = "VenusStep",
      nextEvent: String? = null,
      neutralPlacements: List<String> = emptyList(),
  ) {
    val first = players.indexOfFirst { it.count("StartToken") == 1 }
    val order = players.drop(first) + players.take(first)
    order.filter { it.count("Pass") == 0 }.forEach { it.doTask("Pass") }
    players.forEach { it.autoExecNow(EAGER) }
    order.first().doTask("$worldGovernment! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }

    if (nextEvent != null) {
      admin.doTasks(neutralPlacements[0], nextEvent, neutralPlacements[1])
    }
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
  }

  // These pairs explicitly select unsupported combinations with Unsafe.
  // Preservation Program first-TR timing:
  // https://boardgamegeek.com/thread/3353355/article/44740462#44740462
  // Terraforming Deal remains active:
  // https://boardgamegeek.com/thread/3343166/article/44660687#44660687
  // Reds payment:
  // https://boardgamegeek.com/thread/2196388/article/31885882#31885882
  @Ignore // The removed TR still triggers Terraforming Deal.
  @Test
  internal fun `Terraforming Deal pays only for the retained TR in a fixed gain`() {
    startPaymentDefectGame(deal = true)
    kim.completeOperation {
          doTasks(
              "PlayProject<Class<$MagneticFieldGeneratorsPromo>>",
              "Ok", /* No Steel payment. */
          )
          doTask("MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_2_4>")
          autoExecNow(EAGER)
        }
        .expect("2 TerraformRating, -18 MC")
  }

  @Test
  internal fun `BUG - Terraforming Deal pays for the reversed TR in a fixed gain`() {
    startPaymentDefectGame(deal = true)
    kim.completeOperation {
          doTasks(
              "PlayProject<Class<$MagneticFieldGeneratorsPromo>>",
              "Ok", /* No Steel payment. */
          )
          doTask("MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_2_4>")
          autoExecNow(EAGER)
        }
        .expect("2 TerraformRating, -16 MC")
  }

  @Ignore // Reds charges before Preservation Program removes the TR.
  @Test
  internal fun `Reds charges nothing for the prevented TR`() {
    startPaymentDefectGame(
        deal = false,
        rulingParty = "Reds",
        firstRole = "PartyFollower",
        secondRole = "PartyLeader",
        neutralRole = "PartyLeader",
    )
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating, -14 MC")
  }

  @Test
  internal fun `BUG - Reds charges for the prevented TR`() {
    startPaymentDefectGame(
        deal = false,
        rulingParty = "Reds",
        firstRole = "PartyFollower",
        secondRole = "PartyLeader",
        neutralRole = "PartyLeader",
    )
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating, -17 MC")
  }

  @Ignore // Both payments respond before Preservation Program removes the TR.
  @Test
  internal fun `Reds and Terraforming Deal both count only retained TR`() {
    startPaymentDefectGame(
        deal = true,
        rulingParty = "Reds",
        firstRole = "PartyFollower",
        secondRole = "PartyLeader",
        neutralRole = "PartyLeader",
    )
    kim.completeOperation {
          doTasks(
              "PlayProject<Class<$MagneticFieldGeneratorsPromo>>",
              "Ok", /* No Steel payment. */
          )
          doTask("MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_2_4>")
          autoExecNow(EAGER)
        }
        .expect("2 TerraformRating, -24 MC")
  }

  @Test
  internal fun `BUG - Reds and Terraforming Deal both count reversed TR`() {
    startPaymentDefectGame(
        deal = true,
        rulingParty = "Reds",
        firstRole = "PartyFollower",
        secondRole = "PartyLeader",
        neutralRole = "PartyLeader",
    )
    kim.completeOperation {
          doTasks(
              "PlayProject<Class<$MagneticFieldGeneratorsPromo>>",
              "Ok", /* No Steel payment. */
          )
          doTask("MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_2_4>")
          autoExecNow(EAGER)
        }
        .expect("2 TerraformRating, -25 MC")
  }

  @Ignore // Reds requires payment for both TR before the first is removed.
  @Test
  internal fun `Reds permits an affordable two-step gain when the first TR is prevented`() {
    prepareAffordableTwoStepGain()
    kim.doTasks("PlayProject<Class<$ReleaseOfInertGases>>").expect("TerraformRating, -17 MC")
  }

  @Test
  internal fun `BUG - Reds rejects an affordable two-step gain when the first TR is prevented`() {
    prepareAffordableTwoStepGain()
    shouldThrow<LimitsException> {
      kim.doTasks("PlayProject<Class<$ReleaseOfInertGases>>")
    }
    kim.assertCounts(
        17 to "MC",
        1 to "ProjectCard",
        0 to "PlayedEvent<Class<$ReleaseOfInertGases>>",
    )
  }

  private fun prepareAffordableTwoStepGain() {
    startPaymentDefectGame(
        deal = false,
        rulingParty = "Reds",
        firstRole = "PartyFollower",
        secondRole = "PartyLeader",
        neutralRole = "PartyLeader",
    )
    // Ordinary purchases leave exactly 17 MC and a card, without using the generation's TR skip.
    kim.doTasks("UseStandardProject<PowerPlantProject>")
    kim.doTasks("UseStandardProject<PowerPlantProject>")
    kim.doTasks("UseStandardProject<PowerPlantProject>")
    kim.doTasks("UseStandardProject<PowerPlantProject>")
    kim.doTasks("UseStandardProject<PowerPlantProject>")
    kim.doTasks("UseStandardProject<SellPatentsProject>", "9 MC FROM ProjectCard")
  }

  // Terraforming Deal outside Action:
  // https://boardgamegeek.com/thread/3343166/article/44660753#44660753
  @Ignore // The earlier Solar-phase gain does not prevent the incorrect payout.
  @Test
  internal fun `Terraforming Deal ignores prevented TR after a chairman award`() {
    startPaymentDefectGame(deal = true, rulingParty = "Scientists")
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating, -14 MC")
  }

  @Test
  internal fun `BUG - Terraforming Deal pays for prevented TR after a chairman award`() {
    startPaymentDefectGame(deal = true, rulingParty = "Scientists")
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("0 TerraformRating, -12 MC")
  }

  private fun startPaymentDefectGame(
      deal: Boolean,
      rulingParty: String? = null,
      firstRole: String = "PartyLeader",
      secondRole: String = "PartyFollower",
      neutralRole: String = "PartyFollower",
  ) {
    val politics = if (rulingParty == null) "" else ", TurmoilExpansion"
    newProgramGame(extraOptions = "TerraformingDeal, Unsafe$politics")
    playPreludes(PreservationProgram, if (deal) TerraformingDeal else PowerGeneration)
    if (rulingParty != null) {
      kim.completeOperation {
        doTask("LobbyAction")
        doTask("$firstRole<$rulingParty> FROM LobbyDelegate")
      }
      kim.completeOperation {
        doTask("PaidLobbyAction")

        doTask("$secondRole<$rulingParty>")
      }
    } else {
      kim.doTasks(
          "PlayProject<Class<$GiantSpaceMirror>>",
      )
      kim.doTask("0 StandardAction") // Decline the second action.
    }
    stan.doTask("Pass")
    if (deal && rulingParty != null)
        kim.doTasks(
            "PlayProject<Class<$GiantSpaceMirror>>",
        )
    passGeneration(
        nextEvent = if (rulingParty == null) null else "ExploreFirstDirective",
        neutralPlacements = listOf("PartyLeader<Greens, Neutral>", "$neutralRole<Reds, Neutral>"),
    )
    stan.doTask("Pass")
  }
}
