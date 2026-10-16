package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CrashSiteCleanupTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "-promos:cards, CrashSiteCleanup")

  @Test
  internal fun `Can be played after removing an opponent's plant`() {
    stan.exMachina("Plant")
    kim.inTurn {
      doTasks("PlayProject<Class<AsteroidCard>>", "14 Pay<> FROM MC")
      doTasks(
          "AsteroidCard FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTasks("2 Titanium", "TerraformRating", "-Plant<Stan>")
    }

    kim.inTurn {
      doTasks(
          "PlayProject<Class<CrashSiteCleanup>>",
          "4 Pay<> FROM MC",
          "CrashSiteCleanup FROM ProjectCard",
          "Titanium",
      )
    }
  }

  @Test
  internal fun `Cannot be played after removing the owner's plant`() {
    kim.exMachina("Plant")
    kim.inTurn {
      doTasks("PlayProject<Class<AsteroidCard>>", "14 Pay<> FROM MC")
      doTasks(
          "AsteroidCard FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTasks("2 Titanium", "TerraformRating", "-Plant")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PlayProject<Class<CrashSiteCleanup>>")
      }
    }
  }

  @Test
  internal fun `Cannot be played after an opponent removes their own plant`() {
    stan.exMachina("Plant")
    stan.inTurn {
      doTasks("PlayProject<Class<AsteroidCard>>", "14 Pay<> FROM MC")
      doTasks(
          "AsteroidCard FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTasks("2 Titanium", "TerraformRating", "-Plant")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PlayProject<Class<CrashSiteCleanup>>")
      }
    }
  }

  @Test
  internal fun `Cannot be played after a plant removal in the previous generation`() {
    stan.exMachina("Plant")
    kim.inTurn {
      doTasks("PlayProject<Class<AsteroidCard>>", "14 Pay<> FROM MC")
      doTasks(
          "AsteroidCard FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTasks("2 Titanium", "TerraformRating", "-Plant<Stan>")
    }
    advanceUntilActionPhase()

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PlayProject<Class<CrashSiteCleanup>>")
      }
    }
  }

  @Test
  internal fun `Cannot be played by a player who did not remove the plant`() {
    stan.exMachina("Plant")
    rob.inTurn {
      doTasks("PlayProject<Class<AsteroidCard>>", "14 Pay<> FROM MC")
      doTasks(
          "AsteroidCard FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTasks("2 Titanium", "TerraformRating", "-Plant<Stan>")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PlayProject<Class<CrashSiteCleanup>>")
      }
    }
  }

  @Test
  internal fun `Cannot be played after declining an asteroid's plant attack`() {
    stan.exMachina("Plant")
    kim.inTurn {
          doTasks("PlayProject<Class<AsteroidCard>>", "14 Pay<> FROM MC")
          doTasks(
              "AsteroidCard FROM ProjectCard",
          )
          doTask("TemperatureStep")
          doTasks("2 Titanium", "TerraformRating")
          doTask("-0 Plant<Anyone>")
        }
        .expect("0 Plant<Stan>")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PlayProject<Class<CrashSiteCleanup>>")
      }
    }
  }
}
