package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class VirusTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    val p2 = requireP2()
    p2.manual("PROD[2 Plant], $Birds")
    p2.manual("PROD[Plant], $Fish")
    p2.manual("Animal<$Birds>, Animal<$Fish>")
  }

  // FAQ: "you must choose a single card from which to remove animals."
  @Test
  internal fun `Cannot split animal removal across two cards`() {
    shouldThrow<NarrowingException> {
      p1.manual("$Virus") {
        doTask("-Animal<Player2, $Birds<Player2>>, -Animal<Player2, $Fish<Player2>>")
      }
    }
  }

  @Test
  internal fun `Can remove animals from one of multiple eligible cards`() {
    p1.manual("$Virus") { doTask("-Animal<Player2, $Birds<Player2>>") }
        .expect("-Animal<Player2, $Birds<Player2>>")
    requireP2().assertCounts(0 to "Animal<$Birds>", 1 to "Animal<$Fish>")
  }
}
