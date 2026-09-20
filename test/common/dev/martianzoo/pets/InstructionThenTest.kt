package dev.martianzoo.pets

import dev.martianzoo.pets.ast.Instruction.Then
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Engine-facing behavior of a `THEN` instruction after the language pipeline has elaborated it. */
internal class InstructionThenTest {

  @Test
  internal fun `an open shared choice keeps its THEN stages together`() {
    val sharedType = elaborate("Token THEN Token") as Then
    val concrete = elaborate("Plant THEN Steel") as Then
    val sharedX = elaborate("X Plant THEN X Steel") as Then

    sharedType.mustRemainOneTask { langTable.resolve(it).abstract } shouldBe true
    concrete.mustRemainOneTask { langTable.resolve(it).abstract } shouldBe false
    concrete.mustRemainOneTask(null) shouldBe false
    sharedX.mustRemainOneTask(null) shouldBe true
  }
}
