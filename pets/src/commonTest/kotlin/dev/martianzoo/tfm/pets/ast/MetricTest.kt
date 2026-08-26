package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.tfm.pets.testSampleStrings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MetricTest {
  private val inputs =
      """
      This
      Xyz
      !Ahh
      3 Bar
      Bar - 11
      PROD[Bar]
      Eep OR Abc
      2 (Qux - 5)
      2 Bar MAX 11
      Qux<Foo, Qux>
      EVAL Abc.score
      EVAL !Ahh.score
      Bar<Ooh> OR !Foo
      Eep(HAS Foo) - 11
      PROD[2 Abc MAX 11]
      Bar<Abc> MAX 11 - 3
      EVAL Eep<!Bar>.score
      PROD[PROD[PROD[Xyz]]]
      2 Bar - EVAL Ahh.score
      EVAL Bar<Qux>.score - 1
      2 (2 (2 Foo<Bar> - Qux))
      PROD[PROD[Foo - Abc]] - 5
      3 (3 (Bar - 2 Bar) MAX 11)
      PROD[PROD[Foo<Bar> OR Xyz]]
      PROD[PROD[2 (2 Bar) MAX 11]]
      (2 Foo - 2 (2 Bar)) MAX 5 - 3
      3 Foo<!Ahh<Foo, Abc>>(HAS Ooh)
      3 EVAL Foo<Bar<Ahh>, Foo>.score
      2 PROD[2 Foo MAX 5 - Foo] MAX 11
      3 Ooh - (2 (Foo MAX 5) - 3) MAX 5
      EVAL Eep<Qux<Abc, Bar>, Foo>.score
      Qux - PROD[PROD[Foo - (Foo - Qux)]]
      EVAL Xyz<!Qux<Xyz, Foo, !Ooh>>.score
      3 (2 Bar - Foo - Ooh) - 3 Qux - 3
      PROD[Abc<Foo> MAX 5 - (Foo - Bar) - 1]
      3 (Foo - (2 Qux - 2 (2 Foo) MAX 5))
      2 (2 (3 Foo MAX 5)) - !Wau<Abc<Ooh>> - 1
      3 EVAL Ahh<Foo, Ooh, Xyz<Qux, Bar>>.score
      Xyz<Bar<Ahh<Ooh, Foo>>, Foo<Ahh>>(HAS Qux)
      (Bar - (Bar OR Xyz OR Qux<Abc<Foo>>)) MAX 5
      EVAL Xyz<Eep<Xyz>(HAS 2 Qux)>(HAS Abc).score
      Abc<Abc<Abc<Qux>>, Bar>(HAS Bar<Bar>) OR !Ahh
      Qux<Eep> OR Eep OR Wau<Qux(HAS Xyz, MC)> OR Wau
      EVAL Xyz<Eep>(HAS PROD[Abc OR MAX 1 Foo]).score
      Ooh<Abc<Ooh(HAS MAX 0 Foo OR MC), Xyz>, Abc<Ahh>>
      !Bar<Eep<Bar>(HAS MAX 1 Foo)>(HAS MC, =1 Foo<Foo>)
      PROD[3 (Qux - (Foo<Foo> - Bar<Foo> MAX 5)) MAX 11]
      Foo<Wau> OR Abc<Ahh(HAS Bar)>(HAS MAX 0 MC)
      EVAL Ahh.score MAX 5 - (!Bar OR Bar<Qux>) - 11 - Bar
      PROD[2 (Foo - Abc - 2 (2 (2 Foo MAX 5)) - Foo) MAX 5]
      PROD[Xyz - Qux - PROD[Qux] - Foo - (2 Qux - Bar<Foo>)]
      Abc - PROD[Abc - PROD[Ahh]] - 11 - 2 (Foo MAX 5) MAX 11
      Ooh<!Wau, Bar<!Xyz<Xyz<Ooh, Xyz, Qux>, Abc<Foo>>, !Abc>>
      PROD[Abc MAX 11 - 11 - (2 (Foo MAX 5) - 2 (2 Foo)) MAX 5]
      PROD[Foo MAX 11 - Foo - (Bar OR Abc<Bar> OR Foo(HAS Bar))]
      EVAL Bar<Bar, Foo<!Xyz>, Abc<Foo>>(HAS =1 MC).score
      Eep<Foo(HAS PROD[MC, =1 Foo]), Bar<Xyz(HAS 2 Foo), Qux>, Eep>
      """
          .trimIndent()

  @Test
  internal fun testSampleStrings() {
    testSampleStrings<Metric>(inputs)
  }

  @Test
  internal fun subtractionIsLeftAssociativeAndPreservesNecessaryGrouping() {
    parse<Metric>("Foo - Bar - Qux").toString() shouldBe "Foo - Bar - Qux"
    parse<Metric>("Foo - (Bar - Qux)").toString() shouldBe "Foo - (Bar - Qux)"
  }

  @Test
  internal fun subtractionEvaluationRemainsNonnegative() {
    val counts = mapOf("Ore" to 12, "Fleet" to 3)
    fun evaluate(text: String): Int =
        parse<Metric>(text)
            .evaluate(
                { counts[it.expression.toString()] ?: 0 },
                { error("no properties") },
                { error("no unions") },
            )

    evaluate("Ore MAX 5 - Fleet") shouldBe 2
    evaluate("2 (Ore - 3) MAX 4") shouldBe 4
    evaluate("Ore - (Fleet - 2)") shouldBe 11
    evaluate("Ore - 20") shouldBe 0
  }

  @Test
  internal fun orAcceptsOnlyDistinctComponentCounts() {
    shouldThrow<PetSyntaxException> { parse<Metric>("Foo MAX 5 OR Bar") }
    shouldThrow<PetSyntaxException> { parse<Metric>("Foo - Bar OR Qux") }
    shouldThrow<PetSyntaxException> { parse<Metric>("Foo OR Foo") }

    val foo = Metric.Count(cn("Foo").expression)
    val bar = Metric.Count(cn("Bar").expression)
    Metric.Or.create(listOf(foo, bar, foo)).toString() shouldBe "Foo OR Bar"
  }

  @Test
  internal fun constantsAreMetricValuesAndMinuends() {
    shouldThrow<PetSyntaxException> { parse<Metric>("2 5") }
    shouldThrow<PetSyntaxException> { parse<Metric>("0 Foo") }
    shouldThrow<PetSyntaxException> { parse<Metric>("Foo + Bar") }

    parse<Metric>("0").toString() shouldBe "0"
    parse<Metric>("5").toString() shouldBe "5"
    parse<Metric>("PROD[5]").toString() shouldBe "PROD[5]"
    parse<Metric>("Foo - 5").toString() shouldBe "Foo - 5"
    parse<Metric>("1 - Foo").toString() shouldBe "1 - Foo"
    parse<Metric>("3 (11 - Foo)").toString() shouldBe "3 (11 - Foo)"
  }

  @Test
  internal fun unitScalingIsCanonicalizedAway() {
    val count = Metric.Count(cn("Foo").expression)

    Metric.scaled(count, 1) shouldBe count
    parse<Metric>("1 Foo") shouldBe count
  }

  @Test
  internal fun unexpandedEvalIsAProgrammerError() {
    shouldThrow<IllegalStateException> {
      parse<Metric>("EVAL Foo.score").evaluate({ 0 }, { 0 }, { 0 })
    }
  }
}
