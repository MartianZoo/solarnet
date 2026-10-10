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

    kim.playPrelude(EarlyColonization) { doTask("Colony<Luna>") }
        .expect(
            "2 ColonyProduction<Luna>, 2 ColonyProduction<Ceres>, 0 ColonyProduction<Miranda>, " +
                "0 ColonyProduction<Titan>, 0 ColonyProduction<Enceladus>, Colony<Luna>, 3 Energy"
        )
  }

  @Test
  internal fun `Cannot be played when its owner already has a colony on every active tile`() {
    newTestGame("PreludeExpansion, EarlyColonization, Luna, Ceres, Miranda, Titan, Enceladus")
    kim.exMachina("Colony<Luna>, Colony<Ceres>")

    shouldThrow<DependencyException> { kim.playPrelude(EarlyColonization) }
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
    kim.playPrelude(BoardOfDirectors)
    startActionPhase()
    admin.sneak("${position - admin.count("ColonyProduction<Luna>")} ColonyProduction<Luna>")

    shouldThrow<LimitsException> {
      kim.cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        kim.playPrelude(EarlyColonization) { doTask("Colony<Ceres>") }
      }
    }
    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          doTask("-PreludeCard")
        }
        .expect("3 MC, 0 $EarlyColonization, 0 Colony<Ceres>, 0 Energy, 0 ColonyProduction<Luna>")
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
      admin.count("ColonyProduction<Luna>") shouldBe 3
      admin.count("ColonyProduction<Ceres>") shouldBe 3

      kim.wgt("VenusStep")

      admin.count("ColonyProduction<Luna>") shouldBe 4
      admin.count("ColonyProduction<Ceres>") shouldBe 4
      admin.count("ColonyProduction<Miranda>") shouldBe 0
      admin.count("ColonyProduction<Titan>") shouldBe 0
      admin.count("ColonyProduction<Enceladus>") shouldBe 0
    }
  }
}
