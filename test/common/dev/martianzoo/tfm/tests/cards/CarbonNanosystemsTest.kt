package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CarbonNanosystemsTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Multiple graphene can pay for the same space card`() {
    kim.exMachina("$CarbonNanosystems, 2 Graphene<$CarbonNanosystems>")

    kim.playProject(IcyImpactors, 7) { doTask("-2 Graphene<$CarbonNanosystems>") }
        .expect("-2 Graphene<$CarbonNanosystems>")
  }
}
