package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.ExpressionException
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
  internal fun `card play rejects a front from a different deck`() {
    val player = setUpGame().tfm(PLAYER1)

    shouldThrow<ExpressionException> {
      player.beginManual("PlayCard<Class<CorporationCard>, Class<$AcquiredCompany>, Hand>")
    }
  }

  @Test
  internal fun `Accepting pays only with its specialized resource`() {
    val p1 = setUpGame().tfm(PLAYER1)
    p1.manual("Steel, Titanium")

    p1.beginManual("Owed<Class<Steel>> THEN Invoice<CardPurchase, Action1, Class<Steel>>") {
      shouldThrow<NarrowingException> { doTask("Pay<Class<Titanium>> FROM Titanium") }
      doTask("Pay<Class<Steel>> FROM Steel")
    }
  }
}
