package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.data.Player.Companion.PLAYER3
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.AirRaid
import dev.martianzoo.tfm.tests.cards.cardnames.AtmoCollectors
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AirRaidTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(ColoniesExpansion, players = 3, colonyTiles = testColonyTiles(3))
    engine.phase("Action")
    p1.autoExecMode = NONE
    p1.manual("$AtmoCollectors") { addCardResources(AtmoCollectors) }
    p1.manual("MC, ProjectCard")
  }

  @Test
  internal fun `Steals all five mc`() {
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p2.manual("5 MC")
    p3.manual("5 MC")

    p1.playProject(AirRaid, 0) {
          doTask("5 MC<Player1> FROM MC<Player3>")
          doTask("-Floater<$AtmoCollectors>")
        }
        .expect("-Floater<$AtmoCollectors>, 5 MC<Player1>, -5 MC<Player3>")

    p2.assertCounts(5 to "MC")
  }

  @Test
  internal fun `Cannot be played when no player has five mc`() {
    val p2 = requireP2()
    p2.manual("4 MC")

    shouldThrow<LimitsException> {
      p1.playProject(AirRaid, 0) { doTask("5 MC<Player1> FROM MC<Player2>") }
    }

    p1.assertCounts(1 to "MC", 2 to "Floater<$AtmoCollectors>", 1 to "ProjectCard")
    p2.assertCounts(4 to "MC")
  }
}
