package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilEventsTest : CardTest() {
  @Test
  internal fun `setup reveals coming and distant events with their neutral delegates`() {
    newGame(TurmoilExpansion)

    admin.count("GlobalEvent") shouldBe 2
    admin.count("Coming<Class<AquiferReleasedByPublicCouncil>>") shouldBe 1
    admin.count("Distant<Class<DryDeserts>>") shouldBe 1
    admin.count("Current") shouldBe 0
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 1
    admin.count("PartyDelegate<Reds, Neutral>") shouldBe 1
    admin.count("Dominant<MarsFirst>") shouldBe 1
    admin.count("ReserveDelegate<Neutral>") shouldBe 11
  }

  @Test
  internal fun `changing times advances events discards current and reveals the next card`() {
    newGame(TurmoilExpansion)

    admin.manual("ChangingTimes") { doTask("CelebrityLeaders") }

    admin.count("GlobalEvent") shouldBe 3
    admin.count("Current<Class<AquiferReleasedByPublicCouncil>>") shouldBe 1
    admin.count("Coming<Class<DryDeserts>>") shouldBe 1
    admin.count("Distant<Class<CelebrityLeaders>>") shouldBe 1
    admin.count("PartyDelegate<Greens, Neutral>") shouldBe 1
    admin.count("PartyDelegate<Unity, Neutral>") shouldBe 1
    admin.count("ReserveDelegate<Neutral>") shouldBe 9

    admin.manual("ChangingTimes") { doTask("Diversity") }

    admin.count("GlobalEvent") shouldBe 3
    admin.count("AquiferReleasedByPublicCouncil") shouldBe 0
    admin.count("Current<Class<DryDeserts>>") shouldBe 1
    admin.count("Coming<Class<CelebrityLeaders>>") shouldBe 1
    admin.count("Distant<Class<Diversity>>") shouldBe 1
    admin.count("PartyDelegate<Unity, Neutral>") shouldBe 2
    admin.count("PartyDelegate<Scientists, Neutral>") shouldBe 1
    admin.count("ReserveDelegate<Neutral>") shouldBe 7
  }
}
