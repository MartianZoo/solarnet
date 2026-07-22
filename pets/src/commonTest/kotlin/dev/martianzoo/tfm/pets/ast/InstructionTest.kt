package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Most testing is done by AutomatedTest
internal class InstructionTest {
  val inputs =
      """
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
      5 Qux, PROD[-Bar]
      1. / 11 Megacredit
      PROD[PROD[-11 Abc]]
      PROD[Ooh FROM Wau]
      1 / Abc<Bar(HAS Bar)>
      11 Foo<Foo, Bar<Xyz>>.
      Foo<Ahh<Xyz>, Bar, Qux>
      Ahh<Abc<Xyz>, Bar<Xyz>>.
      PROD[Abc<Foo> FROM Abc<Xyz>]
      PROD[Qux FROM Bar / Abc]
      PROD[Abc<Ooh> / Megacredit]
      PROD[11 Xyz, Bar FROM Foo]
      PROD[-5 Foo, Qux FROM Ahh.]
      PROD[-Bar OR -1! OR -Foo<Bar>]
      -1 / 11 Qux<Abc, Qux<Ahh>, Qux>
      PROD[1], 1, Abc<Qux> FROM Abc<Foo>!
      PROD[Xyz / 5 Foo], 1 THEN 5 / Abc
      (5 Bar<Abc> FROM Bar) OR -Foo, Bar
      PROD[Abc FROM Qux, 5 Foo FROM Abc]
      PROD[(1: Foo) OR -1, 11 Abc FROM Abc]
      ((MAX 1 Bar OR 1) OR Foo): (1 OR Ahh!)
      5 / Megacredit OR (Foo, Ooh FROM Foo)
      1 / Abc THEN ((Foo FROM Xyz) OR 5 Bar)
      (11 Bar<Foo> FROM Bar) OR (Foo FROM Abc)
      5: (1 / Megacredit OR -1, -Ooh<Bar> / Qux)
      Xyz<Bar> FROM Xyz<Abc>? / Abc, 5 Eep FROM Xyz?
      PROD[1, -1., PROD[1: -1], (1, (Bar, 5 Foo))]
      11 Ahh<Wau<Foo<Qux>, Eep<Xyz>, Qux>> FROM Ahh<Wau<Foo<Qux>, Foo, Qux>>
      Qux<Abc> OR ((Ahh FROM Foo) OR (-1 OR -Bar))
      5 / Xyz<Foo<Foo>, Ahh<Qux<Bar<Qux>>, Qux>, Xyz>
      5 Ooh<Foo> FROM Foo<Ooh>!, PROD[-Foo THEN 1: -1]
      PROD[1 OR PROD[5: Foo] OR (1 / Megacredit, -Abc)]
      Foo FROM Ooh, PROD[1: (-1, 1)], 5 / 11 Megacredit
      Ooh. OR 1 OR (1, Foo FROM Eep, Bar / 5 Megacredit)
      PROD[(Ooh / Megacredit, Foo, 1), Bar / Bar THEN 1, 1]
      11 Bar, (Xyz, (Foo OR 1): 1) OR (Foo: (-1 OR 1 OR Qux))
      1, Bar, -Foo OR PROD[Foo FROM Abc] OR (Foo FROM Xyz)
      11 Qux<Qux<Ooh<Foo>(HAS 5)>, Ooh(HAS MAX 1 Bar)> FROM Qux<Qux<Foo>, Ooh(HAS MAX 1 Bar)>
      PROD[((1 OR MAX 1 Megacredit): 1) OR (1 OR -Bar) OR 1, Bar]
      (1: 1, -5 Bar), 11 Qux<Qux<Foo>, Bar>, PROD[1 / 5 Foo], Bar!
      """
          .trimIndent()

  @Test
  fun testSampleStrings() {
    testSampleStrings<Instruction>(inputs)
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

  fun testRoundTrip(start: String, end: String = start) = testRoundTrip<Instruction>(start, end)
}
