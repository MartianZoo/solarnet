package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.tfm.pets.testSampleStrings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MetricTest {
  private val inputs =
      """
      Eep
      2 Abc
      Eep<Wau>
      PROD[Qux]
      Wau MAX 11
      PROD[2 Qux]
      2 Xyz MAX 11
      Eep<Wau<Xyz>>
      3 (3 Qux<Ahh>)
      PROD[2 (2 Qux)]
      PROD[Ooh] MAX 11
      3 (2 PROD[2 Bar])
      2 PROD[Ahh MAX 11]
      PROD[3 (Ooh MAX 5)]
      PROD[3 (2 Bar<Ahh>)]
      PROD[Foo<Xyz> OR Foo]
      PROD[PROD[2 Abc<Ooh>]]
      PROD[2 (2 (Abc MAX 5))]
      PROD[2 PROD[Foo] MAX 11]
      PROD[PROD[Bar(HAS? Foo)]]
      PROD[3 (2 Bar<Abc>)] MAX 5
      2 PROD[Abc(HAS Foo OR Qux)]
      Foo<Foo(HAS Qux), Foo> MAX 5
      2 (2 Qux) OR PROD[Bar] MAX 11
      PROD[(Ooh OR Qux<Qux>) MAX 11]
      PROD[Xyz<Wau<Ahh>, Ooh> MAX 11]
      PROD[PROD[2 (Bar MAX 5)]] MAX 11
      (PROD[2 Qux] OR 2 (2 Qux)) MAX 11
      PROD[PROD[2 ((Foo OR Foo) MAX 5)]]
      2 (2 (2 (Foo OR PROD[Foo] MAX 11)))
      2 Qux OR 3 (2 (2 (2 (2 Foo)))) MAX 5
      PROD[2 Xyz<Abc> OR Abc OR Foo OR Foo]
      Xyz<Abc<Ahh>, Xyz<Eep, Foo<Foo<Qux>>>>
      PROD[2 Abc OR 2 Xyz<Xyz> OR Bar MAX 11]
      Ahh<Qux<Qux>(HAS 11 Bar), Foo<Abc, Qux>>
      PROD[Xyz<Foo, Eep>(HAS MAX 0 Megacredit)]
      PROD[PROD[Ooh<Foo> OR 2 Qux MAX 5 OR Ooh]]
      2 Eep MAX 11 OR Foo OR Bar OR Ooh<Bar, Qux>
      PROD[Eep<Qux<Foo, Abc, Foo>, Qux<Foo, Qux>>]
      2 (2 (2 Qux)) OR Ahh<Eep> OR Foo OR PROD[Foo]
      Bar OR PROD[3 (Bar MAX 11)] OR 2 Abc<Ahh<Foo>>
      Bar<Qux<Bar>, Ooh(HAS =0 Bar OR =0 Megacredit)>
      PROD[Foo<Xyz<Xyz<Foo, Foo, Bar>>(HAS Qux), Xyz>]
      Ahh<Xyz, Abc, Xyz<Qux<Abc<Bar>>>> OR 2 Ahh OR Eep
      (Ahh OR Foo MAX 11 OR 2 PROD[2 (2 (2 Foo))]) MAX 5
      PROD[2 Bar MAX 11 OR (2 Bar OR 2 Qux) MAX 5 OR Xyz]
      PROD[Foo MAX 5 OR Qux OR 2 (Bar OR Foo OR Bar<Foo>)]
      PROD[2 Bar] OR PROD[2 Bar] OR PROD[Xyz MAX 11 OR Foo]
      PROD[Foo MAX 5 OR Abc] OR PROD[Xyz MAX 11] OR Ooh<Abc>
      Foo<Abc<Ahh, Ooh(HAS MAX 1 Megacredit)>(HAS MAX 0 Abc)>
      3 (PROD[Abc<Bar> MAX 11] OR Bar OR 3 (Bar MAX 5) OR Xyz)
      PROD[2 Xyz] OR Foo<Abc, Foo> OR 2 Bar OR PROD[Foo] MAX 11
      Qux<Foo<Qux<Xyz<Foo, Bar<Xyz<Eep<Xyz<Qux>>, Foo>>>, Ooh>>>
      Abc<Ahh<Abc, Bar<Foo>>>(HAS Ahh, MAX 0 Xyz OR 5 Foo) MAX 11
      PROD[Foo] OR 3 (2 (Qux OR Foo MAX 5 OR 2 Qux)) OR Ahh OR Foo
      """
          .trimIndent()

  @Test
  fun testSampleStrings() {
    testSampleStrings<Metric>(inputs)
  }

  @Test
  fun plusIsNotMetricSyntax() {
    shouldThrow<PetSyntaxException> { parse<Metric>("Steel + Titanium") }
  }

  @Test
  fun unitScalingIsCanonicalizedAway() {
    val count = Metric.Count(cn("Foo").expression)

    Metric.scaled(1, count) shouldBe count
    parse<Metric>("1 Foo") shouldBe count
  }
}
