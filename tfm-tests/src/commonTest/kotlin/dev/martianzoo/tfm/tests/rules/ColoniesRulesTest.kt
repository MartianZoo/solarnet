package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.NitriteReducingBacteria
import dev.martianzoo.tfm.tests.cards.cardnames.RegolithEaters
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ColoniesRulesTest : CardTest() {
  @Test
  internal fun `A card-resource colony bonus goes to the colony owner`() {
    newGame(
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = setOf("Luna", "Ceres", "Triton", "Ganymede", "Enceladus").map(::cn).toSet(),
    )
    val p2 = requireP2()
    p1.manual("100 Megacredit, 5 ProjectCard")
    p2.manual("100 Megacredit, 5 ProjectCard")
    engine.phase("Action")
    p2.playProject(RegolithEaters, 13)
    p1.playProject(NitriteReducingBacteria, 11)
    p1.stdProject("BuildColonySP") {
      doTask("Colony<Enceladus>")
      doTask("3 Microbe<$NitriteReducingBacteria>")
    }

    p2.stdAction("TradeSA", 1) {
      doTask("Trade<Enceladus>")
      doTask("Microbe<$RegolithEaters>")
      p1.doTask("Microbe<$NitriteReducingBacteria>")
    }

    p1.count("Microbe<$NitriteReducingBacteria>") shouldBe 7
    p2.count("Microbe<$RegolithEaters>") shouldBe 1
  }

  @Test
  internal fun `Pluto draws a card before requiring its discard`() {
    newGame(
        ColoniesExpansion,
        colonyTiles = testColonyTiles(players = 2, "Pluto"),
    )
    val p2 = requireP2()
    p1.manual("Colony<Pluto>")
    p1.manual("-2 ProjectCard")
    p2.manual("3 Energy")
    engine.phase("Action")

    p2.stdAction("TradeSA", 2) { doTask("Trade<Pluto>") }

    p1.count("ProjectCard") shouldBe 0
    p2.count("ProjectCard") shouldBe 1
  }
}
