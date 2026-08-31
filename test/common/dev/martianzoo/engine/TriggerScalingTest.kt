package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.test.Test

internal class TriggerScalingTest {
  @Test
  internal fun `ordinary triggers scale their result while X triggers produce one result`() {
    val game = Engine.newGame(canonicalPremise(catalog = catalog))
    val agent = game.agent(PLAYER1).also { it.autoExecMode = NONE }

    agent.beginManual("5 ScalingSignal!") {
      game.tasks
          .extract { it.instruction.toString() }
          .shouldContainExactlyInAnyOrder(
              "5 ScaledResult!",
              "UnscaledResult!",
              "10 BoundResult!",
              "FixedResult!",
          )
    }
  }

  private companion object {
    val declarations =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS ScalingSignal
                      CLASS ScaledResult
                      CLASS UnscaledResult
                      CLASS BoundResult
                      CLASS FixedResult

                      CLASS TriggerScalingProbe {
                        HAS =1 This
                        ScalingSignal: ScaledResult
                        X ScalingSignal: UnscaledResult
                        X ScalingSignal: 2X BoundResult, FixedResult
                      }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }

    val catalog = TfmCatalog.Composite(Canon, declarations)
  }
}
