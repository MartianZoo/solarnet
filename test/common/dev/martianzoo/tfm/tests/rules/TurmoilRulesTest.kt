package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilRulesTest : CardTest() {
  @Test
  internal fun `setup creates one lobby delegate per player and the initial government`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()

    p1.count("TurmoilPlayer") shouldBe 1
    p2.count("TurmoilPlayer") shouldBe 1
    p1.count("LobbyDelegate") shouldBe 1
    p2.count("LobbyDelegate") shouldBe 1
    admin.count("Neutral") shouldBe 1
    admin.count("Party") shouldBe 6
    admin.count("Chairman<Neutral>") shouldBe 1
    admin.count("Ruling<Greens>") shouldBe 1
    admin.count("Ruling") shouldBe 1
  }
}
