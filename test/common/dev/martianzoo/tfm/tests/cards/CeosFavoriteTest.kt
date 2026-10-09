package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CeosFavoriteTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  // FAQ: "This card can be played to add an additional resource to 'Search for Life'."
  @Test
  internal fun `Adds a resource to Search for Life`() {
    kim.exMachina("$SearchForLife, Science<$SearchForLife>")

    kim.playProject(CeosFavoriteProject, 1) { doTask("Science<$SearchForLife>") }
        .expect("Science<$SearchForLife>")
  }

  // FAQ: "this card can still be played without effect."
  @Test
  internal fun `Can be played without an eligible resource-bearing card`() {
    kim.exMachina("$Tardigrades")

    kim.playProject(CeosFavoriteProject, 1).expect("0 Microbe<$Tardigrades>")
  }

  @Test
  internal fun `Cannot decline when an eligible resource exists`() {
    kim.exMachina("$SearchForLife, Science<$SearchForLife>")

    shouldThrow<NarrowingException> {
      kim.playProject(CeosFavoriteProject, 1) { doTask("Ok") }
    }
  }
}
