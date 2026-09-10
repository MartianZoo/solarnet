package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.PropertyName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 5 of `docs/pets-language-spec.md`: metrics as numbers computed from one state. */
internal class Lang05MetricsTest {

  /** Evaluates a metric from a table of component counts, and nothing else. */
  private fun value(source: String, vararg counts: Pair<String, Int>): Int {
    val table = counts.toMap()
    return parse<Metric>(source)
        .evaluate(
            count = { table[it.expression.toString()] ?: 0 },
            readProperty = { error("no properties here: $it") },
            countUnion = { or ->
              or.metrics.maxOfOrNull { table[it.expression.toString()] ?: 0 } ?: 0
            },
            rank = { error("no ranks here: $it") },
        )
  }

  // L5-1 Only counting is world-dependent

  @Test
  internal fun `L5-1 scaling, capping and subtraction come from the syntax alone`() {
    val asked = mutableListOf<String>()
    parse<Metric>("3 Plant MAX 4 - 2")
        .evaluate(
            count = {
              asked += "${it.expression}"
              21
            },
            readProperty = { error("unused") },
            countUnion = { error("unused") },
            rank = { error("unused") },
        ) shouldBe 2

    asked shouldBe listOf("Plant")
  }

  @Test
  internal fun `L5-1 a metric is never negative`() {
    value("Plant - 20", "Plant" to 3) shouldBe 0
    value("Plant - Steel - Heat", "Plant" to 7, "Steel" to 3, "Heat" to 9) shouldBe 0
  }

  // L5-2 Counts and constants

  @Test
  internal fun `L5-2 an expression counts components and a bare number is a constant`() {
    parse<Metric>("Plant") shouldBe Metric.Count(parse("Plant"))
    parse<Metric>("5") shouldBe Metric.Constant(5)
    value("Plant", "Plant" to 4) shouldBe 4
    value("5") shouldBe 5
    value("0") shouldBe 0
  }

  // L5-3 Complete groups

  @Test
  internal fun `L5-3 n M counts complete groups of n`() {
    value("3 Plant", "Plant" to 6) shouldBe 2
    value("3 Plant", "Plant" to 7) shouldBe 2
    value("3 Plant", "Plant" to 8) shouldBe 2
    value("3 Plant", "Plant" to 9) shouldBe 3
    value("3 Plant", "Plant" to 2) shouldBe 0
  }

  @Test
  internal fun `L5-3 a unit of one is dropped and a unit of zero is rejected`() {
    Metric.scaled(Metric.Count(parse("Plant")), 1) shouldBe Metric.Count(parse("Plant"))
    parse<Metric>("1 Plant") shouldBe Metric.Count(parse("Plant"))
    shouldThrow<PetSyntaxException> { parse<Metric>("0 Plant") }
  }

  // L5-4 MAX

  @Test
  internal fun `L5-4 MAX is the smaller of two values, and does not stack`() {
    value("Plant MAX Steel", "Plant" to 7, "Steel" to 3) shouldBe 3
    value("Plant MAX Steel", "Plant" to 2, "Steel" to 3) shouldBe 2
    value("Plant MAX 5", "Plant" to 9) shouldBe 5
    shouldThrow<PetSyntaxException> { parse<Metric>("Plant MAX 5 MAX 3") }
  }

  // L5-5 Subtraction

  @Test
  internal fun `L5-5 subtraction saturates at zero and is left-associative`() {
    value("Plant - Steel", "Plant" to 12, "Steel" to 3) shouldBe 9
    value("Plant - Steel - Heat", "Plant" to 12, "Steel" to 3, "Heat" to 2) shouldBe 7
    value("Plant - (Steel - Heat)", "Plant" to 12, "Steel" to 3, "Heat" to 2) shouldBe 11

    parse<Metric>("Plant - Steel - Heat").toString() shouldBe "Plant - Steel - Heat"
    parse<Metric>("Plant - (Steel - Heat)").toString() shouldBe "Plant - (Steel - Heat)"
  }

  // L5-6 Unions

  @Test
  internal fun `L5-6 OR counts a union of plain component counts`() {
    val union = parse<Metric>("Plant OR Steel") as Metric.Or

    union.metrics shouldBe listOf(Metric.Count(parse("Plant")), Metric.Count(parse("Steel")))
    shouldThrow<PetSyntaxException> { parse<Metric>("Plant MAX 5 OR Steel") }
    shouldThrow<PetSyntaxException> { parse<Metric>("Plant - Steel OR Heat") }
    shouldThrow<PetSyntaxException> { parse<Metric>("Plant OR Plant") }
    Metric.Or.create(
            listOf(
                Metric.Count(parse("Plant")),
                Metric.Count(parse("Steel")),
                Metric.Count(parse("Plant")),
            )
        )
        .toString() shouldBe "Plant OR Steel"
  }

