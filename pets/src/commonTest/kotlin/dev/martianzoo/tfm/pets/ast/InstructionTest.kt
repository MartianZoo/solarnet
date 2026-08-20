package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Most testing is done by AutomatedTest
internal class InstructionTest {
  @Test
  fun gainAndRemoveConvenienceFactories() {
    gain(cn("Foo")) shouldBe parse<Instruction>("Foo!")
    gain(cn("Foo"), count = 3, intensity = AMAP) shouldBe parse<Instruction>("3 Foo.")
    remove(cn("Foo")) shouldBe parse<Instruction>("-Foo!")
    remove(cn("Foo"), count = 3, intensity = AMAP) shouldBe parse<Instruction>("-3 Foo.")
  }

  @Test
  fun contextFreeInstructionFailuresUseThePetsSyntaxDomain() {
    shouldThrow<PetSyntaxException> {
      parse<Instruction>("999999999999999999999999999999 Plant")
    }
    shouldThrow<PetSyntaxException> { parse<Instruction>("Plant OR Plant") }
    shouldThrow<PetSyntaxException> { parse<Instruction>("X Plant, X Heat") }
  }

  @Test
  fun commaSyntaxIsATreeAndNotAnInstruction() {
    parse<InstructionTree>("Plant, Heat") shouldBe
        InstructionGroup(listOf(parse("Plant"), parse("Heat")))
    shouldThrow<PetSyntaxException> { parse<Instruction>("Plant, Heat") }
  }

  @Test
  fun thenIsRightAssociativeAndRejectsSequencesOnTheLeft() {
    val then = parse<Instruction>("Plant THEN Heat THEN Steel") as Then

    then.stages shouldBe listOf(parse<Instruction>("Plant"), parse<Instruction>("Heat"))
    then.continuation shouldBe parse<Instruction>("Steel")
    parse<Instruction>("Plant THEN (Heat THEN Steel)") shouldBe then
    shouldThrow<PetSyntaxException> { parse<Instruction>("(Plant THEN Heat) THEN Steel") }
    shouldThrow<PetSyntaxException> {
      parse<Instruction>("((Plant THEN Heat) OR Steel) THEN Energy")
    }
  }

  @Test
  fun groupedShapesRoundTripWithoutChangingTheirTree() {
    testRoundTrip<InstructionTree>("1: (1, -5 Bar)")
    testRoundTrip<InstructionTree>("(Foo, Bar) OR Qux")
    testRoundTrip<InstructionTree>(
        "PROD[1, -1., PROD[1: -1], (1, (Bar, 5 Foo))]",
        "PROD[1, -1., PROD[1: -1], 1, Bar, 5 Foo]",
    )
    testRoundTrip<InstructionTree>(
        "PROD[(Ooh / Megacredit, Foo, 1), Bar / Bar THEN 1, 1]",
        "PROD[Ooh / Megacredit, Foo, 1, Bar / Bar THEN 1, 1]",
    )
  }

