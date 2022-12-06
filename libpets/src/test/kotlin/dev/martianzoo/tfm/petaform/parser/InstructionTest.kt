package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.Instruction.Companion.multi
import dev.martianzoo.tfm.petaform.api.Instruction.Gain
import dev.martianzoo.tfm.petaform.api.Instruction.Remove
import dev.martianzoo.tfm.petaform.api.QuantifiedExpression
import dev.martianzoo.tfm.petaform.parser.PetaformParser.Instructions
import dev.martianzoo.tfm.petaform.parser.PetaformParser.QEs
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import dev.martianzoo.tfm.petaform.parser.RandomGenerator.testRandom
import org.junit.jupiter.api.Test

class InstructionTest {
  val inputs = """
    Foo
    -Bat
    Foo!
    Baz.
    -Xyz.
    3 Xyz
    -3 Qux
    11 Abc
    3 Foo.
    -3 Baz.
    Xyz<Baz>?
    -3 Wau, Bar
    -3 Xyz<Abc>
    3 Baz / Xyz
    3 Qux<Bar>.
    11 Foo<Qux>!
    3 Bar, -Wau!
    Abc / 11 Bat
    Abc<Baz, Baz>
    1 Xyz FROM Abc
    11 Xyz: -3 Foo
    -Wau?, Wau, Xyz
    11 Foo FROM Xyz
    Wau(HAS 3 Wau)!
    -3 Bat<Xyz, Qux>
    3 Bar / Bar<Xyz>
    3 Baz(HAS 3 Foo)!
    -11 Foo / Abc<Bat>
    Qux. OR -Bat<Xyz>.
    1 Qux<Abc> FROM Foo
    11 Bat<Abc(HAS Baz)>
    3 Qux FROM Bar / Foo
    11 Wau(HAS MAX 1 Baz)
    MAX 1 Foo: -Bat<Abc>.
    -11 Foo(HAS MAX 1 Abc)
    Xyz<Xyz, Bar> THEN Xyz
    1 Abc<Xyz, Wau> FROM Bar
    1 Qux<Baz> FROM Xyz<Foo>
    3 Abc / 11 Bar<Xyz, Wau>
    Bar<Foo<Baz, Foo, Wau>>?
    (11 Foo: -11 Xyz) OR -Xyz
    Foo!, -Foo<Foo>, Bar, Wau
    Qux. / Abc<Bar>(HAS =3 Foo)
    Bat, Xyz / Abc(HAS 11 Xyz<Bat>)
    11 Wau FROM Foo<Baz, Foo> OR Wau
    3 Wau<Foo(HAS 11 Bat, MAX 1 Bat)>
    11 Qux!, 11 Xyz / 3 Bar OR Xyz / Baz
    11 Qux? / 11 Bar<Abc, Xyz<Qux>, Bat>
    Abc! OR Xyz<Bat<Wau, Baz>, Wau, Wau>.
    Abc<Bat, Bat<Xyz<Foo>, Qux, Abc<Xyz, Bat, Foo>>>
    1 Qux<Foo> FROM Abc(HAS MAX 1 Abc(HAS MAX 1 Baz))
    Qux<Wau, Bat, Wau>!, 3 Bat FROM Xyz<Wau, Abc, Abc>
    Xyz / 11 Xyz OR 1 Bar<Abc>! FROM Baz / 3 Xyz OR Abc
    Foo<Bat, Abc>(HAS 11 Foo): 1 Bar. FROM Wau THEN Baz
    Bar / Bat<Bar<Bat(HAS Bat(HAS Bar)), Qux>, Bat, Xyz>
    MAX 3 Bar: Qux? THEN MAX 1 Baz: (Foo OR 3 Xyz(HAS =3 Baz).)
    3 Qux: 3 Bar<Abc<Bat>> THEN 11 Foo<Bat, Baz, Foo<Bar>> FROM Foo
    3 Bat<Baz, Bar> THEN (Qux THEN (1 Wau. FROM Wau / Wau OR -Bar))
    (Wau, (Abc THEN (-Abc, 11 Abc)) THEN 3 Qux?) OR 11 Xyz OR -11 Baz
    (1 Abc? FROM Wau / 11 Foo, 11 Foo FROM Xyz OR -11 Bat / Qux) OR -Wau
    3 Xyz<Bar>: 3 Abc / 3 Foo<Bat, Abc, Xyz>, -3 Qux<Abc(HAS Wau)>, Abc!
    -Wau<Baz> OR 11 Baz OR 3 Xyz<Bar<Bat>, Bat<Qux>, Qux>(HAS Abc, 11 Bat)
    (Foo OR (MAX 3 Baz<Xyz>, 11 Abc)): 1 Xyz<Qux, Wau, Baz<Baz>> FROM Bat / Foo<Wau>
    Wau OR Qux<Abc, Baz, Xyz>. / 3 Qux OR Bar OR 3 Xyz?, Qux<Bat<Qux, Xyz>, Bat, Xyz>
    11 Abc<Foo, Bar<Baz<Xyz>, Foo>, Bar>?, Bat<Bar, Xyz<Xyz<Abc>, Baz(HAS 3 Bar), Baz>>!
    Abc, 11 Foo<Foo, Wau<Xyz>, Bat(HAS Bat)>. / 3 Wau, (MAX 11 Xyz OR =3 Baz): Bat. / 3 Bar
  """.trimIndent().split('\n')

  @Test fun test() {
    assertThat(inputs.filterNot { checkRoundTrip(it) }).isEmpty()
  }

  @Test
  fun testStrangeCases() {
    testRoundTrip("1")
    testRoundTrip("0")
  }

  @Test fun random() {
    for (i in 1..10000) {
      testRandom<Instruction>()
    }
  }

  private fun testRoundTrip(start: String, end: String = start) =
      assertThat(parse<Instruction>(start).toString()).isEqualTo(end)

  private fun checkRoundTrip(start: String, end: String = start) =
      parse<Instruction>(start).toString() == end
}
