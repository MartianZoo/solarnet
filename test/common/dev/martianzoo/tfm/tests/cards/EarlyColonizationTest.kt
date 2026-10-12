package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EarlyColonizationTest : TfmSandboxTest() {
  @Test
  internal fun `Advances active tracks from their current positions and leaves inactive tiles alone`() {
    newTestGame("PreludeExpansion, EarlyColonization, Luna, Ceres, Miranda, Titan, Enceladus")
    admin.sneak("2 ColonyProduction<Luna>")

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<EarlyColonization>>",
              "EarlyColonization FROM PreludeCard",
              "3 Energy",
          )
          selectTask("EACH @ColonyTile { 2 ColonyProduction<@ColonyTile>! }")
          doTasks("Colony<Luna>", "PROD[2 MC]")
        }
        .expect(
            "2 ColonyProduction<Luna>, 2 ColonyProduction<Ceres>, 0 ColonyProduction<Miranda>, " +
                "0 ColonyProduction<Titan>, 0 ColonyProduction<Enceladus>, Colony<Luna>, 3 Energy"
        )
  }

  @Test
  internal fun `Cannot be played when its owner already has a colony on every active tile`() {
    newTestGame("PreludeExpansion, EarlyColonization, Luna, Ceres, Miranda, Titan, Enceladus")
    kim.exMachina("Colony<Luna>, Colony<Ceres>")

    shouldThrow<DependencyException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<EarlyColonization>>",
            "EarlyColonization FROM PreludeCard",
        )
        selectTask("3 Energy!")
        selectTask("EACH @ColonyTile { 2 ColonyProduction<@ColonyTile>! }")
        selectOnlyTask()
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
    newTestGame("PreludeExpansion, EarlyColonization, BoardOfDirectors, Luna, Ceres")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    admin.sneak("${position - admin.count("ColonyProduction<Luna>")} ColonyProduction<Luna>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<BoardOfDirectors, Action1>")
        selectTask("PreludeCard<Kim, Hand>!")
        selectOnlyTask()
        doTask("-12 MC")
        selectTask("-Director<BoardOfDirectors>!")
        selectOnlyTask()
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<EarlyColonization>>",
            "EarlyColonization FROM PreludeCard",
        )
        selectTask("3 Energy!")
        selectTask("EACH @ColonyTile { 2 ColonyProduction<@ColonyTile>! }")
        selectTask("Colony<Kim, ColonyTile(HAS MAX 0 Colony)>!")
        doTask("Colony<Ceres>")
        selectTask("PROD[Steel]")
      }
    }
    kim.inTurn {
          doTask("UseCardAction<BoardOfDirectors, Action1>")
          selectTask("PreludeCard<Kim, Hand>!")
          selectOnlyTask()
          doTask("-12 MC")
          selectTask("-Director<BoardOfDirectors>!")
          doTask("-PreludeCard")
          selectTask("Audit!")
          doTask("15 MC")
        }
        .expect("0 $EarlyColonization, 0 Colony<Ceres>, 0 Energy, 0 ColonyProduction<Luna>")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `The next Solar phase advances the boosted tracks normally`() {
      newTestGame(
          "PreludeExpansion, EarlyColonization, Luna, Ceres, Miranda, Titan, Enceladus",
          playerCount = 2,
      )
      kim.playPrelude(EarlyColonization) { doTask("Colony<Luna>") }
      kim.playPrelude(Donation)
      stan.playPrelude(Supplier)
      stan.playPrelude(MetalsCompany)
      kim.pass()
      stan.pass()

      // The tracks advance as the Solar phase begins, without waiting for World Government.
      admin.count("SolarPhase") shouldBe 1
      admin.count("ColonyProduction<Luna>") shouldBe 4
      admin.count("ColonyProduction<Ceres>") shouldBe 4
      admin.count("ColonyProduction<Miranda>") shouldBe 0
      admin.count("ColonyProduction<Titan>") shouldBe 0
      admin.count("ColonyProduction<Enceladus>") shouldBe 0
    }
  }
}
