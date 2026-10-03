package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ConscriptionTest : CardTest() {
  @Test
  internal fun `Intervening Prelude leaves the discount for the next project card`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        ColoniesExpansion,
        CorporateEraExpansion,
        colonyTiles = testColonyTiles(2),
    )
    admin.phase("Prelude")
    p1.playPrelude(BoardOfDirectors)
    admin.phase("Action")
    p1.runOperation("50 MC, 2 ProjectCard, $Sponsors")

    p1.playProject(Conscription, 5)
    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(Donation)
    }
    val moneyBefore = p1.count("MC")

    p1.playProject(Soletta, 19).expect("PROD[7 Heat]")
    p1.count("MC") shouldBe moneyBefore - 19
  }
}
