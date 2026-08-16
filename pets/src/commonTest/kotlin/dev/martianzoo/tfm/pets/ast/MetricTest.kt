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
      Foo
      !Abc
      3 Abc
      Wau<Bar>
      PROD[Abc]
      Foo MAX 11
      3 PROD[Abc]
      2 Foo OR Bar
      2 (Eep MAX 5)
      !Eep(HAS? Bar)
      PROD[Abc MAX 5]
      PROD[3 Abc<Foo>]
      PROD[2 Qux MAX 5]
      2 (2 (3 Qux<Foo>))
      2 Abc<Foo> OR 2 Xyz
      PROD[3 (Xyz MAX 11)]
      PROD[PROD[Foo] MAX 5]
      PROD[Xyz MAX 5] MAX 11
      PROD[PROD[2 Qux] MAX 5]
      3 (Abc<Abc<Abc>> MAX 11)
      PROD[PROD[2 (Foo MAX 5)]]
      3 (3 (2 PROD[Foo MAX 11]))
      2 PROD[2 (Foo MAX 5)] MAX 5
      PROD[2 (2 (2 (Bar MAX 11)))]
      Foo<Wau, !Abc>(HAS MAX 0 Bar)
      Wau<Xyz, Qux>(HAS? Bar OR Qux)
      PROD[PROD[Foo<Bar<Bar>>] MAX 5]
      PROD[PROD[2 PROD[2 Foo]] OR Foo]
      Xyz<Bar<Qux<Bar<Bar>>, Bar<Qux>>>
      PROD[Ooh<Qux, Foo, Bar<Bar<Qux>>>]
      Foo OR 2 (2 Bar) MAX 5 OR PROD[Abc]
      PROD[Foo] OR 2 Abc OR PROD[Abc<Ahh>]
      PROD[Abc] OR PROD[2 (2 Qux)] OR 3 Ooh
      2 Qux<Ooh> OR PROD[2 Bar] OR Bar MAX 5
      Abc OR PROD[Qux] OR 2 Foo OR 3 Abc<Foo>
      !Abc OR PROD[Foo MAX 11] OR Qux OR 2 Abc
      Abc<Eep<Ahh<Ooh>>, !Xyz, Wau<Qux>(HAS 1)>
      PROD[Bar OR PROD[Abc] OR Bar OR 3 (3 Xyz)]
      Bar<Ahh> OR PROD[Ooh<Abc, Bar> OR Xyz<Qux>]
      PROD[PROD[2 Foo] OR Qux<Abc<Abc, Foo<Bar>>>]
      2 (2 (2 Qux)) OR Xyz<Qux, !Abc> OR Ooh OR Bar
      2 (Foo MAX 5) MAX 11 OR PROD[PROD[Xyz]] OR Xyz
      2 (Foo MAX 5) OR 2 Bar<Abc> OR PROD[Foo] OR Xyz
      PROD[Foo<Abc<Abc, Foo<Bar>>(HAS =1 Megacredit)>]
      (2 Abc MAX 5 OR Abc OR 2 (2 Foo) OR 3 Ooh) MAX 11
      !Xyz<Xyz<!Xyz<Wau>>, Ooh<Qux<Qux>>, Eep<Abc, Xyz>>
      PROD[2 PROD[Qux] OR 2 Abc OR Qux<Ahh> OR PROD[Ooh]]
      PROD[Wau<Foo<Foo, Abc<Foo>, Bar>, !Foo(HAS 1), Foo>]
      2 Bar OR Foo OR 3 (2 (2 Qux)) OR 3 (2 (2 Foo) OR Foo)
      Xyz<Abc<Qux<Wau<Qux, Xyz>(HAS MAX 0 Qux)>, !Xyz, Wau>>
      3 (2 (2 (2 Bar)) OR Ooh<Bar> OR PROD[PROD[Qux]] OR Foo)
      2 (2 (Bar MAX 11) OR 3 Xyz MAX 5) OR PROD[2 (Foo MAX 5)]
      PROD[2 Foo OR Bar<Ooh, Qux, Xyz> OR 2 Xyz OR 2 Foo MAX 5]
      Wau<Bar<Bar>, Bar<!Ahh<Foo, Bar>>, Qux<Qux, Ooh>>(HAS Foo)
      3 (2 Foo) OR Xyz<Qux<Abc>(HAS Bar)> OR Ooh OR 2 PROD[2 Bar]
      Bar<Bar<Qux, Xyz<Bar>>, Ahh(HAS 2 OR 1 OR (1 OR Foo))> MAX 5
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

    Metric.scaled(count, 1) shouldBe count
    parse<Metric>("1 Foo") shouldBe count
  }
}
