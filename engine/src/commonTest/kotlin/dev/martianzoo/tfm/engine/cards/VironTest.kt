package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class VironTest : CardTest() {
  @Test
  fun `can repeat the same action on a card`() {
    val p1 = newPlayer()

    p1.cardAction1("AtmoCollectors")

    p1.useViron { doTask("UseAction1<AtmoCollectors>") }.expect("Floater")

    p1.assertOneActionMarkerOnEachCard()
  }

  @Test
  fun `can choose a different action on the same card`() {
    val p1 = newPlayer()

    p1.cardAction1("AtmoCollectors")

    p1.useViron {
          doTask("UseAction2<AtmoCollectors>")
          doTask("2 Titanium")
        }
        .expect("-Floater")

    p1.assertOneActionMarkerOnEachCard()
  }

  @Test
  fun `cannot choose Viron itself`() {
    val p1 = newPlayer()

    p1.useViron {
      shouldThrow<NarrowingException> { doTask("UseAction1<Viron>") }
      abort()
    }
  }

  @Test
  fun `cannot choose a card whose action has not been used`() {
    val p1 = newPlayer()

    p1.useViron {
      shouldThrow<NarrowingException> { doTask("UseAction1<AtmoCollectors>") }
      abort()
    }
  }

  @Test
  fun `cannot choose another player's used card`() {
    val game = newGame(GameSetup(Canon, "BMVC", 2))
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    p1.phase("Action")
    p1.godMode().manual("Viron")
    p2.godMode().manual("AtmoCollectors") { doTask("2 Floater<AtmoCollectors>") }
    p2.cardAction1("AtmoCollectors")

    p1.useViron {
      shouldThrow<NarrowingException> { doTask("UseAction1<AtmoCollectors<Player2>>") }
      abort()
    }

    p2.assertCounts(1 to "ActionUsedMarker<AtmoCollectors>")
  }

  private fun newPlayer(): TfmGameplay {
    val game = newGame(GameSetup(Canon, "BMVC", 2))
    return game.tfm(PLAYER1).also {
      it.phase("Action")
      it.godMode().manual("Viron")
      it.godMode().manual("AtmoCollectors") { doTask("2 Floater<AtmoCollectors>") }
    }
  }

  private fun TfmGameplay.assertOneActionMarkerOnEachCard() {
    assertCounts(
        1 to "ActionUsedMarker<AtmoCollectors>",
        1 to "ActionUsedMarker<Viron>",
        2 to "ActionUsedMarker",
    )
  }

  private fun TfmGameplay.useViron(body: OperationBody.() -> Unit) =
      stdAction("UseCardActionSA") {
        doTask("UseAction1<Viron>")
        body()
        doTask("ActionUsedMarker<Viron>")
      }
}
