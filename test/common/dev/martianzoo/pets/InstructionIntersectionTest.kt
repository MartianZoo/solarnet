package dev.martianzoo.pets

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class InstructionIntersectionTest {
  private fun intersection(left: String, right: String, expected: String?) {
    val a = elaborate(left)
    val b = elaborate(right)
    val result = expected?.let(::elaborate)
    a.intersect(b, langTable, langWorld) shouldBe result
    b.intersect(a, langTable, langWorld) shouldBe result
  }

  @Test
  fun `types and dependencies contribute independent constraints`() {
    intersection("Token<Player1>", "RedToken<Anyone>", "RedToken<Player1>")
    intersection("Tile<Land1>", "GreeneryTile<Area>", "GreeneryTile<Player1, Land1>")
    intersection("Token<Player1>", "Token<Player2>", null)
  }

  @Test
  fun `intersection preserves fixed counts and optional ceilings`() {
    intersection("2 Token<Player1>?", "3 RedToken<Anyone>?", "2 RedToken<Player1>?")
    intersection("2 Token<Player1>!", "3 RedToken<Anyone>?", "2 RedToken<Player1>!")
    intersection("3 Token<Player1>!", "2 RedToken<Anyone>?", null)
    intersection("X Token<Player1>!", "3 RedToken<Anyone>!", "3 RedToken<Player1>!")
    intersection("2 Token<Player1>!", "2 RedToken<Anyone>.", null)
    intersection("RedToken?", "BlueToken?", "Ok")
  }

  @Test
  fun `alternatives intersect without inventing a choice`() {
    intersection("Token<Player1> OR Heat", "RedToken<Anyone> OR Steel", "RedToken<Player1>")
    intersection("RedToken OR BlueToken", "Token", "RedToken OR BlueToken")
    intersection("Token", "Heat OR Steel", null)
  }

  @Test
  fun `groups and sequences intersect by position`() {
    intersection("Token<Player1>, Heat", "RedToken<Anyone>, Heat", "RedToken<Player1>, Heat")
    intersection(
        "Token<Player1> THEN Heat",
        "RedToken<Anyone> THEN Heat",
        "RedToken<Player1> THEN Heat",
    )
    intersection("Token THEN Heat", "RedToken THEN Heat THEN Steel", null)
    intersection("Token, Heat", "Heat, RedToken", null)
  }

  @Test
  fun `fixed wrappers cannot be weakened by intersection`() {
    intersection("Heat: Token<Player1>", "Heat: RedToken<Anyone>", "Heat: RedToken<Player1>")
    intersection("Heat: Token", "Steel: RedToken", null)
    intersection("Token / Heat", "RedToken / Steel", null)
    intersection("Token<Player1> / Heat", "RedToken<Anyone> / Heat", "RedToken<Player1> / Heat")
    intersection("Token BY Player1", "RedToken BY Player2", null)
  }

  @Test
  fun `shared choices cannot become contradictory`() {
    intersection(
        "Token<Chosen@Player> THEN Heat<Chosen@Player>",
        "RedToken<Player1> THEN Heat<Player2>",
        null,
    )
    intersection("X Token THEN X Heat", "2 RedToken THEN 3 Heat", null)
    intersection(
        "Token<Chosen@Player> FROM Token<Chosen@Player>",
        "RedToken<Player1> FROM BlueToken<Player2>",
        null,
    )
  }

  @Test
  fun `a shared choice propagates constraints from either occurrence`() {
    intersection(
        "Token<Chosen@Player> THEN Heat<Chosen@Player>",
        "RedToken<Anyone> THEN Heat<Player1>",
        "RedToken<Player1> THEN Heat<Player1>",
    )
    intersection(
        "Token<Chosen@Player> FROM Token<Chosen@Player>",
        "RedToken<Anyone> FROM BlueToken<Player1>",
        "RedToken<Player1> FROM BlueToken<Player1>",
    )
    intersection("X Token THEN X Heat", "2 RedToken THEN X Heat", "2 RedToken THEN 2 Heat")
  }

  @Test
  fun `structurally excluded dependencies have no intersection`() {
    intersection("Token<Player1>", "RedToken<Anyone(NOT Player1)>", null)
  }

  @Test
  fun `intersection retains both refinements on an unsettled dependency`() {
    intersection(
        "Token<Player(HAS Plant)>",
        "RedToken<Player(HAS Heat)>",
        "RedToken<Player(HAS Plant, HAS Heat)>",
    )
  }

  @Test
  fun `a concrete dependency must satisfy inherited predicates`() {
    val task = elaborate("Token<Player(HAS Plant)>")
    val input = elaborate("RedToken<Player1>")

    task.intersect(input, langTable, TableWorld(langTable, answer = false)) shouldBe null
    task.intersect(input, langTable, langWorld) shouldBe input
  }

  @Test
  fun `independent amounts in a continuation are not one shared X`() {
    intersection(
        "Token<Player1> THEN (X Heat, X Steel)",
        "RedToken<Anyone> THEN (2 Heat, 3 Steel)",
        "RedToken<Player1> THEN (2 Heat, 3 Steel)",
    )
  }

  @Test
  fun `an unresolved shared choice cannot be mistaken for declining a change`() {
    val task = elaborate("Token<Chosen@Player> FROM Token<Chosen@Player>?")
    val input = elaborate("RedToken<Anyone> FROM BlueToken<Anyone>?")

    shouldThrow<NarrowingException> { task.intersect(input, langTable, langWorld) }
        .message
        .shouldContain("more specific choice")
  }
}
