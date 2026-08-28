package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.test.Test

internal class OrTriggerTest {
  @Test
  internal fun simpleSuperclassTriggerFiresForSubclass() {
    val game = newGame()
    val gameplay = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }

    gameplay.beginManual("ConcreteIndexedSignal!") {
      game.tasks
          .extract { it.instruction.toString() }
          .filter { it == "IndexedReward!" }
          .shouldContainExactlyInAnyOrder("IndexedReward!")
    }
  }

  @Test
  internal fun indexingPreservesEffectRegistrationOrderAcrossTriggerClasses() {
    val game = newGame()
    val gameplay = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }

    gameplay.beginManual("ConcreteOrderedSignal!") {
      game.tasks
          .extract { it.instruction.toString() }
          .filter { it.startsWith("OrderedReward") }
          .shouldContainExactly("OrderedReward1!", "OrderedReward2!")
    }
  }

  @Test
  internal fun firstMatchingArmGovernsSpecialization() {
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
    return Engine.newGame(canonicalPremise(catalog = OrProbeCatalog))
  }
}

private object OrProbeCatalog : TfmCatalog.Composite(Canon, OrProbeDeclarations)

private object OrProbeDeclarations : TfmCatalog() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              ABSTRACT CLASS LeftSpecializedSignal { HAS MAX 1 This }
              ABSTRACT CLASS RightSpecializedSignal { HAS MAX 1 This }
              CLASS LeftOnlySignal : LeftSpecializedSignal { HAS =1 This }
              CLASS RightOnlySignal : RightSpecializedSignal { HAS =1 This }
              CLASS BothSpecializedSignals : LeftSpecializedSignal, RightSpecializedSignal
              CLASS LeftFirstReward<LeftSpecializedSignal, RightSpecializedSignal>
              CLASS RightFirstReward<LeftSpecializedSignal, RightSpecializedSignal>
              ABSTRACT CLASS IndexedSignal
              CLASS ConcreteIndexedSignal : IndexedSignal
              CLASS IndexedReward
              CLASS ConcreteOrderedSignal : IndexedSignal
              CLASS OrderedReward1, OrderedReward2

              CLASS IndexedProbe {
                HAS =1 This
                IndexedSignal: IndexedReward
              }

              CLASS OrderedIndexedProbe {
                HAS =1 This
                IndexedSignal: OrderedReward1
                ConcreteOrderedSignal: OrderedReward2
              }

              CLASS LeftFirstOrProbe {
                HAS =1 This
                LeftSpecializedSignal OR RightSpecializedSignal IF =1 LeftSpecializedSignal: LeftFirstReward<LeftSpecializedSignal, RightSpecializedSignal>
              }

              CLASS RightFirstOrProbe {
                HAS =1 This
                RightSpecializedSignal OR LeftSpecializedSignal IF =1 RightSpecializedSignal: RightFirstReward<LeftSpecializedSignal, RightSpecializedSignal>
              }
              """
                  .trimIndent()
          )
          .toSet()
}
