package dev.martianzoo.tfm.api

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.SpecialClassNames.OK
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import org.junit.jupiter.api.Test

/**
 * Tests implementing the [CustomInstruction] API. Doing this reasonably involves going
 * through [Instruction.Custom].
 */
class CustomInstructionApiTest {
  val instruction = instruction("@foo()")
  val dumb = Gain(sat(1, OK.type))

  private fun fakeAuthority(custom: CustomInstruction) =
      object : Authority.Minimal() {
        override val customInstructions = listOf(custom)
      }

  private fun testIt(custom: CustomInstruction) {
    var gotIt = false
    val game: GameState = object : StubGameState(fakeAuthority(custom)) {
      override fun applyChange(
          count: Int,
          gaining: GenericTypeExpression?,
          removing: GenericTypeExpression?,
          cause: Cause?,
      ) {
        gotIt = true
        assertThat(gaining).isEqualTo(OK.type)
      }
    }
    instruction.execute(game)
    assertThat(gotIt).isTrue()
  }

  @Test
  fun overridePreferred() {
    val custom = object : CustomInstruction("foo") {
      override fun translate(
          game: ReadOnlyGameState, arguments: List<TypeExpression>
      ) = dumb
    }
    testIt(custom)
  }

  @Test
  fun overrideAlsoOkay() {
    val custom = object : CustomInstruction("foo") {
      override fun translateToPets(
          game: ReadOnlyGameState, arguments: List<TypeExpression>
      ) = dumb.toString()
    }
    testIt(custom)
  }

  @Test
  fun overrideLastResort() {
    val custom = object : CustomInstruction("foo") {
      override fun execute(game: GameState, arguments: List<TypeExpression>) {
        game.applyChange(1, gaining = OK.type)
      }
    }
    testIt(custom)
  }
}
