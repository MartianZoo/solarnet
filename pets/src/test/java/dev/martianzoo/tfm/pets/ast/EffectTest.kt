package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parsing.parseAsIs
import dev.martianzoo.tfm.pets.testSampleStrings
import dev.martianzoo.tfm.testlib.PetGenerator
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class EffectTest {

  val inputs =
      """
    Ooh: 1
    Xyz: -1
    -Foo: 5!
    Abc: -Abc
    -Xyz: -Bar
    -Xyz:: -Abc
    Eep: PROD[1]
    Qux: -11 Eep!
    PROD[Ahh]: Foo
    Ahh: @name(Wau)
    PROD[Ahh]: -Qux?
    Qux:: PROD[5 Foo]
    PROD[Xyz]: PROD[1]
    -Ooh<Xyz, Bar>:: -1
    Ooh: PROD[PROD[Foo]]
    PROD[Bar]: PROD[-Bar]
    PROD[Xyz]:: PROD[-Bar]
    PROD[Ooh]: -Foo, 11 Bar
    -Foo<Bar, Bar, Abc>: Bar
    PROD[Abc]: Ooh FROM Xyz
    -Ooh: @name(Qux<Qux<Bar>>)
    -Ooh: (Qux FROM Bar) OR 5
    Xyz<Ooh, Bar, Eep>: PROD[-5]
    Xyz: PROD[Qux<Foo, Abc>: Foo]
    Wau: Bar(HAS 5 Qux) FROM Ahh
    Ooh:: 1 / Abc, 11 Ooh<Foo<Foo>>
    -Qux: Xyz / 5 Foo(HAS MAX 1 Qux)
    Abc<Foo>:: 5 Foo FROM Abc<Foo>, 5
    Ooh<Bar>:: 11 Foo, 5 Foo<Bar<Foo>>
    -Bar(HAS Bar): -Eep<Qux, Bar<Bar>>.
    -Qux<Ooh<Foo>>(HAS 5 OR Bar): -5 Ooh
    PROD[Qux]: (1: (1 / 5 Foo, Foo<Xyz>))
    PROD[-Ahh]: PROD[-Qux / 11 Megacredit]
    Eep: (1 THEN 1) OR (-Qux, 1, -5 Foo, 1)
    Qux:: Bar FROM Bar / Bar<Qux>, -5 Qux?
    -Ooh: Ooh, (5 Abc<Foo>, 1: -1), 5 Foo!, 1
    Eep<Abc>:: 11 Ahh<Foo>, @name(Qux) OR -Abc
    Foo: PROD[5. OR (=1 Megacredit: (-1 OR 1))]
    -Foo<Ooh<Abc>>: 11 Xyz, 1, -Foo!, @name(Ooh)
    Xyz<Xyz>: Xyz FROM Abc / Xyz<Xyz<Bar>, Bar>
    PROD[Abc]: Ooh OR (1 THEN Foo.), -11, Foo, Ooh
    PROD[-Foo]: Qux: Qux / 5 Bar, Qux OR @name(Abc)
    -Ooh<Foo<Ahh>>(HAS 1 OR (1 OR Foo)): Bar, -5 Ooh
    -Eep<Foo, Ooh<Foo>>: ((-1, -Abc), 1!) OR Abc<Qux>
    PROD[-Abc]: (Xyz OR MAX 0 Qux): -1 / Ooh<Foo>, Foo
    PROD[Ooh<Ooh>]: PROD[Abc / Qux, -Ooh OR @name(Foo)]
    Eep: (1, 1 OR (Foo: @name(Foo))) OR (@name(Bar), -1)
    -Wau<Bar<Foo>>: -5, 1. / 11 Abc, 5 Abc FROM Foo / Ooh
    -Foo: PROD[5 Abc], (-1 THEN 1) OR (Bar OR (1: 1)), Ahh
    Qux<Abc<Qux>, Qux>:: (1, 1 OR Foo) OR (Abc FROM Abc.)
    Foo(HAS 11 Foo): -1 / 5 Abc<Bar>, @name(Abc), -Abc<Qux>.
    -Ahh<Xyz(HAS =0 Megacredit), Foo<Abc>>: 5 / 11 Megacredit
    -Xyz: (1 / Bar, 1 / Wau), (1, 1 / Foo OR Qux OR (1: -Bar))
    -Bar<Foo<Ahh, Foo<Foo>>, Foo, Qux<Qux<Qux>, Foo>>:: PROD[1]
    -Abc<Qux>: ((1 OR (MAX 1 Megacredit OR 1)): (Bar, 1 OR Qux))
  """
          .trimIndent()

  @Test
  fun testSampleStrings() {
    testSampleStrings<Effect>(inputs)
  }

  @Test
  fun nodeCount() {
    val eff: Effect = parseAsIs("Xyz<Xyz>: PROD[(1 Abc FROM Qux) OR 1]")
    // ef, og, te, cn, te, cn, pr, or, tr, sc, fr, te, cn, te, cn, ga, ste, te, cn
    assertThat(eff.descendantCount()).isEqualTo(19)
  }

  // @Test
  fun genGrossApiCalls() {
    PetGenerator(0.95).generateTestApiConstructions<Effect>(20)
  }
}
