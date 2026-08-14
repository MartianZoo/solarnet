package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class VirusTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    val p2 = requireP2()
    p2.manual("PROD[2 Plant], Birds")
    p2.manual("PROD[Plant], Fish")
    p2.manual("Animal<Birds>, Animal<Fish>")
  }

  // FAQ: "you must choose a single card from which to remove animals."
  @Test
  fun `with animals on two cards, p1 tries to remove from both using Virus`() {
    shouldThrow<NarrowingException> {
      p1.manual("Virus") {
        doTask("-Animal<Player2, Birds<Player2>>, -Animal<Player2, Fish<Player2>>")
      }
    }
  }

  @Test
  fun `with animals on two cards, p1 removes from one using Virus`() {
    p1.manual("Virus") { doTask("-Animal<Player2, Birds<Player2>>") }
        .expect("-Animal<P2, Birds<P2>>")
    requireP2().assertCounts(0 to "Animal<Birds>", 1 to "Animal<Fish>")
  }
}
