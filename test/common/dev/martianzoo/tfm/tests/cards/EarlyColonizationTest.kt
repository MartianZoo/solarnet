package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EarlyColonizationTest : TfmSandboxTest() {
  @Test
  internal fun `Advances active tracks from their current positions and leaves inactive tiles alone`() {
    newTestGame("PreludeExpansion, $EarlyColonization, Luna, Ceres, Miranda, Titan, Enceladus")
    admin.setToExMachina(3, "ColonyProduction<Luna>")

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$EarlyColonization>>",
              "3 Energy",
          )
          // Expand the increase into separate track changes for each active colony.
          doTasks(
              "EACH @ColonyTile { 2! ColonyProduction<@ColonyTile> }",
              "Colony<Luna>",
              "PROD[2 MC]",
          )
        }
        .expect(
            "2 ColonyProduction<Luna>, 2 ColonyProduction<Ceres>, 0 ColonyProduction<Miranda>, " +
                "0 ColonyProduction<Titan>, 0 ColonyProduction<Enceladus>, Colony<Luna>, 3 Energy"
        )
  }

  @Test
  internal fun `Cannot be played when its owner already has a colony on every active tile`() {
    newTestGame("PreludeExpansion, $EarlyColonization, Luna, Ceres, Miranda, Titan, Enceladus")
    kim.exMachina("Colony<Luna>, Colony<Ceres>")

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$EarlyColonization>>",
            "3 Energy",
        )
        // Expand the increase into separate track changes for each active colony.
        doTask("EACH @ColonyTile { 2! ColonyProduction<@ColonyTile> }")
        // Resolve the colony offer to prove that no eligible active tile remains.
        selectTask("Colony<>")
      }
    }
  }

  @Test
  internal fun `Fizzles when an active track has room for only a single advance`() {
    fizzleAtPosition(5)
  }

  @Test
  internal fun `Fizzles when an active track is already full`() {
    fizzleAtPosition(6)
  }

  private fun fizzleAtPosition(position: Int) {
    newTestGame("PreludeExpansion, $EarlyColonization, $BoardOfDirectors, Luna, Ceres")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BoardOfDirectors>>",
          "4 Director<$BoardOfDirectors>",
      )
    }
    advanceUntilActionPhase()
    admin.setToExMachina(position, "ColonyProduction<Luna>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard<Hand>",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
        )
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$EarlyColonization>>",
            "3 Energy",
        )
        // Expand the increase into separate track changes for each active colony.
        doTask("EACH @ColonyTile { 2! ColonyProduction<@ColonyTile> }")

        doTasks(
            "Colony<Ceres>",
            "PROD[Steel]",
        )
      }
    }
    kim.inTurn {
          doTasks(
              *cardActionTasks("$BoardOfDirectors"),
              "PreludeCard<Hand>",
              "-12 MC",
              "-Director<$BoardOfDirectors>",
              "-PreludeCard",
          )
          // Execute the fizzle audit before receiving compensation.
          doTasks(
              "Audit!",
              "15 MC",
          )
        }
        .expect("0 $EarlyColonization, 0 Colony<Ceres>, 0 Energy, 0 ColonyProduction<Luna>")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `The next Solar phase advances the boosted tracks normally`() {
      newTestGame(
          "PreludeExpansion, $EarlyColonization, Luna, Ceres, Miranda, Titan, Enceladus, -QuickStartVariant",
          playerCount = 2,
      )
      kim.completeOperation {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$EarlyColonization>>",
            "3 Energy",
            "EACH",
            "Colony<Luna>",
            "PROD[2 MC]",
        )

        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$Donation>>",
            "21 MC",
        )
      }
      stan.completeOperation {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
            "PROD[2 Energy]",
            "4 Steel",
        )

        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
            "PROD[MC]",
            "PROD[Steel]",
            "PROD[Titanium]",
        )
      }
      kim.doTask("Pass")
      stan.doTask("Pass")
      kim.doTasks(
          "3 Heat FROM Energy",
          "22 MC",
      )
      stan.doTasks("21 MC", "Steel", "Titanium", "2 Energy")

      // The tracks advance as the Solar phase begins, without waiting for World Government.
      admin.count("SolarPhase") shouldBe 1
      admin.count("ColonyProduction<Luna>") shouldBe 4
      admin.count("ColonyProduction<Ceres>") shouldBe 4
      admin.count("ColonyProduction<Miranda>") shouldBe 0
      admin.count("ColonyProduction<Titan>") shouldBe 0
      admin.count("ColonyProduction<Enceladus>") shouldBe 0
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    }
  }
}
