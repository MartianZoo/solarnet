package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.AirRaid
import dev.martianzoo.tfm.tests.cards.cardnames.AtmoCollectors
import dev.martianzoo.tfm.tests.cards.cardnames.SearchForLife
import dev.martianzoo.tfm.tests.cards.cardnames.Tardigrades
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AirRaidTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGameWithAutoWorkflow(
        ColoniesExpansion,
        players = 3,
        colonyTiles = testColonyTiles(3),
    )
    playUntilFirstActionPhase()
    p1.turn { playProject(AtmoCollectors, 15) { addCardResources(AtmoCollectors) } }
    p1.autoExecPolicy = NONE
  }

  @Test
  internal fun `Steals all five MC`() {
    val p2 = requireP2()
    val p3 = game.testTfm(PLAYER3)
    p2.pass()
    p3.turn { playProject(SearchForLife, 3) }

    p1.playProject(AirRaid, 0) {
          doTask("5 MC FROM MC<Player3>")
          doTask("-Floater<$AtmoCollectors>")
        }
        .expect("-Floater<$AtmoCollectors>, 5 MC<Player1>, -5 MC<Player3>")
  }

  @Test
  internal fun `Cannot steal from yourself`() {
    val p2 = requireP2()
    val p3 = game.testTfm(PLAYER3)
    p2.pass()
    p3.turn { playProject(SearchForLife, 3) }

    shouldThrow<ExpressionException> {
      p1.playProject(AirRaid, 0) { doTask("5 MC FROM MC<Player1>") }
    }
  }

  @Test
  internal fun `Cannot be played when only you have five mc`() {
    val p2 = requireP2()
    val p3 = game.testTfm(PLAYER3)
    p2.turn { stdProject("PowerPlantProject") }
    p3.turn { playProject(Tardigrades, 4) }
    p1.runOperation("-${p1.count("MC") - 5} MC")
    p1.assertCounts(5 to "MC")

    shouldThrow<LimitsException> {
      p1.playProject(AirRaid, 0) { doTask("5 MC FROM MC<Player2>") }
    }
  }
}