  // L5-7 Precedence

  @Test
  internal fun `L5-7 scaling and MAX bind tighter than subtraction, which binds tighter than OR`() {
    value("Plant MAX 5 - Steel", "Plant" to 12, "Steel" to 3) shouldBe 2
    value("(Plant - Steel) MAX 5", "Plant" to 12, "Steel" to 3) shouldBe 5
    parse<Metric>("Plant MAX 5 - Steel").toString() shouldBe "Plant MAX 5 - Steel"
    parse<Metric>("(Plant - Steel) MAX 5").toString() shouldBe "(Plant - Steel) MAX 5"
    parse<Metric>("2 (Plant - Steel)").toString() shouldBe "2 (Plant - Steel)"
  }

  @Test
  internal fun `L5-7 after a slash a metric union must be grouped`() {
    parse<InstructionTree>("Heat / Plant MAX 5 - Steel").toString() shouldBe
        "Heat / Plant MAX 5 - Steel"
    parse<InstructionTree>("Heat / (Plant OR Steel)").toString() shouldBe "Heat / (Plant OR Steel)"
    parse<InstructionTree>("Heat / Plant OR Steel").toString() shouldBe "Heat / Plant OR Steel"
    (parse<InstructionTree>("Heat / Plant OR Steel") is Instruction.Or) shouldBe true
  }

  // L5-8 Properties

  @Test
  internal fun `L5-8 a property metric names a receiver, or takes one from its context`() {
    parse<Metric>("Gardener.score") shouldBe Property(PropertyName("score"), parse("Gardener"))
    parse<Metric>("score") shouldBe Property(PropertyName("score"), null)
    parse<Metric>("EVAL Gardener.score").toString() shouldBe "EVAL Gardener.score"
    shouldThrow<IllegalStateException> {
      parse<Metric>("EVAL Gardener.score").evaluate({ 0 }, { 0 }, { 0 }, { 0 })
    }
  }

  // L5-9 RANK

  /**
   * This module can pin `RANK`'s syntax and its scope — which expression names each candidate, and
   * that the selector's refinement is not part of that name. Ranking a live field is realized in
   * `test/common/dev/martianzoo/engine/RankMetricTest.kt`.
   */
  @Test
  internal fun `L5-9 RANK names a selector, a candidate expression and its metrics`() {
    val rank =
        parse<Metric>("RANK Player(NOT Player1) { Score<Player>, MC<Player> }") as Metric.Rank

    rank.selector shouldBe parse("Player(NOT Player1)")
    rank.selectorName shouldBe parse("Player")
    rank.metrics shouldBe listOf(parse<Metric>("Score<Player>"), parse<Metric>("MC<Player>"))
    rank.candidate shouldBe null
    shouldThrow<PetSyntaxException> { parse<Metric>("RANK Player { }") }
  }

  // L5-10 Rendering

