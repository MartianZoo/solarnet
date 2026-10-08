package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.CorroderSuits
import dev.martianzoo.tfm.tests.cards.cardnames.VenusianAnimals
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class CorroderSuitsTest : ProjectCardTest() {
  @Test
  internal fun `Can be played without an eligible Venus card`() {
    kim.playProject(CorroderSuits, 8).expect("PROD[2 MC], 0 CardResource")
  }

  @Test
  internal fun `Cannot decline when an eligible Venus resource exists`() {
    kim.exMachina("$VenusianAnimals")

    // Unlike Venusian Plants, this one resource instruction has no unavailable arm to choose.
    shouldThrow<NarrowingException> { kim.playProject(CorroderSuits, 8) { declineTask() } }
  }
}
