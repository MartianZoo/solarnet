package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class PaymentSpecializationTest {
  @Test
  fun `card deck check accepts the matching deck and rejects another`() {
    val player = setUpGame().tfm(PLAYER1).godMode()

    player.manual("CheckCardDeck<Class<ProjectCard>, Class<$AcquiredCompany>>")
    shouldThrow<DeadEndException> {
      player.manual("CheckCardDeck<Class<CorporationCard>, Class<$AcquiredCompany>>")
    }
  }

  @Test
  fun `an Accept can pay only with its specialized resource`() {
    val p1 = setUpGame().tfm(PLAYER1)
    p1.godMode().manual("Steel, Titanium")

    p1.godMode().beginManual("Owed<Class<Steel>>") {
      shouldThrow<NarrowingException> {
        doTask("Pay<Class<Titanium>> FROM Titanium")
      }
      doTask("Pay<Class<Steel>> FROM Steel")
    }
  }
}
