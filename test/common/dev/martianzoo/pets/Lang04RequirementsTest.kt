package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.And
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.pets.ast.Requirement.Exact
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.Requirement.Or
import dev.martianzoo.pets.types.inferTypeVariables
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 4 of `docs/pets-language-spec.md`: requirements as queries over one state. */
internal class Lang04RequirementsTest {

  /** Answers a requirement from a table of component counts, and nothing else. */
  private fun ask(source: String, vararg counts: Pair<String, Int>): Boolean {
    val table = counts.toMap()
    return parse<Requirement>(source).isMetBy { metric ->
      metric.evaluate(
          count = { table[it.expression.toString()] ?: 0 },
          readProperty = { error("no properties here: $it") },
          countUnion = { or -> or.metrics.sumOf { table[it.expression.toString()] ?: 0 } },
          rank = { error("no ranks here: $it") },
      )
    }
  }

  // L4-1 Evaluation

  @Test
  internal fun `L4-1 a requirement is answered from the values of the metrics it names`() {
    val asked = mutableListOf<String>()
    parse<Requirement>("3 Plant, MAX 1 Steel").isMetBy { metric ->
      asked += "$metric"
      5
    } shouldBe false

    asked shouldBe listOf("Plant", "Steel")
  }

  // L4-2 The three counting forms

  @Test
  internal fun `L4-2 minimum, maximum and exact`() {
    ask("3 Plant", "Plant" to 2) shouldBe false
    ask("3 Plant", "Plant" to 3) shouldBe true
    ask("3 Plant", "Plant" to 9) shouldBe true

    ask("MAX 3 Plant", "Plant" to 3) shouldBe true
    ask("MAX 3 Plant", "Plant" to 4) shouldBe false

    ask("=3 Plant", "Plant" to 3) shouldBe true
    ask("=3 Plant", "Plant" to 2) shouldBe false
    ask("=3 Plant", "Plant" to 4) shouldBe false
  }

  @Test
  internal fun `L4-2 an omitted count is one`() {
    parse<Requirement>("Plant") shouldBe Min(1, Metric.Count(parse("Plant")))
    ask("Plant") shouldBe false
    ask("Plant", "Plant" to 1) shouldBe true
    ask("MAX 0 Tile", "Tile" to 1) shouldBe false
    ask("MAX 0 Tile") shouldBe true
  }

  // L4-3 Zero minimum

  @Test
  internal fun `L4-3 a minimum of zero is rejected, but MAX 0 and =0 are not`() {
    shouldThrow<PetSyntaxException> { parse<Requirement>("0 Plant") }
    (parse<Requirement>("MAX 0 Plant") as Counting).range shouldBe 0..0
    (parse<Requirement>("=0 Plant") as Counting).range shouldBe 0..0
  }

  // L4-4 The target is not the metric's scaling

  @Test
  internal fun `L4-4 the target is independent of the metric's own scaling`() {
    val requirement = parse<Requirement>("MAX 2 (3 Plant)") as Max

    requirement.maximum shouldBe 2
    requirement.metric shouldBe parse<Metric>("3 Plant")
    ask("MAX 2 (3 Plant)", "Plant" to 6) shouldBe true
    ask("MAX 2 (3 Plant)", "Plant" to 8) shouldBe true
    ask("MAX 2 (3 Plant)", "Plant" to 9) shouldBe false
  }

  @Test
  internal fun `L4-4 a plain count of n does not become a scaled metric`() {
    parse<Requirement>("8 Plant") shouldBe Min(8, Metric.Count(parse("Plant")))
    parse<Requirement>("MAX 8 Plant") shouldBe Max(8, Metric.Count(parse("Plant")))
    parse<Requirement>("=8 Plant") shouldBe Exact(8, Metric.Count(parse("Plant")))
  }

  // L4-5 One metric atom

