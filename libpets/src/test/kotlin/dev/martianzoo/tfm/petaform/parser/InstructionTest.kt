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
import org.junit.jupiter.api.Test

class InstructionTest {
  @Test fun wtf() {
    val expected = listOf(
        Gain(Expression("Megacredit"), 42),
        Gain(Expression("ProjectCard"), 10))

    assertThat(parse(QEs.explicitScalar, "42")).isEqualTo(42)
    assertThat(parse(QEs.qe, "42")).isEqualTo(QuantifiedExpression(Expression("Megacredit"), 42))
    assertThat(parse(Instructions.gain, "42")).isEqualTo(expected[0])
    assertThat(parse(Instructions.perable, "42")).isEqualTo(expected[0])
    assertThat(parse(Instructions.maybePer, "42")).isEqualTo(expected[0])
    assertThat(parse(Instructions.maybeProd, "42")).isEqualTo(expected[0])
    assertThat(parse(Instructions.atomInstruction, "42")).isEqualTo(expected[0])
    assertThat(parse(Instructions.gated, "42")).isEqualTo(expected[0])
    assertThat(parse(Instructions.orInstr, "42")).isEqualTo(expected[0])
    assertThat(parse(Instructions.then, "10 ProjectCard")).isEqualTo(expected[1])
    assertThat(parse(Instructions.instruction, "42, 10 ProjectCard")).isEqualTo(multi(expected))
  }

  @Test
  fun testGainInstruction() {
    val list = listOf(
        "1",
        "0",
        "OceanTile",
        "1 OceanTile<WaterArea>",
        "1024 OceanTile",
        "CityTile<LandArea>",
    )
    for (s in list) {
      parse<Instruction>(s) as Gain
    }
  }

  @Test
  fun testRemoveInstruction() {
    val list = listOf(
        "-OceanTile",
        "-1 OceanTile<WaterArea>",
        " - 1024 OceanTile",
        "-CityTile <LandArea>",
    )
    for (s in list) {
      parse<Instruction>(s) as Remove
    }
  }

  @Test
  fun testAndInstruction() {
    val instr = parse<Instruction>("-5 Heat, 4 Plant")
    assertThat(instr).isEqualTo(
        Instruction.multi(
            Remove(Expression("Heat"), 5),
            Gain(Expression("Plant"), 4)
        ),
    )
  }

  @Test
  fun `Local Heat Trapping`() {
    val input = "-5 Heat, 4 Plant OR 2 Animal"
    val instruction = parse<Instruction>(input)
    assertThat(instruction.toString()).isEqualTo(input)
    assertThat(instruction).isEqualTo(
        Instruction.multi(
            Remove(Expression("Heat"), 5),
            Instruction.or(
                Gain(Expression("Plant"), 4),
                Gain(Expression("Animal"), 2)
            )
        ),
    )
  }

  @Test
  fun `Alternate Local Heat Trapping 1`() {
    val input = "-Heat, 4 Plant OR 2"
    val instruction = parse<Instruction>(input)
    assertThat(instruction.toString()).isEqualTo(input)
    assertThat(instruction).isEqualTo(
        Instruction.multi(
            Remove(Expression("Heat"), 1),
            Instruction.or(
                Gain(Expression("Plant"), 4),
                Gain(Expression("Megacredit"), 2)
            )
        ),
    )
  }

  @Test
  fun `Alternate Local Heat Trapping 2`() {
    val input = "(-5 Heat, 4 Plant) OR 2 Animal"
    val instruction = parse<Instruction>(input)
    assertThat(instruction.toString()).isEqualTo(input)
    assertThat(instruction).isEqualTo(
        Instruction.or(
            Instruction.multi(
                Remove(Expression("Heat"), 5),
                Gain(Expression("Plant"), 4)
            ),
            Gain(Expression("Animal"), 2)
        ),
    )
  }

  @Test fun gates() {
    testRoundTrip("Foo: Bar")

    testRoundTrip("Foo: (Bar OR Baz)")
    testRoundTrip("(Foo: Bar) OR Baz")
    testRoundTrip("Foo: Bar OR Baz", "(Foo: Bar) OR Baz")

    testRoundTrip("Foo: (Bar, Baz)")
    testRoundTrip("Foo: Bar, Baz")
    testRoundTrip("(Foo: Bar), Baz", "Foo: Bar, Baz")

    testRoundTrip("Foo: (2 Bar: Ok)")
    testRoundTrip("Bar: Ok")
    testRoundTrip("2 Bar: Ok")
    testRoundTrip("MAX 2 Bar: Ok")
    testRoundTrip("MAX 0 Bar: Ok")
    testRoundTrip("Foo: (MAX 0 Bar: Ok)")
  }

  @Test fun then() {
    testRoundTrip("Foo THEN Bar")

    testRoundTrip("Foo THEN (Bar OR Baz)")
    testRoundTrip("(Foo THEN Bar) OR Baz")
    testRoundTrip("Foo THEN Bar OR Baz", "Foo THEN (Bar OR Baz)")

    testRoundTrip("Foo THEN (Bar, Baz)")
    testRoundTrip("Foo THEN Bar, Baz")
    testRoundTrip("(Foo THEN Bar), Baz", "Foo THEN Bar, Baz")
  }

  private fun testRoundTrip(start: String, end: String = start) {
    assertThat(parse<Instruction>(start).toString()).isEqualTo(end)
  }
}
