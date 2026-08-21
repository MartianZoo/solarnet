package dev.martianzoo.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.canonicalPremise
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.test.Test

class TriggerScalingTest {
  @Test
  fun `ordinary triggers scale their result while X triggers produce one result`() {
    val game = Engine.newGame(canonicalPremise(authority = authority))
    val gameplay = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }

    gameplay.beginManual("5 ScalingSignal!") {
      game.tasks
          .extract { it.instruction.toString() }
          .shouldContainExactlyInAnyOrder("5 ScaledResult!", "UnscaledResult!")
    }
  }

  private companion object {
    val declarations =
        object : TfmAuthority() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS ScalingSignal
                      CLASS ScaledResult
                      CLASS UnscaledResult

                      CLASS TriggerScalingProbe {
                        HAS =1 This
                        ScalingSignal: ScaledResult
                        X ScalingSignal: UnscaledResult
                      }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }

    val authority = TfmAuthority.Composite(Canon, declarations)
  }
}
