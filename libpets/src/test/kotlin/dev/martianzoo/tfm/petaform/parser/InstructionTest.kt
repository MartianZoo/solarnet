package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.Instruction.AndInstruction
import dev.martianzoo.tfm.petaform.api.Instruction.GainInstruction
import dev.martianzoo.tfm.petaform.api.Instruction.OrInstruction
import dev.martianzoo.tfm.petaform.api.Instruction.RemoveInstruction
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import org.junit.jupiter.api.Test

class InstructionTest {
  @Test
  fun testGainInstruction() {
    val list = listOf(
        "OceanTile",
        "1 OceanTile<WaterArea>",
        "1024 OceanTile",
        "CityTile<LandArea>",
    )
    for (s in list) {
      parse<Instruction>(s) as GainInstruction
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
      parse<Instruction>(s) as RemoveInstruction
    }
  }

  @Test
  fun testAndInstruction() {
    val instr = parse<Instruction>("-5 Heat, 4 Plant")
    assertThat(instr).isEqualTo(
        AndInstruction.from(
            RemoveInstruction(Expression("Heat"), 5),
            GainInstruction(Expression("Plant"), 4),
        ),
    )
  }

  @Test
  fun testGroupedAndInstruction() {
    val instr = parse<Instruction>("(-5 Heat, 4 Plant)")
    assertThat(instr).isEqualTo(
        AndInstruction.from(
            RemoveInstruction(Expression("Heat"), 5),
            GainInstruction(Expression("Plant"), 4),
        ),
    )
  }

  @Test
  fun `Local Heat Trapping`() {
    val input = "-5 Heat, 4 Plant OR 2 Animal"
    val instruction = parse<Instruction>(input)
    assertThat(instruction.petaform).isEqualTo(input)
    assertThat(instruction).isEqualTo(
        AndInstruction.from(
            RemoveInstruction(Expression("Heat"), 5),
            OrInstruction.from(
                GainInstruction(Expression("Plant"), 4),
                GainInstruction(Expression("Animal"), 2),
            ),
        ),
    )
  }

  @Test
  fun `Alternate Local Heat Trapping 1`() {
    val input = "-Heat, 4 Plant OR 2"
    val instruction = parse<Instruction>(input)
    assertThat(instruction.petaform).isEqualTo(input)
    assertThat(instruction).isEqualTo(
        AndInstruction.from(
            RemoveInstruction(Expression("Heat"), 1),
            OrInstruction.from(
                GainInstruction(Expression("Plant"), 4),
                GainInstruction(Expression("Megacredit"), 2),
            ),
        ),
    )
  }

  @Test
  fun `Alternate Local Heat Trapping 2`() {
    val input = "(-5 Heat, 4 Plant) OR 2 Animal"
    val instruction = parse<Instruction>(input)
    assertThat(instruction.petaform).isEqualTo(input)
    assertThat(instruction).isEqualTo(
        OrInstruction.from(
            AndInstruction.from(
                RemoveInstruction(Expression("Heat"), 5),
                GainInstruction(Expression("Plant"), 4),
            ),
            GainInstruction(Expression("Animal"), 2),
        ),
    )
  }
}
