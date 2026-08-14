package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER3
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class LawSuitTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("2 Megacredit, ProjectCard, PROD[Plant]")
  }

  @Test
  fun `after p2 lowers p1 production, p1 pays two to play Law Suit`() {
    val p2 = requireP2()
    p2.manual("5 Megacredit, PROD[-Plant<Player1>]")
    p2.assertCounts(5 to "Megacredit")
    p1.assertCounts(1 to "MyProductionWasDecreased<Player1, Class<Plant>, Player2>")

    p1.playProject("LawSuit", 2) { choosePlayer2() }
        .expect("Megacredit<Player1>, -3 Megacredit<Player2>")
  }

  @Test
  fun `an opponent removing p1 resources also permits Law Suit`() {
    val p2 = requireP2()
    p1.manual("Plant")
    p2.manual("5 Megacredit, -Plant<Player1>")

    p1.playProject("LawSuit", 2) { choosePlayer2() }
        .expect("Megacredit<Player1>, -3 Megacredit<Player2>")
  }

  @Test
  fun `without an opponent attack, p1 cannot complete Law Suit`() {
    requireP2().manual("3 Megacredit")
    shouldThrow<RequirementException> { p1.playProject("LawSuit", 2) { choosePlayer2() } }
  }

  @Test
  fun `p1 lowering own production does not qualify`() {
    p1.manual("PROD[-Plant]")
    requireP2().manual("3 Megacredit")

    shouldThrow<RequirementException> { p1.playProject("LawSuit", 2) { choosePlayer2() } }
  }

  @Test
  fun `qualification expires at the next generation`() {
    requireP2().manual("3 Megacredit, PROD[-Plant<Player1>]")
    engine.manual("Generation")

    shouldThrow<RequirementException> { p1.playProject("LawSuit", 2) { choosePlayer2() } }
  }

  @Test
  fun `steals only the money the responsible player has`() {
    requireP2().manual("2 Megacredit, PROD[-Plant<Player1>]")

    p1.playProject("LawSuit", 2) { choosePlayer2() }
        .expect("0 Megacredit<Player1>, -2 Megacredit<Player2>")
  }

  @Test
  fun `a penniless attacker remains selectable when another player has money`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    engine.phase("Action")
    p1.manual("2 Megacredit, ProjectCard, PROD[Plant]")
    p2.manual("PROD[-Plant<Player1>]")
    p3.manual("5 Megacredit")
    p1.assertCounts(1 to "MyProductionWasDecreased<Player1, Class<Plant>, Player2>")

    p1.playProject("LawSuit", 2) { choosePlayer2() }

    p2.assertCounts(1 to "PlayedEvent<Class<LawSuit>>")
    p1.assertCounts(0 to "Megacredit", 0 to "PlayedEvent<Class<LawSuit>>")
    p3.assertCounts(5 to "Megacredit")
  }

  @Test
  fun `with multiple responsible players, p1 chooses which one to sue`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    engine.phase("Action")
    p1.manual("2 Megacredit, ProjectCard, PROD[2 Plant]")
    p2.manual("5 Megacredit, PROD[-Plant<Player1>]")
    p3.manual("5 Megacredit, PROD[-Plant<Player1>]")

    p1.playProject("LawSuit", 2) {
      choosePlayer2()
    }

    p1.assertCounts(3 to "Megacredit")
    p2.assertCounts(2 to "Megacredit")
    p3.assertCounts(5 to "Megacredit")
  }

  @Test
  fun `Law Suit costs the responsible player one victory point`() {
    val p2 = requireP2()
    p2.manual("3 Megacredit, PROD[-Plant<Player1>]")
    p1.playProject("LawSuit", 2) { choosePlayer2() }
    p1.assertCounts(0 to "PlayedEvent<Class<LawSuit>>")
    p2.assertCounts(1 to "PlayedEvent<Class<LawSuit>>")

    engine.phase("End")

    p1.assertCounts(20 to "VictoryPoint")
    p2.assertCounts(19 to "VictoryPoint")
  }

  private fun dev.martianzoo.engine.Gameplay.OperationBody.choosePlayer2() {
    doTask("3 Megacredit<Player1> FROM Megacredit<Player2>.")
  }
}