  @Test
  internal fun `L5-10 metrics round-trip`() {
    roundTripAll<Metric>(
        """
        Xyz
        This
        3 Bar
        Plant
        3 Plant
        Bar - 11
        PROD[Bar]
        Eep OR Abc
        Plant - 11
        2 (Qux - 5)
        PROD[Plant]
        2 Bar MAX 11
        Foo(NOT Ahh)
        2 (Plant - 5)
        Heat OR Steel
        Qux<Foo, Qux>
        2 Plant MAX 11
        EVAL Abc.score
        Plant(NOT Steel)
        Eep(HAS Foo) - 11
        PROD[2 Abc MAX 11]
        Bar<Abc> MAX 11 - 3
        EVAL Gardener.score
        PROD[PROD[PROD[Xyz]]]
        Plant(HAS Steel) - 11
        2 Bar - EVAL Ahh.score
        Marker<Mars1, Player1>
        EVAL Bar<Qux>.score - 1
        EVAL Foo(NOT Ahh).score
        PROD[PROD[PROD[Plant]]]
        Plant<Steel> MAX 11 - 3
        2 (2 (2 Foo<Bar> - Qux))
        Bar<Ooh> OR Abc(NOT Foo)
        PROD[PROD[Foo - Abc]] - 5
        3 (3 (Bar - 2 Bar) MAX 11)
        PROD[PROD[Foo<Bar> OR Xyz]]
        EVAL Eep<Foo(NOT Bar)>.score
        PROD[PROD[2 (2 Bar) MAX 11]]
        (2 Foo - 2 (2 Bar)) MAX 5 - 3
        2 (2 (2 Plant<Steel> - Heat))
        RANK Player { Score<Player> }
        3 EVAL Foo<Bar<Ahh>, Foo>.score
        2 PROD[2 Foo MAX 5 - Foo] MAX 11
        3 (2 Bar - Foo - Ooh) - 3 Qux - 3
        3 Ooh - (2 (Foo MAX 5) - 3) MAX 5
        EVAL Eep<Qux<Abc, Bar>, Foo>.score
        3 (Foo - (2 Qux - 2 (2 Foo) MAX 5))
        Qux - PROD[PROD[Foo - (Foo - Qux)]]
        3 Plant - (2 (Plant MAX 5) - 3) MAX 5
        Foo(NOT Bar<Eep<Bar>(HAS MAX 1 Foo)>)
        3 Foo<Bar(NOT Ahh<Foo, Abc>)>(HAS Ooh)
        PROD[Abc<Foo> MAX 5 - (Foo - Bar) - 1]
        3 EVAL Ahh<Foo, Ooh, Xyz<Qux, Bar>>.score
        Xyz<Bar<Ahh<Ooh, Foo>>, Foo<Ahh>>(HAS Qux)
        (Bar - (Bar OR Xyz OR Qux<Abc<Foo>>)) MAX 5
        Foo<Wau> OR Abc<Ahh(HAS Bar)>(HAS MAX 0 MC)
        EVAL Xyz<Eep<Xyz>(HAS 2 Qux)>(HAS Abc).score
        RANK Player { 999 - TerraformRating<Player> }
        EVAL Xyz<Eep>(HAS PROD[Abc OR MAX 1 Foo]).score
        Qux<Eep> OR Eep OR Wau<Qux(HAS Xyz, HAS MC)> OR Wau
        (Plant - (Steel OR Heat OR Marker<Mars1>)) MAX 5
        2 (2 (3 Foo MAX 5)) - Bar(NOT Wau<Abc<Ooh>>) - 1
        RANK Player { VictoryPoint<Player>, MC<Player> }
        Ooh<Abc<Ooh(HAS MAX 0 Foo OR MC), Xyz>, Abc<Ahh>>
        PROD[3 (Qux - (Foo<Foo> - Bar<Foo> MAX 5)) MAX 11]
        EVAL Xyz<Bar(NOT Qux<Xyz, Foo, Abc(NOT Ooh)>)>.score
        Abc<Abc<Abc<Qux>>, Bar>(HAS Bar<Bar>) OR Foo(NOT Ahh)
        PROD[2 (Foo - Abc - 2 (2 (2 Foo MAX 5)) - Foo) MAX 5]
        PROD[Xyz - Qux - PROD[Qux] - Foo - (2 Qux - Bar<Foo>)]
        Abc - PROD[Abc - PROD[Ahh]] - 11 - 2 (Foo MAX 5) MAX 11
        PROD[Abc MAX 11 - 11 - (2 (Foo MAX 5) - 2 (2 Foo)) MAX 5]
        PROD[Foo MAX 11 - Foo - (Bar OR Abc<Bar> OR Foo(HAS Bar))]
        EVAL Bar<Bar, Foo<Bar(NOT Xyz)>, Abc<Foo>>(HAS =1 MC).score
        EVAL Ahh.score MAX 5 - (Foo(NOT Bar) OR Bar<Qux>) - 11 - Bar
        Eep<Foo(HAS PROD[MC, =1 Foo]), Bar<Xyz(HAS 2 Foo), Qux>, Eep>
        EVAL Gardener.score MAX 5 - (Plant(NOT Steel) OR Steel<Heat>) - 11 - Plant
        Ooh<Foo(NOT Wau), Bar<Foo(NOT Xyz<Xyz<Ooh, Xyz, Qux>, Abc<Foo>>), Foo(NOT Abc)>>
        """
    )
  }

  @Test
  internal fun `L5-10 a lone scalar is a metric only where nothing else follows`() {
    parse<Metric>("5").toString() shouldBe "5"
    parse<Metric>("1 - Plant").toString() shouldBe "1 - Plant"
    parse<Metric>("Plant - 5").toString() shouldBe "Plant - 5"
    shouldThrow<PetSyntaxException> { parse<Metric>("2 5") }
    shouldThrow<PetSyntaxException> { parse<Metric>("Plant + Steel") }
  }

  @Test
  internal fun `L5-10 a metric with a class name that shadows nothing still parses`() {
    parse<Metric>("Max").toString() shouldBe "Max"
    parse<Metric>("Marker<Mars1>").toString() shouldBe "Marker<Mars1>"
    cn("Max").expression.toString() shouldBe "Max"
  }
}
