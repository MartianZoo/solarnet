package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ArcticAlgaeTest : CardTest() {
  @Test
  internal fun `Triggers when an opponent places an ocean`() {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase(MiningGuild, UnitedNationsMarsInitiative)
    val p2 = requireP2()

    p1.turn {
      playProject(ArcticAlgae, 12)
      sellPatents(1)
    }
    p2.stdProject("AquiferSP") { placeTile(1, 2) }.expect("2 Plant<Player1>")
  }

  @Test
  internal fun `Can resolve ocean placements and Arctic Algae before removing plants`() {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase(UnitedNationsMarsInitiative, Phobolog)
    prepareGiantIceAsteroid()
    val p2 = requireP2()

    p1.playProject(GiantIceAsteroid, 36) {
          p1.autoExecMode = NONE
          p2.autoExecMode = NONE
          shouldThrow<TaskException> { p2.doTask("2 Plant") }
          doTask("OceanTile<Tharsis_1_2>")
          doTask("2 Plant<Player2>")
          doTask("OceanTile<Tharsis_1_4>")
          doTask("2 Plant<Player2>")
          doTask("-6 Plant<Player2>")
          p1.autoExecMode = FIRST
          p2.autoExecMode = FIRST
        }
        .expect("-2 Plant<Player2>")
  }

  @Test
  internal fun `Can remove plants before resolving ocean placements and Arctic Algae`() {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase(UnitedNationsMarsInitiative, Phobolog)
    prepareGiantIceAsteroid()
    val p2 = requireP2()

    p1.playProject(GiantIceAsteroid, 36) {
          p1.autoExecMode = NONE
          p2.autoExecMode = NONE
          doTask("-6 Plant<Player2>")
          doTask("OceanTile<Tharsis_1_2>")
          doTask("OceanTile<Tharsis_1_4>")
          p1.autoExecMode = FIRST
          p2.autoExecMode = FIRST
        }
        .expect("-2 Plant<Player2>")
  }

  private fun prepareGiantIceAsteroid() {
    p1.turn { sellPatents(1) }
    requireP2().turn {
      playProject(InvestmentLoan, 3)
      sellPatents(1)
    }
    p1.turn {
      sellPatents(1)
      sellPatents(1)
    }
    requireP2().turn {
      playProject(ArcticAlgae, 12)
      playProject(ImportedHydrogen, titanium = 4) {
        doTask("3 Plant")
        placeTile(5, 4)
      }
    }
  }
}
