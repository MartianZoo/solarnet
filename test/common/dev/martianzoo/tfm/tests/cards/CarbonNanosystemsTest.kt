package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CarbonNanosystemsTest : ProjectCardTest() {
  @Test
  internal fun `Its graphene payment resource works without the Promo card pack`() {
    newTestGame(addOptions = "-PromoCardPack, CarbonNanosystems")
    kim.exMachina("$CarbonNanosystems, 2 Graphene<$CarbonNanosystems>")

    kim.playProject(AsteroidCard, 6) { doTask("-2 Graphene<$CarbonNanosystems>") }
        .expect("-2 Graphene<$CarbonNanosystems>")
  }

  @Test
  internal fun `Multiple graphene can pay for the same space card with Promos`() {
    newTestGame()
    kim.exMachina("$CarbonNanosystems, 2 Graphene<$CarbonNanosystems>")

    kim.playProject(AsteroidCard, 6) { doTask("-2 Graphene<$CarbonNanosystems>") }
        .expect("-2 Graphene<$CarbonNanosystems>")
  }
}
