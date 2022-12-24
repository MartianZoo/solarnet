package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetsParser.Instructions
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.Instruction.ComplexFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
class InstructionTest {
  val inputs = """
    1
    1!
    Xyz
    Abc?
    -Bar?
    -1 Xyz
    Foo, 11
    -11 Abc?
    Bar / Bar
    5 Ahh<Bar>
    -1? / 5 Xyz
    -Foo / 1 Ahh
    Qux?, 11 Ahh!
    -Ahh<Ooh>, Ooh
    11 Wau: 5 / Abc
    1 Qux, 5, -1 Ooh
    -11 Wau<Ooh<Eep>>
    -11 Eep<Foo<Abc>>.
    11 Foo<Foo>!, 1 Bar
    -5 Abc<Xyz>. / 5 Foo
    5 Abc FROM Foo! / Foo
    5 Eep<Qux, Ooh>. / Foo
    1, -11 Qux, Xyz!, 5 Abc
    11 Eep FROM Ooh THEN Foo
    Abc, Foo, Qux, -Qux, -Foo
    -5 Wau(HAS =1 OR (1, Abc))
    1, -Abc., -Bar?, -Bar / Abc
    1 Qux FROM Abc!, 1, Abc<Foo>
    Qux<Abc FROM Ooh>, -1 / 1 Ahh
    (1 Wau OR 5 Foo, (1, 1)): Ahh!
    -11 Foo / 11 Foo(HAS MAX 1 Bar)
    -Abc / Abc OR Abc OR Xyz / 1 Qux
    1 Qux OR -Ooh / Qux OR (Qux: Foo)
    5, -Ooh, 1 Foo FROM Ooh, Eep<Xyz>.
    -Abc, 5, -Xyz, (5 Bar, Ahh, Qux): 1
    11! / 1 Qux<Eep<Wau>, Wau<Ahh<Qux>>>
    Xyz, Bar, Qux, 5 Bar., -Abc<Xyz<Foo>>
    Ahh, (5, 5 Bar): (Bar OR -11), -5 Xyz.
    Xyz<Bar<Abc>, Foo, Bar(HAS MAX 0 Foo)>!
    5! / 1 Bar<Ahh, Qux<Abc<Bar, Bar<Qux>>>>
    (-Bar, -1 Abc(HAS Bar), Ahh) THEN -11 Bar
    1, Wau, 5 OR 11, -Ooh, -11 Foo! OR -1 Abc!
    1, -5, Abc, 1, -5 Xyz, 5 Wau FROM Abc, Qux!
    Foo, Xyz<Bar>?, Foo, 1 OR Qux, -Bar THEN Bar
    (Bar OR MAX 1): 5 Bar, 11 Qux FROM Abc. / Ooh
    -5, 1 Foo: Foo, -1, Xyz: -Bar THEN 11 Foo, Ahh
    Xyz, 1 OR 1 Bar / 5 Xyz, 11 Bar FROM Ooh, 1 Qux
    Xyz, 1 Xyz<Foo, Ooh, Bar>., 5?, Bar / Bar OR Bar
    11 Qux, 5 Bar<Bar>: (Bar / 1 Foo, 11, Qux, 1 Foo)
    Xyz<Bar>., -5 Foo<Ahh>, -Bar., Qux<Eep>, Wau<Qux>.
    1 Qux / Foo, -1 / Eep, 1 Foo(HAS 1 Foo<Xyz>, MAX 0)
    1, Xyz!, Abc FROM Qux, Qux, Foo, -1 Bar!, 1 Bar<Ahh>
    (-1 Xyz, Qux / Ooh, Xyz, Ooh<Xyz, Ooh> FROM Foo) OR 5
    Abc<Abc, Qux>., 1 Ooh<Bar, Qux>, 11 Qux FROM Xyz, -Ahh
    (-Foo, 1 Foo, -5) OR -1, -Abc / Bar, Bar, -1 Foo, 1 Abc
    =5 Foo: ((5 Qux / 11 Bar<Bar>, -1) OR (11 Qux FROM Ahh))
    Ahh / Xyz<Abc, Ahh> OR (-1 Bar OR 1 OR (Foo: 5 Bar), Foo)
    (5 OR 5 Xyz OR MAX 1 Xyz<Abc> OR (Bar, Foo)): -1 Ooh / Abc
    Ahh / 1 Eep, 5 OR -Abc, 1, 11, -Qux?, (MAX 0, Foo, =5): Abc
    (1 Eep<Bar>, Foo. THEN Bar) OR (MAX 0 Qux: Foo) OR -Qux<Foo>
    1 Bar? OR (Xyz FROM Eep!), Foo(HAS 11 Bar), 1 / 5 Qux, -1 Foo
    Qux, 5 Xyz / 5 Qux, 5 OR Xyz, -1, 1, Qux, 1 Wau<Foo<Bar, Foo>>
    5. THEN 1 Foo / 1 Foo THEN (5 Bar FROM Qux, 1., Bar / Qux, Ahh)
    5 Foo<Wau<Foo<Ooh<Xyz>>> FROM Ahh<Ahh, Ooh>(HAS MAX 0 Foo), Ahh>
    Xyz<Bar, Ooh, Bar<Bar, Bar>> / Eep, 5 Wau<Ahh, Ooh, Abc FROM Qux>
    -Bar<Foo> OR 5, -Foo, 1 Qux FROM Eep, 11 Xyz., (1, Abc / Abc) OR 1
    Xyz OR Foo<Foo>, 1 / Qux, (Abc FROM Bar) OR -1 / Xyz, 5, Qux, 1 Bar
    -5., 11 Ooh., 1 Xyz, Qux FROM Ooh, Abc / Bar, -Abc OR Xyz OR -11 Abc
    Bar OR (Foo / Foo, Qux, Ooh) OR (Foo: -11 Ahh, 1 Xyz, Foo / Qux, Xyz)
    Abc<Ahh, Abc<Qux<Qux>>, Xyz(HAS Abc OR (Foo OR (Foo, MAX 0)))>(HAS 1)!
    Qux, -1, Foo, -5 Foo<Ahh>, -Xyz, Abc, 5 OR -Bar, -Bar OR Foo, -Bar<Bar>
    1 Qux<Qux>, 11 Bar., 5, -1 Xyz?, -Qux / Bar, -Ooh OR Foo, -11 Xyz!, -Bar
    5 Eep, (1 THEN Foo) OR 5 Bar, 5 Eep<Ooh>, Eep OR (Ahh FROM Qux) OR -5 Ooh
    5 Eep<Eep>, Abc, Bar, Qux, 5 Bar<Eep, Qux>, 5 Eep<Qux<Bar, Bar<Foo>, Bar>>
    11 Foo FROM Ooh? / Bar, -Foo!, 11 Ooh<Bar, Eep<Bar<Bar> FROM Abc>>, -11 Abc
    Qux!, Qux<Foo<Qux>>., 1 Bar, 1 Bar<Foo, Ooh FROM Bar>, -Bar, 5 Bar, Xyz, Ahh
    -1 Abc, Foo THEN (-1, Bar), Xyz / 1 Foo OR Foo., -Foo<Xyz>!, -Xyz, 5 Foo<Ooh>
    -1 Qux, Abc, -Bar<Foo>, Xyz., (MAX 0: Foo / Qux) OR Abc OR 1 Abc OR 5 Foo., -5
    1 Bar THEN (1! OR 5, -1 Qux!, 1, 1 Bar) THEN (Bar OR (1 THEN Foo) OR -Ahh, Foo)
    -Foo<Foo> OR (Qux THEN -Bar), -Qux, Foo<Foo>, 5 Xyz OR -1 Abc!, 1 Foo, Qux<Abc>!
  """.trimIndent()

