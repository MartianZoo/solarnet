package dev.martianzoo.pets

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Section 7 of `docs/pets-language-spec.md`: which more specific instruction is an acceptable way
 * of carrying out a more general one. Every instruction here is elaborated first (L12-1), because
 * an authored change carries no quantifier to compare until it is.
 */
internal class Lang07NarrowingTest {

  private fun narrows(wide: String, narrow: String): Boolean =
      elaborate(narrow).narrows(elaborate(wide), langWorld)

  private fun refuses(wide: String, narrow: String): NarrowingException = shouldThrow {
    elaborate(narrow).ensureNarrows(elaborate(wide), langWorld)
  }

  private fun abstract(source: String): Boolean = elaborate(source).isAbstract(langWorld)

  // L7-1 What counts as open

  @Test
  internal fun `L7-1 an instruction is abstract when something is still open`() {
    abstract("2 Plant!") shouldBe false
    abstract("2 Plant?") shouldBe true
    abstract("X Plant!") shouldBe true
    abstract("Plant! OR Heat!") shouldBe true
    abstract("Tile<>") shouldBe true
    abstract("GreeneryTile<Land1>") shouldBe false
    abstract("Ok") shouldBe false
  }

  @Test
  internal fun `L7-1 an abstract part anywhere makes the whole thing abstract`() {
    abstract("Plant, Tile<>") shouldBe true
    abstract("Plant THEN Tile<>") shouldBe true
    abstract("MAX 0 Heat: Tile<>") shouldBe true
    abstract("Plant / Heat") shouldBe false
  }

  // L7-2 Shape

  @Test
  internal fun `L7-2 a narrowing preserves the kind of node`() {
    refuses("2 Plant", "-2 Plant")
    refuses("Plant THEN Heat", "Plant")
    refuses("MAX 0 Heat: Plant", "Plant")
    refuses("Plant / Heat", "Plant")
  }

  @Test
  internal fun `L7-2 a narrowing preserves the number of stages and the size of a group`() {
    refuses("Plant THEN Heat", "Plant THEN Heat THEN Steel")
    refuses("Plant, Heat", "Plant, Heat, Steel")
    narrows("Plant THEN Heat", "Plant THEN Heat") shouldBe true
  }

  // L7-3 Changes

  @Test
  internal fun `L7-3 a count may not grow, and shrinks only under an optional quantifier`() {
    narrows("2 Plant!", "2 Plant!") shouldBe true
    refuses("2 Plant!", "3 Plant!")
    refuses("2 Plant!", "Plant!")
    narrows("2 Plant?", "Plant?") shouldBe true
    refuses("2 Plant?", "3 Plant?")
    refuses("2 Plant.", "Plant.")
  }

  @Test
  internal fun `L7-3 an optional quantifier may become anything, and mandatory and AMAP may not`() {
    narrows("2 Plant?", "2 Plant!") shouldBe true
    narrows("2 Plant?", "2 Plant.") shouldBe true
    narrows("2 Plant?", "2 Plant?") shouldBe true
    refuses("2 Plant!", "2 Plant.")
    refuses("2 Plant.", "2 Plant!")
  }

  @Test
  internal fun `L7-3 each written expression must narrow the authored one`() {
    narrows("Tile<>", "GreeneryTile<Land1>") shouldBe true
    narrows("Tile<Land1>", "GreeneryTile<Land1>") shouldBe true
    refuses("Tile<Land1>", "GreeneryTile<Land2>")
    refuses("Tile<>", "Plant")
    narrows("Plant FROM Heat", "Plant FROM Heat") shouldBe true
  }

  // L7-4 Ok

  @Test
  internal fun `L7-4 Ok narrows an optional change and nothing else`() {
    narrows("2 Plant?", "Ok") shouldBe true
    refuses("2 Plant!", "Ok")
    refuses("2 Plant.", "Ok")
  }

  // L7-5 What is not a choice

  @Test
  internal fun `L7-5 a gate, a metric, an actor and a selector must be reproduced exactly`() {
    narrows("MAX 0 Heat: Tile<>", "MAX 0 Heat: GreeneryTile<Land1>") shouldBe true
    refuses("MAX 0 Heat: Tile<>", "MAX 0 Plant: GreeneryTile<Land1>")

    narrows("Tile<> / Heat", "GreeneryTile<Land1> / Heat") shouldBe true
    refuses("Tile<> / Heat", "GreeneryTile<Land1> / Plant")

    narrows("Tile<> BY Player1", "GreeneryTile<Land1> BY Player1") shouldBe true
    refuses("Tile<> BY Player1", "GreeneryTile<Land1> BY Player2")

    narrows("EACH Player { Tile<> }", "EACH Player { GreeneryTile<Land1> }") shouldBe true
    refuses("EACH Player { Tile<> }", "EACH Area { GreeneryTile<Land1> }")
  }

  // L7-6 OR

  @Test
  internal fun `L7-6 a proposal narrows an OR by narrowing any one arm`() {
    narrows("Plant! OR Heat!", "Plant!") shouldBe true
    narrows("Plant! OR Heat!", "Heat!") shouldBe true
    refuses("Plant! OR Heat!", "Steel!")
  }

  @Test
  internal fun `L7-6 an OR narrows an OR only when every arm does`() {
    narrows("Tile<>! OR Plant!", "GreeneryTile<Land1>! OR Plant!") shouldBe true
    refuses("Tile<>! OR Plant!", "GreeneryTile<Land1>! OR Steel!")
  }

  // L7-7 X

  @Test
  internal fun `L7-7 X takes one value everywhere, scaled by each coefficient`() {
    narrows("X Plant THEN X Heat", "3 Plant THEN 3 Heat") shouldBe true
    narrows("X Plant THEN 2X Heat", "3 Plant THEN 6 Heat") shouldBe true
    refuses("X Plant THEN 2X Heat", "3 Plant THEN 5 Heat")
    refuses("2X Plant THEN Heat", "3 Plant THEN Heat")
  }

  // L7-8 Shared type variables

  @Test
  internal fun `L7-8 a repeated abstract expression takes one value everywhere`() {
    narrows("Token THEN Token", "RedToken THEN RedToken") shouldBe true
    refuses("Token THEN Token", "RedToken THEN BlueToken")

    narrows(
        "Tile<LandArea> THEN Tile<LandArea>",
        "GreeneryTile<Land1> THEN GreeneryTile<Land1>",
    ) shouldBe true
    refuses("Tile<LandArea> THEN Tile<LandArea>", "GreeneryTile<Land1> THEN OceanTile<Land1>")
  }

  // L7-9 The two spellings

  @Test
  internal fun `L7-9 narrows answers and ensureNarrows explains`() {
    narrows("2 Plant!", "3 Plant!") shouldBe false
    refuses("2 Plant!", "3 Plant!").message!!.contains("does not narrow") shouldBe true
  }

  // L7-10 Groups

  @Test
  internal fun `L7-10 groups narrow elementwise and by position`() {
    narrows("Tile<>, Plant", "GreeneryTile<Land1>, Plant") shouldBe true
    refuses("Tile<>, Plant", "Plant, GreeneryTile<Land1>")
  }
}