  private val inputs =
      """
      2
      -5
      -X?
      Qux?
      X Foo
      X Foo?
      2X Abc.
      -11X Bar
      -1, X Foo
      -Foo<Qux>?
      X Wau<Qux>?
      5 Foo BY Wau
      Foo<Eep<Qux>>
      5, 2 Qux / Bar
      5 !Bar FROM Ahh
      Abc(HAS 5 !Bar)?
      2 Bar THEN 1: Xyz
      Bar / PROD[Foo], 1
      !Abc<Foo<Bar<Foo>>>
      Ok BY Eep<!Qux<Qux>>
      -1 / 2 Foo MAX 5, Bar
      Foo / Bar MAX 5 - Qux
      -11X?, PROD[Bar] OR Ok
      Ahh<Bar, Abc<Bar<Eep>>>
      (Foo: Xyz) OR -Bar / Foo
      Qux THEN X Qux, 1, 5 Foo.
      (MAX 0 Foo, MAX 1 Foo): Ok
      (1: 1, -1!, Xyz, -1) OR Foo
      (5 Xyz FROM Bar) BY Ooh<Wau>
      Ahh<Abc> THEN Qux, PROD[-Qux]
      Foo, Foo(HAS 2 Bar) / Bar<Foo>
      2 Abc(HAS 1) FROM Foo, 5, 5 Bar
      X !Qux<Foo, Ooh<Bar>>(HAS 5 Qux)
      X Wau FROM Bar, Foo / PROD[3 Qux]
      !Bar, 5 BY Abc<Xyz<Bar<Bar<Qux>>>>
      Foo(HAS Abc)!, Xyz OR 2 Abc<Ooh>, 1
      11X Wau<Ahh>(HAS 1 OR Abc) FROM Abc.
      2X Qux / 2 Abc OR (-2 Ahh<Foo>., Qux)
      -2 Abc<Xyz<Qux>, Bar<Foo>>., Ok OR Abc
      PROD[Ok OR (1: Foo)], PROD[2 Ahh / Qux]
      Bar(HAS Abc) / Eep<Abc>, 5 Bar., 1 / Bar
      (1 OR Abc): 2, 11 Qux: 1 / Foo<Bar>, -Abc
      X Ooh<Qux<Abc<Qux>>>?, -Qux<Xyz<Qux<Abc>>>
      1, X Foo, Foo? / Ooh, Ok THEN Ok THEN 2 Bar
      (-Foo, Foo, 1) OR Foo BY Qux, PROD[Xyz<Abc>]
      X Ahh FROM Bar<Qux<Xyz(HAS 1 OR (1 OR Qux))>>
      -2 Ooh OR Foo / Foo, Xyz, -X Abc, Ooh FROM Bar
      1, Abc<Foo<Ahh>, Foo<Abc<Bar>, Foo, Foo>, Xyz>.
      Bar<Ooh<Bar>> FROM Qux, Abc FROM Abc / 2 (3 Foo)
      1, 5X Ahh., Bar / PROD[Foo], PROD[Ok BY Foo<Bar>]
      2 Ahh, MAX 0 Megacredit: Qux?, Abc?, Qux<Bar, Qux>
      1?, -Bar<Qux> / PROD[Bar], Foo<Ooh<Foo, Bar, Bar>>!
      X? OR Foo<Qux>. / Xyz<Bar> OR -X Foo<Abc<Foo, Foo>>.
      (Foo OR Ahh<Qux>): (Ok BY !Bar) BY Eep<Foo<Abc<Qux>>>
      -5 Foo OR (Eep OR Foo), Bar BY Foo, 1, -Qux<Qux> / Bar
      2 Qux!, Foo FROM Foo, X Bar<Qux<Foo>>. BY Wau<Xyz, Ahh>
      Wau<Foo> FROM Foo!, (1: 2) OR (1, 2 / Foo) OR -Qux / Foo
      1 / PROD[PROD[Bar]], 5X Ahh FROM Bar<Foo, Foo<Bar, Foo>>!
      ((Foo OR =1 Megacredit) OR MAX 2 Ooh): (1, Bar.) OR X Bar.
      Qux / 2 Abc<Bar>, 1 OR (Foo, Qux, 1), -Foo, 2 Bar FROM Wau?
      -X Wau<Ahh<Ahh>, !Foo<Abc>>, PROD[-1 OR (Foo FROM Qux<Abc>)]
      """
          .trimIndent()

  @Test
  fun testSampleStrings() {
    testSampleStrings<InstructionTree>(inputs)
  }

  @Test
  fun from() {
    testRoundTrip("Foo FROM Bar")
    testRoundTrip("Foo FROM Bar?")
    testRoundTrip("3 Foo FROM Bar")
    testRoundTrip("Foo FROM Bar.")
    testRoundTrip("1 Foo FROM Bar.", "Foo FROM Bar.")

    parse<Instruction>("1 Foo FROM Bar.") shouldBe
        Transmute(
            FromExpression(cn("Foo").expression, cn("Bar").expression),
            ActualScalar(1),
            AMAP,
        )
    testRoundTrip("Foo<Bar> FROM Foo<Qux>")
    testRoundTrip("Foo<Bar> FROM Foo<Qux>.")

    val instr =
        Transmute(
            FromExpression(
                cn("Foo").of(cn("Bar").of(cn("Qux"))),
                cn("Foo").of(cn("Bar").of(cn("Abc").of(cn("Eep")))),
            ),
            ActualScalar(1),
            null,
        )
    instr.toString() shouldBe "Foo<Bar<Qux>> FROM Foo<Bar<Abc<Eep>>>"
    parse<Instruction>("Foo<Bar<Qux>> FROM Foo<Bar<Abc<Eep>>>") shouldBe instr
    shouldThrow<PetSyntaxException> { parse<Instruction>("Foo<Bar FROM Qux>") }
  }

  @Test
  fun backslashCrLfContinuesAnElement() {
    testRoundTrip("Foo\\\r\n OR Bar", "Foo OR Bar")
  }

  @Test
  fun gatingBindsLessTightlyThanOr() {
    val gated = parse<Instruction>("Foo: Bar OR Baz") as Gated

    (gated.inner is Or) shouldBe true
    gated.toString() shouldBe "Foo: Bar OR Baz"
    parse<Instruction>("Foo: (Bar OR Baz)").toString() shouldBe "Foo: Bar OR Baz"
  }

  @Test
  fun aGatedAlternativeRequiresParentheses() {
    val alternatives = parse<Instruction>("(Foo: Bar) OR Baz") as Or

    (alternatives.instructions.first() is Gated) shouldBe true
    alternatives.toString() shouldBe "(Foo: Bar) OR Baz"
    shouldThrow<PetSyntaxException> { parse<Instruction>("Bar OR Foo: Baz") }
  }

  private fun testRoundTrip(start: String, end: String = start) =
      testRoundTrip<InstructionTree>(start, end)
}