  @Test fun testSampleStrings() {
    val pass = dev.martianzoo.tfm.pets.testSampleStrings<Instruction>(inputs)
    assertThat(pass).isTrue()
  }

  @Test fun from() {
    testRoundTrip("Foo FROM Bar")
    testRoundTrip("Foo FROM Bar?")
    testRoundTrip("3 Foo FROM Bar")
    testRoundTrip("1 Foo FROM Bar.")

    assertThat(parse<Instruction>("1 Foo FROM Bar.")).isEqualTo(
        Transmute(
            SimpleFrom(
                TypeExpression("Foo"),
                TypeExpression("Bar"),
            ), 1, AMAP)
    )
    testRoundTrip("Foo<Bar FROM Qux>")
    testRoundTrip("Foo<Bar FROM Qux>.")

    val instr = Transmute(
        ComplexFrom("Foo", listOf(
            ComplexFrom("Bar", listOf(
                SimpleFrom(
                    TypeExpression("Qux"),
                    TypeExpression("Abc", listOf(TypeExpression("Eep")))
                ))
            )),
        ),
        null,
        null
    )
    assertThat(instr.toString()).isEqualTo("Foo<Bar<Qux FROM Abc<Eep>>>")
    assertThat(parse<Instruction>("Foo<Bar<Qux FROM Abc<Eep>>>")).isEqualTo(instr)
  }

  @Test fun custom() {
    parse(Instructions.custom, "\$foo()")
    parse(Instructions.atom, "\$foo()")
    parse(Instructions.gated, "\$foo()")
    parse(Instructions.orInstr, "\$foo()")
    parse(Instructions.then, "\$foo()")
    parse(Instructions.instruction, "\$foo()")
    parse<Instruction>("\$foo()")
  }

  @Test fun debug5() {
    // for some reason, this creates Requirement.Or(Bar, Bar), so toSetCareful would blow up
    val actual = parse<Instruction>("(Bar OR Bar? OR -Foo, Foo) OR (5, -Bar, Bar<Foo, Qux>: (-Bar, 1))")
    val expected = Instruction.or(
        Instruction.multi(
            Instruction.or(
                Gain(TypeExpression("Bar", listOf())),
                Gain(TypeExpression("Bar", listOf()), intensity = OPTIONAL),
                Remove(TypeExpression("Foo", listOf()))),
            Gain(TypeExpression("Foo", listOf())),
        ),
        Instruction.multi(
            Gain(null, 5),
            Remove(TypeExpression("Bar", listOf())),
            Gated(
                Min(TypeExpression("Bar", listOf(TypeExpression("Foo", listOf()), TypeExpression("Qux", listOf())))),
                Instruction.multi(
                    Remove(TypeExpression("Bar", listOf())),
                    Gain(null, 1),
                ),
            ),
        ),
    )
    assertThat(actual).isEqualTo(expected)
  }
  fun testRoundTrip(start: String, end: String = start) =
      dev.martianzoo.tfm.pets.testRoundTrip<Instruction>(start, end)
}
