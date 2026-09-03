package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.data.Player.Companion.PLAYER3
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class LawSuitTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("2 MC, ProjectCard, PROD[Plant]")
  }

  @Test
  internal fun `Can be played after an opponent lowers the owner's production`() {
    val p2 = requireP2()
    p2.manual("5 MC, PROD[-Plant<Player1>]")
    p2.assertCounts(5 to "MC")
    p1.assertCounts(1 to "MyProductionWasDecreased<Player1, Class<Plant>, Player2>")

    p1.playProject(LawSuit, 2) { choosePlayer2() }.expect("1 MC<Player1>, -3 MC<Player2>")
  }

  @Test
  internal fun `Can be played after an opponent removes the owner's resources`() {
    val p2 = requireP2()
    p1.manual("Plant")
    p2.manual("5 MC, -Plant<Player1>")

    p1.playProject(LawSuit, 2) { choosePlayer2() }.expect("1 MC<Player1>, -3 MC<Player2>")
  }

  @Test
  internal fun `Cannot be played without an opponent's attack`() {
    requireP2().manual("3 MC")
    shouldThrow<RequirementException> { p1.playProject(LawSuit, 2) { choosePlayer2() } }
  }

  @Test
  internal fun `Its player lowering their own production does not qualify`() {
    p1.manual("PROD[-Plant]")
    requireP2().manual("3 MC")

    shouldThrow<RequirementException> { p1.playProject(LawSuit, 2) { choosePlayer2() } }
  }

  @Test
  internal fun `Qualification expires at the next generation`() {
    requireP2().manual("3 MC, PROD[-Plant<Player1>]")
    engine.manual("Generation")

    shouldThrow<RequirementException> { p1.playProject(LawSuit, 2) { choosePlayer2() } }
  }

  @Test
  internal fun `Steals only the money the responsible player has`() {
    requireP2().manual("2 MC, PROD[-Plant<Player1>]")

    p1.playProject(LawSuit, 2) { choosePlayer2() }.expect("0 MC<Player1>, -2 MC<Player2>")
  }

  @Test
  internal fun `A penniless attacker remains selectable when another player has money`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    engine.phase("Action")
    p1.manual("2 MC, ProjectCard, PROD[Plant]")
    p2.manual("PROD[-Plant<Player1>]")
    p3.manual("5 MC")
    p1.assertCounts(1 to "MyProductionWasDecreased<Player1, Class<Plant>, Player2>")

    p1.playProject(LawSuit, 2) { choosePlayer2() }

    p2.assertCounts(1 to "PlayedEvent<Class<$LawSuit>>")
    p1.assertCounts(0 to "MC", 0 to "PlayedEvent<Class<$LawSuit>>")
    p3.assertCounts(5 to "MC")
  }

  @Test
  internal fun `Can choose among multiple responsible players`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    engine.phase("Action")
    p1.manual("2 MC, ProjectCard, PROD[2 Plant]")
    p2.manual("5 MC, PROD[-Plant<Player1>]")
    p3.manual("5 MC, PROD[-Plant<Player1>]")

    p1.playProject(LawSuit, 2) { choosePlayer2() }

    p1.assertCounts(3 to "MC")
    p2.assertCounts(2 to "MC")
    p3.assertCounts(5 to "MC")
  }

  @Test
  internal fun `Law Suit costs the responsible player one victory point`() {
    val p2 = requireP2()
    p2.manual("3 MC, PROD[-Plant<Player1>]")
    p1.playProject(LawSuit, 2) { choosePlayer2() }
    p1.assertCounts(0 to "PlayedEvent<Class<$LawSuit>>")
    p2.assertCounts(1 to "PlayedEvent<Class<$LawSuit>>")

    engine.manual("End FROM Phase")

    p1.assertCounts(20 to "VictoryPoint")
    p2.assertCounts(19 to "VictoryPoint")
  }

  private fun dev.martianzoo.engine.Agent.OperationBody.choosePlayer2() {
    doTask("3 MC<Player1> FROM MC<Player2>.")
  }
}
