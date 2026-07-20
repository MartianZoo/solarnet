package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class PaymentSpecializationTest {
  @Test
  fun `an Accept can pay only with its specialized resource`() {
    val p1 = setUpGame("BM", 2).tfm(PLAYER1)
    p1.godMode().manual("Steel, Titanium")

    p1.godMode().beginManual("Owed<Class<Steel>>") {
      shouldThrow<NarrowingException> {
        doTask("Pay<Class<Titanium>> FROM Titanium")
      }
      doTask("Pay<Class<Steel>> FROM Steel")
    }
  }
}