  @Test
  internal fun `L4-5 a counted union or subtraction must be parenthesized`() {
    (parse<Requirement>("9 (Plant - Steel)") as Counting).metric shouldBe
        parse<Metric>("Plant - Steel")
    (parse<Requirement>("2 (Plant OR Steel)") as Counting).metric shouldBe
        parse<Metric>("Plant OR Steel")
    shouldThrow<PetSyntaxException> { parse<Requirement>("9 Plant - Steel") }
    ask("2 (Plant OR Steel)", "Plant" to 1, "Steel" to 1) shouldBe true
  }

  // L4-6 Conjunction, disjunction, precedence

  @Test
  internal fun `L4-6 comma is conjunction, OR is disjunction, and OR binds tighter`() {
    val requirement = parse<Requirement>("Plant, Steel OR Heat") as And

    requirement.requirements[0] shouldBe parse<Requirement>("Plant")
    (requirement.requirements[1] is Or) shouldBe true
    ask("Plant, Steel OR Heat", "Plant" to 1, "Heat" to 1) shouldBe true
    ask("Plant, Steel OR Heat", "Steel" to 1, "Heat" to 1) shouldBe false
    ask("(Plant, Steel) OR Heat", "Heat" to 1) shouldBe true
  }

  // L4-7 Alternatives are a set; conjuncts are a sequence

  @Test
  internal fun `L4-7 duplicate alternatives collapse and duplicate conjuncts do not`() {
    parse<Requirement>("Plant OR Plant") shouldBe parse<Requirement>("Plant")
    parse<Requirement>("Plant, Plant").toString() shouldBe "Plant, Plant"
    Or.create(listOf(parse("Plant"), parse("Plant"))) shouldBe parse<Requirement>("Plant")
    And.create(listOf(parse("Plant"))) shouldBe parse<Requirement>("Plant")
  }

  // L4-8 EVAL

  @Test
  internal fun `L4-8 an unexpanded EVAL has no value of its own`() {
    parse<Requirement>("EVAL Gardener.requirement").toString() shouldBe "EVAL Gardener.requirement"
    shouldThrow<IllegalStateException> {
      parse<Requirement>("EVAL Gardener.requirement").isMetBy { 0 }
    }
  }

  // L4-9 A requirement observes

  @Test
  internal fun `L4-9 nothing inside a requirement is an open choice`() {
    // An abstract expression repeated only inside requirements declares no variable to bind
    // (T13-8), so the two `Player` occurrences below stay independent filters.
    val effect =
        langTable
            .inferTypeVariables()
            .transformEffect(parse("Plant IF Plant<Player> : (MAX 0 Heat<Player>): Heat"))

    effect.typeVariables.isEmpty shouldBe true
  }

  // L4-10 Rendering

