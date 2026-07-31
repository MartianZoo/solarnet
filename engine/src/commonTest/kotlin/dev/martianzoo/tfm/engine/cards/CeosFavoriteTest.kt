package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class CeosFavoriteTest : CardTest() {
  // FAQ: "This card can be played to add an additional resource to 'Search for Life'."
  @Test
  fun `with science on Search for Life, resolves CEO's Favorite Project`() {
    newGame("VenusNextExpansion")
    p1.manual("SearchForLife, Science<SearchForLife>")
    p1.manual("CeosFavoriteProject") { doTask("Science<SearchForLife>") }
        .expect("Science<SearchForLife>")
    p1.assertCounts(2 to "Science<SearchForLife>")
  }

  // FAQ: "this card can still be played without effect."
  @Test
  fun `without a resource-bearing card, resolves CEO's Favorite Project`() {
    newGame("VenusNextExpansion")
    p1.manual("Tardigrades")
    p1.manual("CeosFavoriteProject")
    p1.assertCounts(0 to "Microbe<Tardigrades>")
  }

  @Test
  fun `with an eligible card, tries to skip its resource choice`() {
    newGame("VenusNextExpansion")
    p1.manual("SearchForLife, Science<SearchForLife>")
    shouldThrow<NarrowingException> { p1.manual("CeosFavoriteProject") { doTask("Ok") } }
  }
}
