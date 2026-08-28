package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class CeosFavoriteTest : CardTest() {
  // FAQ: "This card can be played to add an additional resource to 'Search for Life'."
  @Test
  internal fun `Can add a resource to Search for Life`() {
    newGame(VenusNextExpansion)
    p1.manual("$SearchForLife, Science<$SearchForLife>")
    p1.manual("$CeosFavoriteProject") { doTask("Science<$SearchForLife>") }
        .expect("Science<$SearchForLife>")
    p1.assertCounts(2 to "Science<$SearchForLife>")
  }

  // FAQ: "this card can still be played without effect."
  @Test
  internal fun `Can be played without a resource-bearing card`() {
    newGame(VenusNextExpansion)
    p1.manual("$Tardigrades")
    p1.manual("$CeosFavoriteProject")
    p1.assertCounts(0 to "Microbe<$Tardigrades>")
  }

  @Test
  internal fun `Cannot skip its resource choice when an eligible card exists`() {
    newGame(VenusNextExpansion)
    p1.manual("$SearchForLife, Science<$SearchForLife>")
    shouldThrow<NarrowingException> { p1.manual("$CeosFavoriteProject") { doTask("Ok") } }
  }
}
