package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class VirusTest : CardTest() {
  // FAQ: "you must choose a single card from which to remove animals."
  @Test
  fun `Virus cannot split its animal removal across cards`() {
    val game = newGame("BRM", 2)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    p2.sneak("Birds, Fish, Animal<Birds>, Animal<Fish>")

    shouldThrow<NarrowingException> {
      p1.manual("Virus") { doTask("-Animal<P2, Birds<P2>>, -Animal<P2, Fish<P2>>") }
    }

    p1.manual("Virus") { doTask("-Animal<P2, Birds<P2>>") }.expect("-Animal<P2, Birds<P2>>")
    p2.assertCounts(0 to "Animal<Birds>", 1 to "Animal<Fish>")
  }
}
