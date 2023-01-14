package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parsing.parsePets
import dev.martianzoo.tfm.pets.testSampleStrings
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class EffectTest {

  val inputs = """
    Ooh: 1
    -Ahh: 1
    Qux: -1!
    Eep: Qux?
    -Ahh: -11!
    Ahh: -5 Wau
    PROD[Ahh]: 1
    -Abc: -11 Ahh
    Wau<Qux>: Bar!
    -Abc<Foo>: Foo!
    Eep: 11 Bar<Xyz>
    -Eep<Wau>: 1, Bar
    Eep<Foo>: PROD[1!]
    PROD[Qux]: 1. / Bar
    -Abc<Bar>: PROD[Abc]
    -Eep: 11 Foo FROM Foo
    -Qux<Ooh, Qux>: -5 Abc
    -Qux<Ooh(HAS 1)>: -Ooh!
    Ooh<Eep<Wau, Foo>>: Xyz!
    Ooh: PROD[1 / Megacredit]
    Foo(HAS 0 Bar): PROD[-Eep]
    -Eep: -11., 1 Ooh FROM Qux.
    -Ooh<Ahh>: 1 / 11 Megacredit
    PROD[-Ahh]: Ahh / 11 Qux<Eep>
    -Eep(HAS Abc): 5 Bar<Foo<Bar>>
    PROD[-Foo]: Foo, 1 Qux FROM Ahh
    Ahh<Foo<Foo>(HAS 11, 1)>: 11 Abc
    Wau<Abc<Foo, Qux, Bar<Wau>>>: Ahh
    Eep: 11 Xyz, -Qux! OR (Foo OR Xyz)
    PROD[-Ooh]: -Bar! / 5 Abc<Ooh>, Abc
    PROD[Xyz]: 5 Xyz THEN 1 Xyz FROM Qux
    Xyz<Xyz>: PROD[(1 Abc FROM Qux) OR 1]
    Bar: PROD[(Foo: -1, -1), 1, 5 Abc, 1.]
    PROD[Abc<Qux>]: PROD[Foo / Bar OR -Abc]
    Xyz: -Qux<Abc<Abc>, Foo> / Abc<Bar, Xyz>
    PROD[Qux]: 11 Bar?, Foo., ${'$'}name(Xyz)
    -Xyz: (-Bar<Bar>, -1) OR (11 Qux FROM Eep)
    Ooh<Qux<Bar, Ahh>, Xyz>: -5 Ooh<Foo(HAS 1)>
    Eep: 5 Bar: ${'$'}name(Foo), 5 Xyz: Foo<Xyz>
    -Foo: Xyz, 1 OR (-Qux / Megacredit, Bar<Ooh>)
    PROD[-Ahh<Bar>]: -Wau<Foo<Xyz<Bar, Xyz>, Bar>>
    PROD[-Qux]: PROD[1 Foo<Xyz FROM Bar, Xyz>, Bar]
    PROD[-Foo]: -1, (Xyz?, ${'$'}name(Foo) OR 5 Bar)
    Qux<Foo>: 1 Abc FROM Ahh<Xyz<Ooh>>, 1 OR (Abc: 1)
    PROD[-Ooh<Ahh>]: PROD[Abc / Megacredit, Qux OR -5]
    PROD[Eep]: Qux, (5, Foo) OR 1, 1 Ahh<Qux> FROM Xyz!
    Abc<Wau<Ahh, Bar>, Ooh<Foo>>: 5 Qux, Ooh OR (Bar: 1)
    -Wau: 1, Abc, (-5 Bar / 5 Bar<Abc<Abc>>, Wau), (1, 1)
    PROD[Xyz(HAS 0 OR 0 Abc)]: Bar? / 5 Abc<Ooh<Bar, Ooh>>
    -Ahh: 1 Bar<Foo> FROM Xyz / Foo, (Abc, ${'$'}name(Ahh))
    PROD[Ooh<Foo<Qux, Ahh>>]: PROD[1 Bar<Qux, Bar FROM Wau>]
    PROD[Bar<Eep>]: 11 Ahh<Ooh<Qux<Ooh, Eep FROM Qux>>, Bar>!
    PROD[Ahh]: 5, Wau, Ahh<Foo<Bar>>, PROD[Bar OR -1, 1 / Abc]
    PROD[Abc<Abc<Abc>>]: (1 Abc<Qux, Bar FROM Ahh>, 5 Xyz), Qux
    PROD[Bar<Eep<Ahh<Ooh>>>]: (MAX 1 Megacredit: 1 Bar FROM Foo)
  """.trimIndent()

  @Test
  fun testSampleStrings() {
    val pass = testSampleStrings<Effect>(inputs)
    assertThat(pass).isTrue()
  }

  @Test
  fun nodeCount() {
    val eff: Effect = parsePets("Xyz<Xyz>: PROD[(1 Abc FROM Qux) OR 1]")
    // ef, og, te, cn, te, cn, pr, or, tr, fr, te, cn, te, cn, ga, qe, te, cn
    assertThat(eff.nodeCount()).isEqualTo(18)
  }
}
