package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class LawSuitTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "-promos:cards, LawSuit, SmallAsteroid")

  @Test
  internal fun `Can charge an opponent who lowered the owner's production`() {
    stan.inTurn {
      doTasks(
          "PlayProject<Class<EnergyTapping>>",
          "-3 MC",
          "EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Kim>]",
      )
    }

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LawSuit>>",
              "-2 MC",
              "LawSuit FROM ProjectCard",
          )
          doTask("3 MC<Kim FROM Stan>")
          doTask("PlayedEvent<Stan, Class<LawSuit>> FROM LawSuit")
          doTask("PlayedEvent<Class<$LawSuit>> FROM $LawSuit")
        }
        .expect("MC<Kim>, -3 MC<Stan>")
  }

  @Test
  internal fun `Transfers the event to the attacker selected for payment`() {
    stan.inTurn {
      doTasks(
          "PlayProject<Class<EnergyTapping>>",
          "-3 MC",
          "EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Kim>]",
      )
    }

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LawSuit>>",
              "-2 MC",
              "LawSuit FROM ProjectCard",
          )
          doTask("3 MC<Kim FROM Stan>")
          doTask("PlayedEvent<Stan, Class<LawSuit>> FROM LawSuit")
          doTask("PlayedEvent<Class<$LawSuit>> FROM $LawSuit")
        }
        .expect("0 PlayedEvent<Kim, Class<$LawSuit>>, PlayedEvent<Stan, Class<$LawSuit>>")
  }

  @Test
  internal fun `Can charge an opponent who removed the owner's resources`() {
    kim.exMachina("Plant")
    stan.inTurn {
      doTasks("PlayProject<Class<AsteroidCard>>", "-14 MC")
      doTasks(
          "Ok" /* No Titanium payment. */,
          "AsteroidCard FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTasks("2 Titanium", "TerraformRating", "-Plant<Kim>")
      doTask("PlayedEvent<Class<AsteroidCard>> FROM AsteroidCard")
    }

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LawSuit>>",
              "-2 MC",
              "LawSuit FROM ProjectCard",
          )
          doTask("3 MC<Kim FROM Stan>")
          doTask("PlayedEvent<Stan, Class<LawSuit>> FROM LawSuit")
          doTask("PlayedEvent<Class<$LawSuit>> FROM $LawSuit")
        }
        .expect("MC<Kim>, -3 MC<Stan>")
  }

  @Test
  internal fun `Can be played with only enough money for the card cost`() {
    stan.inTurn {
      doTasks(
          "PlayProject<Class<EnergyTapping>>",
          "-3 MC",
          "EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Kim>]",
      )
    }
    kim.setToExMachina(2, "MC")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LawSuit>>",
              "-2 MC",
              "LawSuit FROM ProjectCard",
          )
          doTask("3 MC<Kim FROM Stan>")
          doTask("PlayedEvent<Stan, Class<LawSuit>> FROM LawSuit")
          doTask("PlayedEvent<Class<$LawSuit>> FROM $LawSuit")
        }
        .expect("MC<Kim>, -3 MC<Stan>")
  }

  @Test
  internal fun `Triggers its player's Media Group`() {
    kim.exMachina("$MediaGroup")
    stan.inTurn {
      doTasks(
          "PlayProject<Class<EnergyTapping>>",
          "-3 MC",
          "EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Kim>]",
      )
    }

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LawSuit>>",
              "-2 MC",
              "LawSuit FROM ProjectCard",
              "3 MC",
          )
          doTask("3 MC<Kim FROM Stan>")
          doTask("PlayedEvent<Stan, Class<LawSuit>> FROM LawSuit")
          doTask("PlayedEvent<Class<$LawSuit>> FROM $LawSuit")
        }
        .expect("4 MC<Kim>, -3 MC<Stan>")
  }

  @Test
  internal fun `Does not trigger the charged player's Media Group`() {
    stan.exMachina("$MediaGroup")
    stan.inTurn {
      doTasks(
          "PlayProject<Class<EnergyTapping>>",
          "-3 MC",
          "EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Kim>]",
      )
    }

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LawSuit>>",
              "-2 MC",
              "LawSuit FROM ProjectCard",
          )
          doTask("3 MC<Kim FROM Stan>")
          doTask("PlayedEvent<Stan, Class<LawSuit>> FROM LawSuit")
          doTask("PlayedEvent<Class<$LawSuit>> FROM $LawSuit")
        }
        .expect("MC<Kim>, -3 MC<Stan>")
  }

  @Test
  internal fun `Cannot qualify by lowering the owner's own production`() {
    kim.setToExMachina(2, "PROD[Heat]")
    kim.inTurn {
      doTasks("PlayProject<Class<HeatTrappers>>", "-6 MC")
      doTasks(
          "Ok" /* No Steel payment. */,
          "HeatTrappers FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-2 Heat]",
      )
    }

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("PlayProject<Class<LawSuit>>")

        doTasks(
            "-2 MC",
            "LawSuit FROM ProjectCard",
        )
        doTask("3 MC<Kim FROM Stan>")
      }
    }
    kim.assertCounts(36 to "MC", 9 to "ProjectCard")
  }

  @Test
  internal fun `Cannot charge for an attack in the previous generation`() {
    stan.inTurn {
      doTasks(
          "PlayProject<Class<EnergyTapping>>",
          "-3 MC",
          "EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Kim>]",
      )
    }
    advanceUntilActionPhase()
    kim.setToExMachina(20, "MC")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("PlayProject<Class<LawSuit>>")

        doTasks(
            "-2 MC",
            "LawSuit FROM ProjectCard",
        )
        doTask("3 MC<Kim FROM Stan>")
      }
    }
    kim.assertCounts(20 to "MC", 10 to "ProjectCard")
  }

  @Test
  internal fun `Cannot charge when every attacker has less than three MC`() {
    kim.exMachina("2 Plant")
    stan.inTurn {
      doTasks("PlayProject<Class<AsteroidCard>>", "-14 MC")
      doTasks(
          "Ok" /* No Titanium payment. */,
          "AsteroidCard FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTasks("2 Titanium", "TerraformRating", "-Plant<Kim>")
      doTask("PlayedEvent<Class<AsteroidCard>> FROM AsteroidCard")
    }
    rob.inTurn {
      doTasks("PlayProject<Class<SmallAsteroid>>", "-10 MC")
      doTasks(
          "Ok" /* No Titanium payment. */,
          "SmallAsteroid FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTasks("TerraformRating", "-Plant<Kim>")
      doTask("PlayedEvent<Class<SmallAsteroid>> FROM SmallAsteroid")
    }
    stan.setToExMachina(2, "MC")
    rob.setToExMachina(2, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<LawSuit>>")

        doTasks(
            "-2 MC",
            "LawSuit FROM ProjectCard",
        )
        doTask("3 MC<Kim FROM Stan>")
      }
    }
    kim.assertCounts(42 to "MC", 10 to "ProjectCard")
    stan.assertCounts(2 to "MC")
    rob.assertCounts(2 to "MC")
  }

  @Test
  internal fun `Can choose among multiple attackers`() {
    kim.exMachina("2 Plant")
    stan.inTurn {
      doTasks("PlayProject<Class<AsteroidCard>>", "-14 MC")
      doTasks(
          "Ok" /* No Titanium payment. */,
          "AsteroidCard FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTasks("2 Titanium", "TerraformRating", "-Plant<Kim>")
      doTask("PlayedEvent<Class<AsteroidCard>> FROM AsteroidCard")
    }
    rob.inTurn {
      doTasks("PlayProject<Class<SmallAsteroid>>", "-10 MC")
      doTasks(
          "Ok" /* No Titanium payment. */,
          "SmallAsteroid FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTasks("TerraformRating", "-Plant<Kim>")
      doTask("PlayedEvent<Class<SmallAsteroid>> FROM SmallAsteroid")
    }

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LawSuit>>",
              "-2 MC",
              "LawSuit FROM ProjectCard",
          )
          doTask("3 MC<Kim FROM Stan>")
          doTask("PlayedEvent<Stan, Class<LawSuit>> FROM LawSuit")
          doTask("PlayedEvent<Class<$LawSuit>> FROM $LawSuit")
        }
        .expect("MC<Kim>, -3 MC<Stan>, 0 MC<Rob>")
  }

  @Test
  internal fun `Cannot charge a funded player who did not attack`() {
    stan.inTurn {
      doTasks(
          "PlayProject<Class<EnergyTapping>>",
          "-3 MC",
          "EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Kim>]",
      )
    }

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<LawSuit>>",
            "-2 MC",
            "LawSuit FROM ProjectCard",
        )
        // Select the linked transfer before testing an incompatible victim and card owner.
        selectOnlyTask()
        narrowTask("3 MC<Kim FROM Rob> THEN PlayedEvent<Rob, Class<LawSuit>> FROM LawSuit")
      }
    }
    kim.assertCounts(42 to "MC", 10 to "ProjectCard")
    stan.assertCounts(39 to "MC")
    rob.assertCounts(42 to "MC")
  }

  @Test
  internal fun `Costs the charged player a victory point`() {
    stan.inTurn {
      doTasks(
          "PlayProject<Class<EnergyTapping>>",
          "-3 MC",
          "EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Kim>]",
      )
    }
    kim.inTurn {
      doTasks(
          "PlayProject<Class<LawSuit>>",
          "-2 MC",
          "LawSuit FROM ProjectCard",
      )
      doTask("3 MC<Kim FROM Stan>")
      doTask("PlayedEvent<Stan, Class<LawSuit>> FROM LawSuit")
      doTask("PlayedEvent<Class<$LawSuit>> FROM $LawSuit")
    }

    // Energy Tapping and Law Suit each cost Stan a point.
    victoryPoints() shouldBe listOf(20, 18, 20)
  }
}
