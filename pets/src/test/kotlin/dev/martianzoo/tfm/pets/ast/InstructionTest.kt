package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.From.ComplexFrom
import dev.martianzoo.tfm.pets.ast.From.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class InstructionTest {
  val inputs = """
    5
    1!
    Xyz
    Eep?
    5 Eep
    11 Ahh
    -Qux, 1
    PROD[1.]
    PROD[Xyz]
    PROD[-Foo]
    11 Ahh<Bar>
    PROD[11 Ahh]
    PROD[-5 Xyz!]
    PROD[Bar<Abc>]
    5 Bar<Abc<Qux>>
    PROD[@name(Ooh)]
    5 Qux, PROD[-Bar]
    1. / 11 Megacredit
    PROD[PROD[-11 Abc]]
    PROD[1 Ooh FROM Wau]
    1 / Abc<Bar(HAS Bar)>
    11 Foo<Foo, Bar<Xyz>>.
    Foo<Ahh<Xyz>, Bar, Qux>
    Ahh<Abc<Xyz>, Bar<Xyz>>.
    PROD[1 Abc<Foo FROM Xyz>]
    PROD[1 Qux FROM Bar / Abc]
    PROD[Abc<Ooh> / Megacredit]
    PROD[11 Xyz, 1 Bar FROM Foo]
    PROD[-5 Foo, 1 Qux FROM Ahh.]
    PROD[-Bar OR -1! OR -Foo<Bar>]
    -1 / 11 Qux<Abc, Qux<Ahh>, Qux>
    PROD[1], 1, 1 Abc<Qux FROM Foo>!
    PROD[Xyz / 5 Foo], 1 THEN 5 / Abc
    (1 Bar<Abc> FROM Bar) OR -Foo, Bar
    @name(Abc, Ooh<Ahh<Xyz, Ooh, Abc>>)
    PROD[1 Abc FROM Qux, 1 Foo FROM Abc]
    PROD[(1: Foo) OR -1, 11 Abc FROM Abc]
    ((MAX 1 Bar OR 1) OR Foo): (1 OR Ahh!)
    5 / Megacredit OR (Foo, 1 Ooh FROM Foo)
    1 / Abc THEN ((1 Foo FROM Xyz) OR 5 Bar)
    (1 Bar<Foo> FROM Bar) OR (1 Foo FROM Abc)
    5: (1 / Megacredit OR -1, -Ooh<Bar> / Qux)
    1 Xyz<Bar FROM Abc>? / Abc, 5 Eep FROM Xyz?
    PROD[1, -1., PROD[1: -1], (1, (Bar, 5 Foo))]
    11 Ahh<Wau<Foo<Qux>, Eep<Xyz> FROM Foo, Qux>>
    Qux<Abc> OR ((1 Ahh FROM Foo) OR (-1 OR -Bar))
    5 / Xyz<Foo<Foo>, Ahh<Qux<Bar<Qux>>, Qux>, Xyz>
    5 Ooh<Foo> FROM Foo<Ooh>!, PROD[-Foo THEN 1: -1]
    PROD[1 OR PROD[5: Foo] OR (1 / Megacredit, -Abc)]
    @name(Qux<Foo<Foo>, Qux<Bar>, Qux<Bar<Foo>>>, Qux)
    1 Foo FROM Ooh, PROD[1: (-1, 1)], 5 / 11 Megacredit
    Ooh. OR 1 OR (1, 1 Foo FROM Eep, Bar / 5 Megacredit)
    PROD[(Ooh / Megacredit, Foo, 1), Bar / Bar THEN 1, 1]
    =1 Foo: (11 Xyz OR Bar, @name(Qux) OR -1 / Megacredit)
    11 Bar, (Xyz, (Foo OR 1): 1) OR (Foo: (-1 OR 1 OR Qux))
    1, Bar, -Foo OR PROD[1 Foo FROM Abc] OR (1 Foo FROM Xyz)
    11 Qux<Qux<Ooh<Foo>(HAS 5) FROM Foo>, Ooh(HAS MAX 1 Bar)>
    (-Foo, PROD[-Foo]), @name(Bar<Foo<Qux>>), (-5, @name(Qux))
    PROD[((1 OR MAX 1 Megacredit): 1) OR (1 OR -Bar) OR 1, Bar]
    (1: 1, -5 Bar), 11 Qux<Qux<Foo>, Bar>, PROD[1 / 5 Foo], Bar!
  """

  @Test
  fun testSampleStrings() {
    val pass = testSampleStrings<Instruction>(inputs)
    assertThat(pass).isTrue()
  }

  @Test
  fun from() {
    testRoundTrip("Foo FROM Bar")
    testRoundTrip("Foo FROM Bar?")
    testRoundTrip("3 Foo FROM Bar")
    testRoundTrip("1 Foo FROM Bar.")

    assertThat(instruction("1 Foo FROM Bar."))
        .isEqualTo(Transmute(SimpleFrom(cn("Foo").type, cn("Bar").type), 1, AMAP))
    testRoundTrip("Foo<Bar FROM Qux>")
    testRoundTrip("Foo<Bar FROM Qux>.")

    val instr =
        Transmute(
            ComplexFrom(
                cn("Foo"),
                listOf(
                    ComplexFrom(
                        cn("Bar"),
                        listOf(SimpleFrom(cn("Qux").type, cn("Abc").addArgs(cn("Eep").type))))),
            ),
            null,
            null)
    assertThat(instr.toString()).isEqualTo("Foo<Bar<Qux FROM Abc<Eep>>>")
    assertThat(instruction("Foo<Bar<Qux FROM Abc<Eep>>>")).isEqualTo(instr)
  }

  @Test
  fun custom2() {
    testRoundTrip<Instruction>("@name()")
    testRoundTrip<Instruction>("@name(Abc)")
    testRoundTrip<Instruction>("@name(Abc, Def)")
    testRoundTrip<Instruction>("@name(Abc<Xyz, Bar>)")
    testRoundTrip<TypeExpr>("Abc")
    testRoundTrip<TypeExpr>("Abc<Bar>")
    testRoundTrip<TypeExpr>("Abc(HAS Bar)")
    testRoundTrip<TypeExpr>("Abc(HAS 11 Bar)")
    testRoundTrip<Requirement>("Bar")
    testRoundTrip<Requirement>("11 Bar")
    testRoundTrip<Requirement>("MAX 11 Bar")

    testRoundTrip<TypeExpr>("Abc(HAS MAX 11 Bar)")

    testRoundTrip<Instruction>("@name(Abc(HAS MAX 11 Bar))")
    testRoundTrip<Instruction>("@name(Abc(HAS MAX 11 Bar<Xyz, Bar>))")
  }

  fun testRoundTrip(start: String, end: String = start) = testRoundTrip<Instruction>(start, end)
}
