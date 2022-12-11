package dev.martianzoo.tfm.petaform

import com.github.h0tk3y.betterParse.combinators.or
import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.Instruction.FromIsBelow
import dev.martianzoo.tfm.petaform.Instruction.FromIsRightHere
import dev.martianzoo.tfm.petaform.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.petaform.Instruction.Transmute
import dev.martianzoo.tfm.petaform.PetaformParser.Instructions
import dev.martianzoo.tfm.petaform.PetaformParser.Instructions.from
import dev.martianzoo.tfm.petaform.PetaformParser.Instructions.noFrom
import dev.martianzoo.tfm.petaform.PetaformParser.parse
import org.junit.jupiter.api.Test

// Most testing is done by AutoTest
class InstructionTest {
  val inputs = """
    Foo
  """.trimIndent().split('\n')

  @Test fun testSampleStrings() {
    inputs.forEach { testRoundTrip<Instruction>(it) }
  }

  @Test fun debug1() {
    parse(Instructions.perable, "Abc")
    parse(Instructions.maybePer, "Abc / Foo")
    parse(Instructions.orInstr, "Abc / Foo OR Qux")
    parse(Instructions.anyGroup, "(Abc / Foo OR Qux)")
    parse(Instructions.then, "(Abc / Foo OR Qux) THEN (-Foo, Bar) THEN -Foo")
  }

  @Test fun debug2() {
    parse(PetaformParser.typeExpression, "Foo<Foo<Foo, Bar>, Qux<Foo>>")
    parse(PetaformParser.qe, "5 Foo<Foo<Foo, Bar>, Qux<Foo>>")
    parse(Instructions.maybePer, "-Bar / 5 Foo<Foo<Foo, Bar>, Qux<Foo>>")
    parse(Instructions.maybePer, "-Bar / 5 Foo<Foo<Foo, Bar>, Qux<Foo>>")
  }

  @Test fun from() {
    testRoundTrip<Instruction>("Foo FROM Bar")
    testRoundTrip<Instruction>("Foo FROM Bar?")
    testRoundTrip<Instruction>("3 Foo FROM Bar")
    testRoundTrip<Instruction>("1 Foo FROM Bar.")

    assertThat(parse<Instruction>("1 Foo FROM Bar.")).isEqualTo(
        Transmute(
            FromIsRightHere(
                TypeExpression("Foo"),
                TypeExpression("Bar"),
            ), 1, AMAP)
    )
    parse(Instructions.fromIsHere, "Bar FROM Qux")
    parse(from, "Bar FROM Qux")
    parse(from or noFrom, "Bar FROM Qux")
    parse(Instructions.fromIsBelow, "Foo<Bar FROM Qux>")

    testRoundTrip<Instruction>("Foo<Bar FROM Qux>")
    testRoundTrip<Instruction>("Foo<Bar FROM Qux>.")

    val instr = Transmute(
        FromIsBelow("Foo", listOf(
            FromIsBelow("Bar", listOf(
                FromIsRightHere(
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

  @Test fun debug3() {
    val s = "1 Bar<Bar, Qux>, Foo, Foo, 5 Bar, Bar: Foo, Bar, Foo, Qux<Bar>, 5 Abc, 1 Abc, Xyz<Ooh>, Qux<Abc>, Bar<Bar>, Bar, Bar, 1 Qux, Foo, 1 Foo<Qux, Foo>, Abc, Bar<Bar>, 1 Foo<Qux>, 1 Qux<Xyz>, Foo, Abc, Bar<Bar>, Bar<Abc>, Bar<Foo>"
    testRoundTrip<Instruction>(s)
  }
}
