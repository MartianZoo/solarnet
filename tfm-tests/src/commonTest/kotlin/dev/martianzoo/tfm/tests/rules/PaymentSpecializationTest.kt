package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class PaymentSpecializationTest {
  @Test
  internal fun `card deck check accepts the matching deck and rejects another`() {
    val player = setUpGame().tfm(PLAYER1).godMode()

    player.manual("CheckCardDeck<Class<ProjectCard>, Class<$AcquiredCompany>>")
    shouldThrow<DeadEndException> {
      player.manual("CheckCardDeck<Class<CorporationCard>, Class<$AcquiredCompany>>")
    }
  }

  @Test
  internal fun `an Accept can pay only with its specialized resource`() {
    val p1 = setUpGame().tfm(PLAYER1)
    p1.godMode().manual("Steel, Titanium")

    p1.godMode().beginManual("Owed<Class<Steel>> THEN Invoice<BuyCards, First, Class<Steel>>") {
      shouldThrow<NarrowingException> { doTask("Pay<Class<Titanium>> FROM Titanium") }
      doTask("Pay<Class<Steel>> FROM Steel")
    }
  }
}
