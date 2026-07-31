package dev.martianzoo.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.canonicalPremise
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.test.Test

class OrTriggerTest {
  @Test
  fun firstMatchingArmGovernsSpecialization() {
    val game = newGame()
    val gameplay = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }

    gameplay.beginManual("BothSpecializedSignals!") {
      game.tasks
          .extract { it.instruction.toString() }
          .shouldContainExactlyInAnyOrder(
              "LeftFirstReward<BothSpecializedSignals, RightSpecializedSignal>!",
              "RightFirstReward<LeftSpecializedSignal, BothSpecializedSignals>!",
          )
    }
  }

  private fun newGame(): World {
    return Engine.newGame(canonicalPremise(ruleset = OrProbeRuleset))
  }
}

private object OrProbeRuleset : TfmRuleset.Composite(Canon, OrProbeDeclarations)

private object OrProbeDeclarations : TfmRuleset.Empty() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              ABSTRACT CLASS LeftSpecializedSignal { HAS MAX 1 This }
              ABSTRACT CLASS RightSpecializedSignal { HAS MAX 1 This }
              CLASS LeftOnlySignal : LeftSpecializedSignal, AutoLoad { HAS =1 This }
              CLASS RightOnlySignal : RightSpecializedSignal, AutoLoad { HAS =1 This }
              CLASS BothSpecializedSignals : LeftSpecializedSignal, RightSpecializedSignal, AutoLoad
              CLASS LeftFirstReward<LeftSpecializedSignal, RightSpecializedSignal>
              CLASS RightFirstReward<LeftSpecializedSignal, RightSpecializedSignal>

              CLASS LeftFirstOrProbe : AutoLoad {
                HAS =1 This
                LeftSpecializedSignal OR RightSpecializedSignal IF =1 LeftSpecializedSignal: LeftFirstReward<LeftSpecializedSignal, RightSpecializedSignal>
              }

              CLASS RightFirstOrProbe : AutoLoad {
                HAS =1 This
                RightSpecializedSignal OR LeftSpecializedSignal IF =1 RightSpecializedSignal: RightFirstReward<LeftSpecializedSignal, RightSpecializedSignal>
              }
              """
                  .trimIndent()
          )
          .toSet()
}
