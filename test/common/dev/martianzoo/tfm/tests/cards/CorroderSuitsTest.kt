package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.CorroderSuits
import dev.martianzoo.tfm.tests.cards.cardnames.VenusianAnimals
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class CorroderSuitsTest : CardTest() {
  @Test
  internal fun `Can be played without another compatible Venus card`() {
    newGame(VenusNextExpansion)

    p1.runOperation("$CorroderSuits").expect("PROD[2 MC], 0 CardResource")
  }

  @Test
  internal fun `Must add an animal when it is the only compatible Venus resource`() {
    newGame(VenusNextExpansion)
    p1.runOperation("$VenusianAnimals")

    // Unlike Venusian Plants, this one resource instruction has no unavailable arm to choose.
    p1.runOperation("$CorroderSuits") {
          shouldThrow<NarrowingException> { declineTask() }
          addCardResources(VenusianAnimals)
        }
        .expect("PROD[2 MC], Animal<$VenusianAnimals>")
  }
}
