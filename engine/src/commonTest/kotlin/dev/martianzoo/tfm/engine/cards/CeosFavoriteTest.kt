package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class CeosFavoriteTest : CardTest() {
  // FAQ: "This card can be played to add an additional resource to 'Search for Life'."
  @Test
  fun `Can add a resource to Search for Life`() {
    newGame(VenusNextExpansion)
    p1.manual("$SearchForLife, Science<$SearchForLife>")
    p1.manual("$CeosFavoriteProject") { doTask("Science<$SearchForLife>") }
        .expect("Science<$SearchForLife>")
    p1.assertCounts(2 to "Science<$SearchForLife>")
  }

  // FAQ: "this card can still be played without effect."
  @Test
  fun `Can be played without a resource-bearing card`() {
    newGame(VenusNextExpansion)
    p1.manual("$Tardigrades")
    p1.manual("$CeosFavoriteProject")
    p1.assertCounts(0 to "Microbe<$Tardigrades>")
  }

  @Test
  fun `Cannot skip its resource choice when an eligible card exists`() {
    newGame(VenusNextExpansion)
    p1.manual("$SearchForLife, Science<$SearchForLife>")
    shouldThrow<NarrowingException> { p1.manual("$CeosFavoriteProject") { doTask("Ok") } }
  }
}
