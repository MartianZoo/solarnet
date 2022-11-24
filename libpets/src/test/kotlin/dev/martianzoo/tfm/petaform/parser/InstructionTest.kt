package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.Instruction.Gain
import dev.martianzoo.tfm.petaform.api.Instruction.Remove
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import org.junit.jupiter.api.Test

class InstructionTest {
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
        Instruction.and(
            Remove(Expression("Heat"), 5),
            Gain(Expression("Plant"), 4)
        ),
    )
  }

  @Test
  fun `Local Heat Trapping`() {
    val input = "-5 Heat, 4 Plant OR 2 Animal"
    val instruction = parse<Instruction>(input)
    assertThat(instruction.petaform).isEqualTo(input)
    assertThat(instruction).isEqualTo(
        Instruction.and(
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
    assertThat(instruction.petaform).isEqualTo(input)
    assertThat(instruction).isEqualTo(
        Instruction.and(
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
    assertThat(instruction.petaform).isEqualTo(input)
    assertThat(instruction).isEqualTo(
        Instruction.or(
            Instruction.and(
                Remove(Expression("Heat"), 5),
                Gain(Expression("Plant"), 4)
            ),
            Gain(Expression("Animal"), 2)
        ),
    )
  }
}
