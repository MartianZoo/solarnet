package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetParser.Instructions
import dev.martianzoo.tfm.pets.PetParser.Requirements
import dev.martianzoo.tfm.pets.PetParser.parsePets
import dev.martianzoo.tfm.pets.ast.FromExpression.ComplexFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class InstructionTest {
  val inputs = """
    5
    11
    11.
    Ahh?
    5 Xyz
    11 Qux
    -11 Eep
    PROD[-1]
    Foo<Foo>.
    PROD[-Abc]
    PROD[-Foo!]
    11 Ooh<Xyz>?
    PROD[-5 Ahh?]
    PROD[-1 / Foo]
    -Ooh<Abc, Wau>.
    -5 Bar(HAS Foo).
    PROD[0 Ahh]: Xyz!
    -Abc<Bar> / 11 Xyz
    MAX 5 Ooh<Foo>: Ooh
    ${'$'}name(Ahh<Eep>)
    Bar / Xyz, 5 Ahh<Qux>
    PROD[1 / 5 Megacredit]
    5 Xyz / 5 Qux<Wau<Xyz>>
    1 Foo<Foo<Ahh> FROM Ahh>
    PROD[Bar<Ahh, Abc> / Foo]
    PROD[Bar<Abc<Abc>> THEN 1]
    PROD[11 Abc<Ooh<Abc>, Qux>]
    5 Xyz: Bar!, ${'$'}name(Foo)
    1 Foo FROM Abc. THEN Bar<Bar>
    11 Bar FROM Bar<Abc, Xyz, Bar>
    PROD[=1 Megacredit: Qux? / Bar]
    PROD[5 Ooh<Ooh<Abc>>. THEN -Qux]
    PROD[(1 / Abc, -1), 11 Bar / Qux]
    1 OR Qux OR (0 Bar: 11 Bar, 5 Foo)
    PROD[1 Abc<Bar, Ooh FROM Ahh, Bar>]
    PROD[5 Ahh<Abc<Abc, Bar<Qux<Qux>>>>]
    -1 OR Abc / Qux<Qux<Foo>, Qux> OR 11!
    -Ahh. / Xyz<Ooh, Bar>, PROD[-11 / Bar]
    1 Qux(HAS MAX 1 Ooh<Bar<Foo>>) FROM Ahh
    5 Xyz<Ooh, Qux<Bar<Bar, Foo>, Eep, Eep>>
    =1 Xyz: (1 / 5 Qux THEN 1 THEN (-1, Xyz))
    PROD[(1, -Foo<Xyz>) OR 1 OR Bar<Bar, Qux>]
    PROD[1 Qux<Qux> FROM Foo<Bar> / Megacredit]
    5 Abc, Bar: (Qux, ${'$'}name(Abc<Foo>, Abc))
    ${'$'}name(Ahh<Eep<Xyz, Qux<Bar<Ahh>>>, Eep>)
    11 Bar<Wau<Abc FROM Qux<Bar, Qux>, Bar>, Ooh>?
    ${'$'}name(Abc<Foo<Eep<Abc>>>) OR PROD[Foo!, 5]
    -1? OR (1 Foo FROM Bar, -Xyz) OR Bar, -Bar<Bar>.
    -5 Qux<Abc>(HAS (0 OR MAX 0 Megacredit) OR 5 Qux)
    (Bar, Foo, 5 Qux), PROD[(0 OR Bar<Bar> OR 1): Abc]
    PROD[1 Ooh(HAS MAX 1 Foo) FROM Qux, 1 Qux FROM Ahh]
    MAX 5 Megacredit: Bar THEN ${'$'}name(Xyz), 5 THEN 1
    PROD[11 Abc FROM Bar, Bar<Foo<Abc, Foo(HAS 1), Foo>>]
    PROD[(MAX 1 Foo<Foo>, =1 Megacredit): ${'$'}name(Xyz)]
    1 Xyz<Foo FROM Bar, Ooh, Wau>(HAS 0 Abc, (1 OR 0) OR 1)
    5 Bar / Xyz OR Qux OR -Ahh / Qux, (1 / 5 Foo, 5, -1, -5)
    PROD[1 Bar<Qux<Xyz> FROM Foo, Qux, Qux<Eep>> THEN 11 Xyz]
    ((Bar, -1), Xyz) OR ((Bar<Qux> THEN Bar: 1) OR (1 OR Bar))
    -1 / Megacredit, 1, ${'$'}name(Foo, Qux<Bar>), 1 / Bar<Eep>
    5 Abc FROM Qux / Eep<Bar<Abc<Xyz>, Bar, Foo<Foo, Abc<Xyz>>>>
  """.trimIndent()

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

    assertThat(parsePets<Instruction>("1 Foo FROM Bar."))
        .isEqualTo(Transmute(SimpleFrom(gte("Foo"), gte("Bar")), 1, AMAP))
    testRoundTrip("Foo<Bar FROM Qux>")
    testRoundTrip("Foo<Bar FROM Qux>.")

    val instr = Transmute(ComplexFrom(
        ClassName("Foo"),
        listOf(ComplexFrom(ClassName("Bar"), listOf(SimpleFrom(gte("Qux"), gte("Abc", gte("Eep")))
        ))),
    ), null, null)
    assertThat(instr.toString()).isEqualTo("Foo<Bar<Qux FROM Abc<Eep>>>")
    assertThat(parsePets<Instruction>("Foo<Bar<Qux FROM Abc<Eep>>>")).isEqualTo(instr)
  }

  @Test
  fun custom1() {
    parsePets(Instructions.custom, "\$foo()")
    parsePets(Instructions.atom, "\$foo()")
    parsePets(Instructions.gated, "\$foo()")
    parsePets(Instructions.orInstr, "\$foo()")
    parsePets(Instructions.then, "\$foo()")
    parsePets(Instructions.whole, "\$foo()")
    parsePets<Instruction>("\$foo()")
  }

  @Test
  fun custom2() {
    testRoundTrip<Instruction>("\$name()")
    testRoundTrip<Instruction>("\$name(Abc)")
    testRoundTrip<Instruction>("\$name(Abc, Def)")
    testRoundTrip<Instruction>("\$name(Abc<Xyz, Bar>)")
    testRoundTrip<TypeExpression>("Abc")
    testRoundTrip<TypeExpression>("Abc<Bar>")
    testRoundTrip<TypeExpression>("Abc(HAS Bar)")
    testRoundTrip<TypeExpression>("Abc(HAS 11 Bar)")
    testRoundTrip<Requirement>("Bar")
    testRoundTrip<Requirement>("11 Bar")
    parsePets(Requirements.min, "Bar")
    parsePets(Requirements.min, "11 Bar")
    parsePets(Requirements.max, "MAX 11 Bar")
    testRoundTrip<Requirement>("MAX 11 Bar")

    testRoundTrip<TypeExpression>("Abc(HAS MAX 11 Bar)")

    testRoundTrip<Instruction>("\$name(Abc(HAS MAX 11 Bar))")
    testRoundTrip<Instruction>("\$name(Abc(HAS MAX 11 Bar<Xyz, Bar>))")
  }

  fun testRoundTrip(start: String, end: String = start) = testRoundTrip<Instruction>(start, end)
}