  @Test
  internal fun `L4-10 requirements round-trip`() {
    roundTripAll<Requirement>(
        """
        Qux
        5 MC
        11 MC
        5 Foo
        Plant
        =1 Foo
        5 Plant
        =11 Xyz
        5 MC, MC
        =1 Plant
        MAX 5 MC
        MAX 1 Abc
        MAX 11 Bar
        =1 Qux<Wau>
        PROD[11 MC]
        MAX 11 Plant
        PROD[=1 Bar]
        Plant, Steel
        =0 Xyz(HAS MC)
        PROD[11 Plant]
        Plant<Player1>
        MC OR MAX 1 Xyz
        PROD[=0 MC, MC]
        PROD[MAX 1 Qux]
        9 (Plant - Steel)
        PROD[=0 Abc<Foo>]
        MAX 5 MC OR 11 Qux
        Bar, Bar, MAX 1 Ooh
        5 Qux<Qux<Xyz, Foo>>
        EVAL Foo.requirement
        PROD[MAX 11 MC, Bar]
        Plant OR MAX 1 Steel
        (5 MC, Bar) OR 11 Abc
        Foo<Qux>, PROD[=0 MC]
        MAX 0 Plant(HAS Steel)
        PROD[Bar OR (Abc, Abc)]
        MAX 0 Ahh<Foo>, MAX 1 MC
        MAX 5 Xyz<Foo<Ooh<Foo>>>
        PROD[MAX 1 Bar] OR 11 MC
        Plant, Steel, MAX 1 Heat
        6 PROD[Steel OR Titanium]
        EVAL Gardener.requirement
        MAX 1 MC, Xyz OR Xyz<Foo>
        =1 (RANK Player { Score })
        PROD[11 Bar(HAS MAX 1 MC)]
        (5 Plant, Steel) OR 11 Heat
        PROD[=1 Qux<Qux, Bar, Abc>]
        PROD[=0 MC, Abc OR MAX 1 Abc]
        PROD[MAX 1 Plant] OR 11 Steel
        MAX 1 Abc<Qux, Ahh(HAS =0 MC)>
        PROD[Foo<Xyz> OR (5 MC, 11 MC)]
        PROD[MAX 1 MC, 5 Bar<Qux, Foo>]
        PROD[Qux<Foo>, MAX 11 Bar, Qux]
        ((MC, Foo), MC OR Foo), Wau<Foo>
        15 (ActiveCard OR AutomatedCard)
        =2 (RANK Player { Score, Cash })
        PROD[MC, (MAX 1 MC, Abc<Bar>, MC)]
        =5 Wau<Qux, Bar<Foo>, Ooh<Eep, Bar>>
        PROD[(MAX 0 MC, Qux<Foo>) OR =0 Bar]
        PROD[MAX 1 Bar], (Qux, Bar<Foo>, MC)
        MAX 1 Bar OR (MAX 0 MC, PROD[=0 Ahh])
        ((MC OR MAX 0 Foo) OR =1 MC, =0 Foo), MC
        MAX 5 MC, 5 Foo OR Qux OR MAX 1 Foo<Bar>
        PROD[=0 Abc OR ((MAX 1 MC, MC), =1 Bar)]
        Bar, 5 Foo, 11 Foo OR (MC, Abc<Abc>) OR Ahh
        PROD[MC OR ((MAX 0 Bar, Foo OR MC) OR Foo)]
        5 Abc OR (Qux OR Bar, Abc<Foo>) OR MC OR Bar
        (Qux, MAX 1 Foo OR Foo, (MC, MC)), =0 Xyz, =1 MC
        MAX 1 Bar OR 5 MC OR =5 MC OR MC, 5 Ooh<Qux>, MC
        (Foo, =11 Abc, MAX 11 MC), (Bar, (Xyz, MC) OR Foo)
        11 Foo, (MAX 1 Foo OR (Abc, MC)) OR PROD[Abc<Bar>]
        ((MAX 1 Qux OR MAX 1 MC OR MC) OR Bar) OR MAX 1 Eep
        MC OR Bar<Bar>, MC OR ((Foo OR MC) OR =1 MC), 11 MC
        Xyz<Qux, Xyz> OR (MC, MAX 1 Bar), PROD[11 Qux], Xyz
        ((Plant OR MAX 0 Steel) OR =1 Heat, =0 Steel), Plant
        MAX 1 Foo<Qux, Ooh>, (MC, MAX 1 Foo), PROD[MAX 1 MC]
        ((Bar, MAX 0 Foo<Xyz>) OR =5 Qux OR Foo) OR PROD[5 Qux]
        MAX 5 Foo<Abc, Foo> OR 11 MC OR PROD[Bar, 5 Qux] OR Xyz
        PROD[MAX 0 Xyz OR MAX 1 Foo OR 5 Ahh OR Ooh, MAX 0 Ahh OR MC]
        """
    )
  }

  @Test
  internal fun `L4-10 grouping is re-inserted wherever re-parsing needs it`() {
    roundTrip<Requirement>("(Plant OR Steel)", "Plant OR Steel")
    roundTrip<Requirement>("Plant, (Steel OR Heat)", "Plant, Steel OR Heat")
    roundTrip<Requirement>("(Plant, Steel) OR Heat")
    roundTrip<Requirement>("=6 (PROD[Steel OR Titanium])", "=6 PROD[Steel OR Titanium]")
  }
}
