package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class BoomTownTest : TfmSandboxTest() {
  @Test
  internal fun `Requires a metal bonus and retains the normal city restriction`() {
    newTestGame(addOptions = "PreludeExpansion")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks("PlayCard<Class<PreludeCard>, Class<BoomTown>>", "BoomTown FROM PreludeCard")
        doTask("PROD[2 Titanium]")
        doTask("CityTile<Tharsis_4_2>")
      }
    }
    kim.exMachina("NormalCityTile<Tharsis_2_1>")
    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks("PlayCard<Class<PreludeCard>, Class<BoomTown>>", "BoomTown FROM PreludeCard")
        doTask("PROD[2 Titanium]")
        doTask("CityTile<Tharsis_1_1>")
      }
    }
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoomTown>>",
          "BoomTown FROM PreludeCard",
          "PROD[2 Titanium]",
          "CityTile<Tharsis_8_9>",
          "Titanium",
      )
    }
  }

  @Test
  internal fun `Reduces titanium value for its owner only`() {
    newTestGame()
    kim.exMachina("$BoomTown, 3 Titanium")
    stan.exMachina("Titanium")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<SmallAsteroid>>",
          "-3 Titanium",
          "-4 MC",
          "SmallAsteroid FROM ProjectCard",
      )
      doTask("TemperatureStep")
      // Resolve the optional attack: no unprotected plant target is available.
      selectTask("-2? Plant<Anyone>")
      doTask("TerraformRating")
      doTask("PlayedEvent<Class<SmallAsteroid>> FROM SmallAsteroid")
    }
    stan.inTurn {
      doTasks(
          "PlayProject<Class<SpaceStation>>",
          "-1 Titanium",
          "-7 MC",
          "SpaceStation FROM ProjectCard",
      )
    }
  }

  @Test
  internal fun `Double Down copies placement and production without doubling the titanium penalty`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoomTown>>",
          "BoomTown FROM PreludeCard",
          "PROD[2 Titanium]",
          "CityTile<Tharsis_1_1>",
          "2 Steel",
      )
    }
    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
      doTasks("CopyPrelude<BoomTown>", "PROD[2 Titanium]", "CityTile<Tharsis_8_9>", "Titanium")
    }

    advanceUntilActionPhase()
    kim.setToExMachina(3, "Titanium")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<SmallAsteroid>>",
          "-3 Titanium",
          "-4 MC",
          "SmallAsteroid FROM ProjectCard",
      )
      doTask("TemperatureStep")
      // Resolve the optional attack: no unprotected plant target is available.
      selectTask("-2? Plant<Anyone>")
      doTask("TerraformRating")
      doTask("PlayedEvent<Class<SmallAsteroid>> FROM SmallAsteroid")
    }
  }

  @Test
  internal fun `Its penalty stacks with both alloys PhoboLog and Unity for card actions`() {
    newTestGame(addOptions = "TurmoilExpansion")
    // The correction changes government after ActionPhase began, so include its policy.
    kim.exMachina(
        "$BoomTown, $PhoboLog, $AdvancedAlloys, $MercurianAlloys, $IcyImpactors, Titanium, Ruling<Unity FROM Greens>, UnityPolicy"
    )

    // Titanium is worth 3 - 1 + 1 + 1 + 1 + 1 = 6 MC.
    kim.inTurn {
          doTasks("UseCardAction<IcyImpactors, Action1>", "-1 Titanium", "-4 MC")
          doTask("2 Asteroid<IcyImpactors>")
        }
        .expect("2 Asteroid<$IcyImpactors>")
  }
}
