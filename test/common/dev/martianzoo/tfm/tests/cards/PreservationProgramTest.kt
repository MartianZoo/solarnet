package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.testsupport.count
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class PreservationProgramTest : TfmGameplayTest() {
  @Test
  fun `Prelude benefit leaves the Action phase skip available`() {
    newProgramGame()
    playPreludes()
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("TerraformRating")
    }
  }

  @Test
  fun `Earlier Prelude TR leaves the Action phase skip available`() {
    newProgramGame()
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>",
          "$UnmiContractor FROM PreludeCard",
          "3 TerraformRating",
          "ProjectCard",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
          "$PreservationProgram FROM PreludeCard",
          "5 TerraformRating",
      )
    }
    playStansPreludes()
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
    }
  }

  @Test
  fun `Prevented TR gain renews each generation rather than each turn`() {
    newProgramGame()
    playPreludes()
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
    }
    stan.turn { doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard") }
    kim.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    stan.doTask("Pass")
    kim.doTask("Pass")
    kim.doTask("26 MC")
    stan.doTasks("21 MC", "2 Energy", "Steel", "Titanium")
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
              "PROD[Heat]",
          )
          .expect("0 TerraformRating")
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("TerraformRating")
    }
  }

  @Test
  fun `Earlier Prelude TR does not waive the skip when Valley Trust plays the Program`() {
    newProgramGame(ValleyTrust)
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>",
          "$UnmiContractor FROM PreludeCard",
          "3 TerraformRating",
          "ProjectCard",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Donation>>",
          "$Donation FROM PreludeCard",
          "21 MC",
      )
    }
    playStansPreludes()
    kim.turn {
      doTasks(
              "DoRequiredActions",
              "3 PreludeCard<Selecting>",
              "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
              "$PreservationProgram FROM PreludeCard<Selecting>",
              "5 TerraformRating",
          )
          .expect("4 TerraformRating")
    }
  }

  @Test
  fun `Playing the Program as a first action skips part of its own benefit`() {
    newProgramGame(ValleyTrust)
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$PowerGeneration>>",
          "$PowerGeneration FROM PreludeCard",
          "PROD[3 Energy]",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Donation>>",
          "$Donation FROM PreludeCard",
          "21 MC",
      )
    }
    playStansPreludes()
    kim.turn {
      doTasks(
              "DoRequiredActions",
              "3 PreludeCard<Selecting>",
              "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
              "$PreservationProgram FROM PreludeCard<Selecting>",
              "5 TerraformRating",
          )
          .expect("4 TerraformRating")
    }
  }

  @Test
  fun `Earlier TR in the same turn prevents a skip when Board of Directors plays the Program`() {
    newTestGame("$PreservationProgram, $BoardOfDirectors, prelude:cards, -QuickStartVariant", 2)
    kim.exMachina("$BoardOfDirectors, 4 Director<$BoardOfDirectors>, 50 MC")
    kim.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
      )
      doTasks(
              "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
              "$PreservationProgram FROM PreludeCard",
              "5 TerraformRating",
          )
          .expect("5 TerraformRating")
    }
  }

  @Test
  fun `Earlier non-TR action leaves the skip available when the Program is played second`() {
    newTestGame("$PreservationProgram, $BoardOfDirectors, prelude:cards, -QuickStartVariant", 2)
    kim.exMachina("$BoardOfDirectors, 4 Director<$BoardOfDirectors>, 50 MC")
    kim.turn {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
      )
      doTasks(
              "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
              "$PreservationProgram FROM PreludeCard",
              "5 TerraformRating",
          )
          .expect("4 TerraformRating")
    }
  }

  @Test
  fun `UNMI cannot use a prevented gain to satisfy its requirement`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTask("25 MC")
    stan.doTasks("21 MC", "2 Energy", "Steel", "Titanium")
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
    }
    shouldThrow<RequirementException> {
      kim.completeOperation {
        doTasks(
            *cardActionTasks("$UnitedNationsMarsInitiative"),
            "3 Pay<> FROM MC",
            "TerraformRating",
        )
      }
    }
    kim.count("TerraformRating") shouldBe 25
  }

  @Test
  fun `UNMI can act after an actual gain following a prevented gain`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTask("25 MC")
    stan.doTasks("21 MC", "2 Energy", "Steel", "Titanium")
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
              *cardActionTasks("$UnitedNationsMarsInitiative"),
              "3 Pay<> FROM MC",
              "TerraformRating",
          )
          .expect("TerraformRating")
    }
  }

  @Test
  fun `Prelude benefit qualifies UNMI before the first Action phase gain`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    kim.turn {
      doTasks(
              *cardActionTasks("$UnitedNationsMarsInitiative"),
              "3 Pay<> FROM MC",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
    }
  }

  @Test
  fun `Prelude benefit still qualifies UNMI after the first Action phase gain is prevented`() {
    newProgramGame(UnitedNationsMarsInitiative)
    playPreludes()
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
      doTasks(
              *cardActionTasks("$UnitedNationsMarsInitiative"),
              "3 Pay<> FROM MC",
              "TerraformRating",
          )
          .expect("TerraformRating")
    }
  }

  @Test
  fun `Prelude benefit still disqualifies Pristar after a prevented Action phase gain`() {
    newProgramGame(Pristar, extraOptions = "$Pristar")
    playPreludes()
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
    }
    stan.doTask("Pass")
    kim.doTask("Pass")
    kim.doTask("23 MC")
    stan.doTasks("21 MC", "2 Energy", "Steel", "Titanium")
    admin.count("SolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 0
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `Pristar receives its production bonus after only a prevented gain`() {
    newProgramGame(Pristar, extraOptions = "$Pristar")
    playPreludes()
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTask("23 MC")
    stan.doTasks("21 MC", "2 Energy", "Steel", "Titanium")
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
    }
    kim.doTask("Pass")
    kim.doTasks("29 MC", "Preservation<$Pristar>")
    stan.doTasks("2 Heat FROM Energy", "21 MC", "2 Energy", "Steel", "Titanium")
    admin.count("SolarPhase") shouldBe 1
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `Pristar loses its production bonus after an actual gain`() {
    newProgramGame(Pristar, extraOptions = "$Pristar")
    playPreludes()
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTask("23 MC")
    stan.doTasks("21 MC", "2 Energy", "Steel", "Titanium")
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    kim.doTask("Pass")
    kim.doTask("24 MC")
    stan.doTasks("2 Heat FROM Energy", "21 MC", "2 Energy", "Steel", "Titanium")
    admin.count("SolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 0
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `A fixed three-TR gain loses only a single step`() {
    newTestGame("$PreservationProgram, -QuickStartVariant", 2)
    kim.exMachina("$PreservationProgram")
    kim.exMachina("PROD[4 Energy]")
    kim.turn {
      doTasks(
              *playCardTasks("$MagneticFieldGeneratorsPromo"),
              "22 Pay<> FROM MC",
              "$MagneticFieldGeneratorsPromo FROM ProjectCard",
              "PROD[-4 Energy]",
              "PROD[2 Plant]",
              "3 TerraformRating",
              "MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_2_4>",
          )
          .expect("2 TerraformRating")
    }
  }

  @Test
  fun `Terraforming Ganymede skips a single computed TR step`() {
    newTestGame("$PreservationProgram, $GalileanMining, -QuickStartVariant", 2, SaturnSystems)
    kim.exMachina("$PreservationProgram, $GalileanMining, 50 MC, 4 Titanium")
    kim.turn {
      doTasks(
              *playCardTasks("$TerraformingGanymede"),
              "Ok",
              "33 Pay<> FROM MC", // No titanium payment.
              "$TerraformingGanymede FROM ProjectCard",
              "PROD[MC]",
              "3 TerraformRating",
          )
          .expect("2 TerraformRating")
    }
  }

  @Test
  fun `Terraforming Ganymede awards all computed TR after the skip is used`() {
    newTestGame("$PreservationProgram, $GalileanMining, -QuickStartVariant", 2, SaturnSystems)
    kim.exMachina("$PreservationProgram, $GalileanMining, 50 MC, 4 Titanium")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
      doTasks(
              *playCardTasks("$TerraformingGanymede"),
              "4 Pay<Class<Titanium>> FROM Titanium",
              "21 Pay<> FROM MC",
              "$TerraformingGanymede FROM ProjectCard",
              "PROD[MC]",
              "3 TerraformRating",
          )
          .expect("3 TerraformRating")
    }
  }

  @Test
  fun `Opponents gain TR without consuming the owners skip`() {
    newProgramGame()
    playPreludes()
    kim.exMachina("50 MC") // Enough for both of Kim's projects without extra generations.
    kim.turn { doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard") }
    stan.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
    }
    stan.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
    }
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("TerraformRating")
    }
  }

  @Test
  fun `Double Down copies the Prelude benefit without adding another skip`() {
    newProgramGame()
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
          "$PreservationProgram FROM PreludeCard",
          "5 TerraformRating",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
          "$DoubleDown FROM PreludeCard",
          "CopyPrelude<$PreservationProgram>",
          "5 TerraformRating",
      )
    }
    playStansPreludes()
    kim.turn {
      doTasks(
              *playCardTasks("$Comet"),
              "21 Pay<> FROM MC",
              "$Comet FROM ProjectCard",
              "TemperatureStep",
              "TerraformRating",
              "OceanTile<Tharsis_1_2>",
              "TerraformRating",
              "2 Steel",
              "Ok",
          ) // No plant attack.
          .expect("TerraformRating")
    }
  }

  @Test
  fun `Double Down copies four TR when the Action phase skip is unused`() {
    newProgramGame(ValleyTrust)
    playPreludes()
    kim.turn {
      doTasks(
              "DoRequiredActions",
              "3 PreludeCard<Selecting>",
              "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
              "$DoubleDown FROM PreludeCard<Selecting>",
              "CopyPrelude<$PreservationProgram>",
              "5 TerraformRating",
          )
          .expect("4 TerraformRating")
    }
  }

  @Test
  fun `Double Down copies five TR after the Action phase skip is used`() {
    newTestGame("$PreservationProgram, $BoardOfDirectors, prelude:cards, -QuickStartVariant", 2)
    kim.exMachina("$BoardOfDirectors, 4 Director<$BoardOfDirectors>, 50 MC")
    kim.exMachina("$PreservationProgram")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
      )
      doTasks(
              "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
              "$DoubleDown FROM PreludeCard",
              "CopyPrelude<$PreservationProgram>",
              "5 TerraformRating",
          )
          .expect("5 TerraformRating")
    }
  }

  @Test
  fun `Pharmacy Union consumes a disease even when its TR gain is prevented`() {
    startPharmacyGame()
    kim.turn {
      doTasks(
              *playCardTasks("$PhysicsComplex"),
              "12 Pay<> FROM MC",
              "$PhysicsComplex FROM ProjectCard",
              "TerraformRating FROM Disease<$PharmacyUnion>",
          )
          .expect("0 TerraformRating")
    }
  }

  @Test
  fun `World Government Advisor does not consume the skip`() {
    newTestGame("$PreservationProgram, $WorldGovernmentAdvisor, -QuickStartVariant", 2)
    kim.exMachina("$PreservationProgram")
    kim.exMachina("$WorldGovernmentAdvisor")
    kim.turn {
      doTasks(*cardActionTasks("$WorldGovernmentAdvisor"), "TemperatureStep BY Admin")
          .expect("TemperatureStep, 0 TerraformRating")
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
    }
  }

  @Test
  fun `Solar chairman TR does not consume the following generations skip`() {
    newTestGame(
        "$PreservationProgram, TurmoilExpansion, Unsafe, -QuickStartVariant",
        2,
        firstGlobalEvents =
            listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ExploreFirstDirective"),
    )
    kim.exMachina("$PreservationProgram, PartyLeader<Scientists>, PartyFollower<Scientists>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    expectChanges("0 TerraformRating<Kim>") { kim.doTask("VenusStep! BY Admin") }
    kim.count("Chairman") shouldBe 1 // Revision and the chairman award cancel.
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
    }
  }

  @Test
  fun `A gain followed by a loss still prevents a skip when the Program is acquired`() {
    newTestGame(
        "$PreservationProgram, $BoardOfDirectors, $Pristar, prelude:cards, -QuickStartVariant",
        2,
    )
    kim.exMachina("$BoardOfDirectors, 4 Director<$BoardOfDirectors>, 50 MC")
    kim.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    stan.doTask("Pass")
    kim.turn {
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Merger>>",
          "$Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
          "PlayCard<Class<CorporationCard>, Class<$Pristar>>",
          "$Pristar FROM CorporationCard<Selecting>",
          "53 MC",
          "-2 TerraformRating",
          "-42 MC",
      )
      doTasks(
          *playCardTasks("$ProjectInspection"),
          "$ProjectInspection FROM ProjectCard",
          "UseAction<$BoardOfDirectors, Action1>",
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
      )
      doTasks(
              "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
              "$PreservationProgram FROM PreludeCard",
              "5 TerraformRating",
          )
          .expect("5 TerraformRating")
    }
  }

  @Test
  fun `A TR loss does not restore a consumed skip`() {
    newTestGame(
        "$PreservationProgram, $BoardOfDirectors, $Pristar, prelude:cards, -QuickStartVariant",
        2,
    )
    kim.exMachina("$BoardOfDirectors, 4 Director<$BoardOfDirectors>, 50 MC")
    kim.exMachina("$PreservationProgram")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    stan.doTask("Pass")
    kim.turn {
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Merger>>",
          "$Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
          "PlayCard<Class<CorporationCard>, Class<$Pristar>>",
          "$Pristar FROM CorporationCard<Selecting>",
          "53 MC",
          "-2 TerraformRating",
          "-42 MC",
      )
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
              "PROD[Heat]",
          )
          .expect("TerraformRating")
    }
  }

  @Test
  fun `A capped global parameter does not consume the skip`() {
    newTestGame("$PreservationProgram, -QuickStartVariant", 2)
    kim.exMachina("$PreservationProgram")
    agents.correct("14 OxygenStep")
    kim.exMachina("8 Plant")
    kim.turn {
      doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Tharsis_2_4>",
              "0 OxygenStep",
          )
          .expect("0 OxygenStep, 0 TerraformRating")
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating")
    }
  }

  @Test
  fun `Linked oxygen temperature and ocean gains skip a single TR`() {
    newTestGame("$PreservationProgram, -QuickStartVariant", 2)
    kim.exMachina("$PreservationProgram")
    agents.correct("7 OxygenStep, 14 TemperatureStep")
    kim.exMachina("8 Plant")
    kim.turn {
      doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Tharsis_2_4>",
              "OxygenStep",
              "TerraformRating",
              "TemperatureStep",
              "TerraformRating",
              "OceanTile<Tharsis_1_2>",
              "2 Steel",
              "TerraformRating",
          )
          .expect("2 TerraformRating")
    }
  }

  @Test
  fun `Pharmacy Union flip loses part of its TR and remains a played event`() {
    startPharmacyGame()
    kim.exMachina(
        "-2 Disease<$PharmacyUnion>"
    ) // Prepare the no-disease branch; its earlier consumption is incidental.
    kim.turn {
      doTasks(
              *playCardTasks("$PhysicsComplex"),
              "12 Pay<> FROM MC",
              "$PhysicsComplex FROM ProjectCard",
              "PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion",
              "3 TerraformRating",
          )
          .expect("2 TerraformRating")
    }
    kim.count("$PharmacyUnion") shouldBe 0
    kim.count("PlayedEvent<Class<$PharmacyUnion>>") shouldBe 1
  }

  @Test
  fun `Earlier TR in a prior turn prevents a skip when the Program is acquired`() {
    newTestGame("$PreservationProgram, $BoardOfDirectors, prelude:cards, -QuickStartVariant", 2)
    kim.exMachina("$BoardOfDirectors, 4 Director<$BoardOfDirectors>, 50 MC")
    kim.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    stan.doTask("Pass")
    kim.turn {
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
      )
      doTasks(
              "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
              "$PreservationProgram FROM PreludeCard",
              "5 TerraformRating",
          )
          .expect("5 TerraformRating")
    }
  }

  @Test
  fun `Reds ruling bonus outside the Action phase leaves the next generations skip available`() {
    newTestGame(
        "$PreservationProgram, TurmoilExpansion, Unsafe, -QuickStartVariant",
        2,
        firstGlobalEvents =
            listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ExploreFirstDirective"),
    )
    kim.exMachina(
        "$PreservationProgram, PartyLeader<Reds> FROM PartyLeader<Reds, Neutral>, 2 PartyFollower<Reds>"
    )
    stan.exMachina("5 TerraformRating")
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTask("20 MC")
    stan.doTask("25 MC")
    expectChanges("TerraformRating<Kim>") { kim.doTask("VenusStep! BY Admin") }
    // Kim receives Reds' bonus and the chairmanship after revision.
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
              "-3 MC",
          )
          .expect("0 TerraformRating")
    }
  }

  @Test
  fun `Venus threshold awards its additional TR while skipping a single step`() {
    newTestGame("$PreservationProgram, -QuickStartVariant", 2)
    kim.exMachina("$PreservationProgram")
    agents.correct("7 VenusStep")
    kim.turn {
      doTasks(
              *standardProjectTasks("AirScrappingProject"),
              "15 Pay<> FROM MC",
              "VenusStep",
              "2 TerraformRating",
          )
          .expect("TerraformRating")
    }
  }

  @Test
  fun `Solo Buffer Gas uses the skip while starting TR remains fourteen`() {
    newTestGame("PreludeExpansion, $PreservationProgram, Tr63SoloObjective, -QuickStartVariant", 1)
    kim.count("TerraformRating") shouldBe 14
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
          "$PreservationProgram FROM PreludeCard",
          "5 TerraformRating",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Donation>>",
          "$Donation FROM PreludeCard",
          "21 MC",
      )
    }
    kim.turn {
      doTasks(*standardProjectTasks("BufferGasProject"), "16 Pay<> FROM MC", "TerraformRating")
          .expect("0 TerraformRating")
      doTasks(*standardProjectTasks("BufferGasProject"), "16 Pay<> FROM MC", "TerraformRating")
          .expect("TerraformRating")
    }
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
  fun `Terraforming Deal pays only for the retained TR in a fixed gain`() {
    startPaymentDefectGame(deal = true)
    kim.turn {
      doTasks(
              *playCardTasks("$MagneticFieldGeneratorsPromo"),
              "22 Pay<> FROM MC",
              "$MagneticFieldGeneratorsPromo FROM ProjectCard",
              "PROD[-4 Energy]",
              "PROD[2 Plant]",
              "3 TerraformRating",
              "MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_2_4>",
              "4 MC",
          )
          .expect("2 TerraformRating, -18 MC")
    }
  }

  @Test
  fun `BUG - Terraforming Deal pays for the reversed TR in a fixed gain`() {
    startPaymentDefectGame(deal = true)
    kim.turn {
      doTasks(
              *playCardTasks("$MagneticFieldGeneratorsPromo"),
              "22 Pay<> FROM MC",
              "$MagneticFieldGeneratorsPromo FROM ProjectCard",
              "PROD[-4 Energy]",
              "PROD[2 Plant]",
              "3 TerraformRating",
              "MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_2_4>",
              "6 MC",
          )
          .expect("2 TerraformRating, -16 MC")
    }
  }

  @Ignore // Reds charges before Preservation Program removes the TR.
  @Test
  fun `Reds charges nothing for the prevented TR`() {
    startPaymentDefectGame(deal = false, rulingParty = "Reds")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating, -14 MC")
    }
  }

  @Test
  fun `BUG - Reds charges for the prevented TR`() {
    startPaymentDefectGame(deal = false, rulingParty = "Reds")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
              "-3 MC",
          )
          .expect("0 TerraformRating, -17 MC")
    }
  }

  @Ignore // Both payments respond before Preservation Program removes the TR.
  @Test
  fun `Reds and Terraforming Deal both count only retained TR`() {
    startPaymentDefectGame(deal = true, rulingParty = "Reds")
    kim.turn {
      doTasks(
              *playCardTasks("$MagneticFieldGeneratorsPromo"),
              "22 Pay<> FROM MC",
              "$MagneticFieldGeneratorsPromo FROM ProjectCard",
              "PROD[-4 Energy]",
              "PROD[2 Plant]",
              "3 TerraformRating",
              "MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_2_4>",
              "4 MC",
              "-6 MC",
          )
          .expect("2 TerraformRating, -24 MC")
    }
  }

  @Test
  fun `BUG - Reds and Terraforming Deal both count reversed TR`() {
    startPaymentDefectGame(deal = true, rulingParty = "Reds")
    kim.turn {
      doTasks(
              *playCardTasks("$MagneticFieldGeneratorsPromo"),
              "22 Pay<> FROM MC",
              "$MagneticFieldGeneratorsPromo FROM ProjectCard",
              "PROD[-4 Energy]",
              "PROD[2 Plant]",
              "3 TerraformRating",
              "MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_2_4>",
              "6 MC",
              "-9 MC",
          )
          .expect("2 TerraformRating, -25 MC")
    }
  }

  @Ignore // Reds requires payment for both TR before the first is removed.
  @Test
  fun `Reds permits an affordable two-step gain when the first TR is prevented`() {
    prepareAffordableTwoStepGain()
    kim.turn {
      doTasks(
              *playCardTasks("$ReleaseOfInertGases"),
              "14 Pay<> FROM MC",
              "$ReleaseOfInertGases FROM ProjectCard",
              "2 TerraformRating",
              "-3 MC",
          )
          .expect("TerraformRating, -17 MC")
    }
  }

  @Test
  fun `BUG - Reds rejects an affordable two-step gain when the first TR is prevented`() {
    prepareAffordableTwoStepGain()
    shouldThrow<LimitsException> {
      kim.completeOperation {
        doTasks(
            *playCardTasks("$ReleaseOfInertGases"),
            "14 Pay<> FROM MC",
            "$ReleaseOfInertGases FROM ProjectCard",
            "2 TerraformRating",
            "-6 MC",
        )
      }
    }
    kim.assertCounts(
        17 to "MC",
        1 to "ProjectCard",
        0 to "PlayedEvent<Class<$ReleaseOfInertGases>>",
    )
  }

  // Terraforming Deal outside Action:
  // https://boardgamegeek.com/thread/3343166/article/44660753#44660753
  @Ignore // The earlier Solar-phase gain does not prevent the incorrect payout.
  @Test
  fun `Terraforming Deal ignores prevented TR after a chairman award`() {
    startPaymentDefectGame(deal = true, rulingParty = "Scientists")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 TerraformRating, -14 MC")
    }
  }

  @Test
  fun `BUG - Terraforming Deal pays for prevented TR after a chairman award`() {
    startPaymentDefectGame(deal = true, rulingParty = "Scientists")
    kim.turn {
      doTasks(
              *standardProjectTasks("AsteroidProject"),
              "14 Pay<> FROM MC",
              "TemperatureStep",
              "TerraformRating",
              "2 MC",
          )
          .expect("0 TerraformRating, -12 MC")
    }
  }

  private fun newProgramGame(
      kimCorporation: ClassName? = null,
      extraOptions: String = "",
  ) {
    newTestGame(
        "PreludeExpansion, $PreservationProgram, -QuickStartVariant, $extraOptions",
        2,
        kimCorporation,
    )
  }

  private fun playPreludes() {
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
          "$PreservationProgram FROM PreludeCard",
          "5 TerraformRating",
      )

      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Donation>>",
          "$Donation FROM PreludeCard",
          "21 MC",
      )
    }
    playStansPreludes()
  }

  private fun playStansPreludes() {
    stan.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
          "$Supplier FROM PreludeCard",
          "PROD[2 Energy]",
          "4 Steel",
      )

      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
          "$MetalsCompany FROM PreludeCard",
          "PROD[MC]",
          "PROD[Steel]",
          "PROD[Titanium]",
      )
    }
    admin.count("ActionPhase") shouldBe 1
  }

  private fun startPharmacyGame() {
    // The corporation opening is incidental; its science reaction is exercised by the test.
    newTestGame("$PreservationProgram, -QuickStartVariant", 2, PharmacyUnion)
    kim.exMachina("$PreservationProgram")
  }

  private fun startPaymentDefectGame(deal: Boolean, rulingParty: String? = null) {
    val politics = if (rulingParty == null) "" else "TurmoilExpansion"
    newTestGame(
        "$PreservationProgram, $TerraformingDeal, Unsafe, -QuickStartVariant, $politics",
        2,
        firstGlobalEvents =
            if (rulingParty == null) emptyList()
            else listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ExploreFirstDirective"),
    )
    kim.exMachina("$PreservationProgram, PROD[4 Energy], 50 MC")
    if (deal) kim.exMachina("$TerraformingDeal")
    if (rulingParty != null) {
      // The delegates are preparation; the real election must award the chairman TR outside Action.
      if (rulingParty == "Reds")
          kim.exMachina("PartyLeader<Reds> FROM PartyLeader<Reds, Neutral>, 2 PartyFollower<Reds>")
      else kim.exMachina("PartyLeader<Scientists>, PartyFollower<Scientists>")
      kim.doTask("Pass")
      stan.doTask("Pass")
      kim.doTasks("20 MC", "4 Energy")
      stan.doTask("20 MC")
      kim.doTask("VenusStep! BY Admin")
      if (deal) kim.doTask(if (rulingParty == "Reds") "4 MC" else "2 MC")
      kim.count("Chairman") shouldBe 1
      admin.count("Ruling<$rulingParty>") shouldBe 1
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
      stan.doTask("Pass")
    }
  }

  private fun prepareAffordableTwoStepGain() {
    startPaymentDefectGame(deal = false, rulingParty = "Reds")
    // This affordability test starts with exactly 17 MC and a single project to play.
    kim.exMachina("-95 MC, -9 ProjectCard<Hand>")
    kim.assertCounts(17 to "MC", 1 to "ProjectCard<Hand>")
  }
}
